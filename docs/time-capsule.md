# 💌 时光胶囊模块

封存当下的爱意与回忆，在未来某个时间重新开启。

---

## 目录

- [数据模型](#数据模型)
- [状态流转](#状态流转)
- [API 端点](#api-端点)
- [前端页面](#前端页面)
- [双人模式流程](#双人模式流程)
- [联动功能](#联动功能)
- [定时任务](#定时任务)

---

## 数据模型

### time_capsule

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | 主键 |
| couple_id | BIGINT NOT NULL | 情侣关系 ID |
| user_id | BIGINT NOT NULL | 创建者用户 ID |
| type | VARCHAR(30) | 胶囊类型：`to_future_ta` / `to_future_us` / `birthday` / `anniversary` / `wish` |
| title | VARCHAR(200) | 胶囊标题 |
| content | TEXT | 文字内容（已封存时对外隐藏） |
| open_at | DATETIME NOT NULL | 预定开启时间 |
| opened_at | DATETIME NULL | 实际开启时间 |
| status | TINYINT | 状态：0=DRAFT 1=SEALED 2=OPENABLE 3=OPENED |
| pair_id | BIGINT NULL | 关联胶囊 ID（双人模式用） |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### time_capsule_media

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | 主键 |
| capsule_id | BIGINT NOT NULL | 胶囊 ID |
| media_type | VARCHAR(20) | 媒体类型：`image` / `audio` / `video` |
| file_url | VARCHAR(1000) | 文件 URL |
| create_time | DATETIME | 创建时间 |

---

## 状态流转

```
0:DRAFT ──(双方写入完成)──→ 1:SEALED ──(到达 open_at)──→ 2:OPENABLE ──(点击开启)──→ 3:OPENED
  ↑                           │                                              │
  └──(单人模式创建)────────────┘                                              │
                                └──(可直接删除)───────────────────────────────┘
```

| 状态 | 可读内容 | 可删除 | 说明 |
|---|---|---|---|
| DRAFT (0) | 创建者可读，伴侣不可读 | ✅ | 双人模式等待伴侣写入 |
| SEALED (1) | ❌ 隐藏 | ✅ | 已封存，到期自动变为 OPENABLE |
| OPENABLE (2) | ❌ 隐藏（需先开启） | ❌ | 已到期，点击开启后可读 |
| OPENED (3) | ✅ 完全可见 | ❌ | 已开启 |

---

## API 端点

Base: `/api/capsule`

### 创建胶囊

```
POST /api/capsule/create
```

**请求体：**
```json
{
  "type": "to_future_ta",
  "title": "给未来的你",
  "content": "希望我们还能在一起...",
  "openAt": "2026-06-19 00:00:00",
  "dualMode": false,
  "mediaList": [{ "mediaType": "image", "fileUrl": "..." }]
}
```

**说明：** `dualMode=true` 时胶囊创建为 DRAFT，伴侣需调用写入接口；`dualMode=false`（默认）直接封存为 SEALED。

### 伴侣写入（双人模式）

```
POST /api/capsule/write-partner
```

| 参数 | 说明 |
|---|---|
| pairCapsuleId | 发起方胶囊 ID |
| content | 伴侣的文字内容 |
| mediaList | 媒体列表 |

### 获取胶囊列表

```
GET /api/capsule/list?status=1&page=0&size=10
```

| 参数 | 说明 |
|---|---|
| status | 可选筛选：不传=全部，0=待写入，1=已封存，2=可开启，3=已开启 |
| page | 页码（从 0 开始） |
| size | 每页条数（默认 10） |

### 获取胶囊详情

```
GET /api/capsule/detail/{id}
```

**返回说明：** 根据胶囊状态决定是否返回内容：

| 状态 | content | mediaList | hint |
|---|---|---|---|
| DRAFT/SEALED | `null` | `[]` | 💌 内容已封存，开启后方可查看 |
| OPENABLE | `null` | `[]` | 📬 时光胶囊已成熟，点击开启 |
| OPENED | 可见 | 可见 | 无 |

双人模式下额外返回 `partner` 对象（仅 OPENED 可见）。

### 开启胶囊

```
POST /api/capsule/open/{id}
```

状态 OPENABLE → OPENED，记录实际开启时间，增加爱情树成长值，记录岛屿动态。

### 删除胶囊

```
POST /api/capsule/delete/{id}
```

删除胶囊、关联媒体、关联岛屿动态。双人模式同时删除伴侣的记录。

### 辅助接口

```
GET /api/capsule/types       → 胶囊类型列表
GET /api/capsule/open-options → 开启时间选项
GET /api/capsule/stats        → 统计（可开启/草稿数量）
```

---

## 前端页面

| 页面 | 路径 | 说明 |
|---|---|---|
| 胶囊列表 | `time-capsule/` | 状态标签页（全部/待写入/已封存/可开启/已开启），长按删除，可开启弹窗 |
| 创建胶囊 | `time-capsule-create/` | 三步向导：类型 → 内容+媒体 → 时间+提交，含双人模式开关 |
| 胶囊详情 | `time-capsule-detail/` | 展示信封头、时间信息、内容/封存提示、附件、伴侣内容、删除按钮 |
| 伴侣写入 | `time-capsule-write/` | 双人模式下伴侣填写内容+媒体后写入并封存 |

### 首页入口

首页双卡片行下方新增 💌 时光胶囊入口行，显示可开启数量角标。可开启胶囊同时出现在「今日提醒」中。

---

## 双人模式流程

```
发起方 ──创建──→ DRAFT 胶囊 ──→ 列表待写入 ──→ 伴侣点击 ──→ 写入页 ──→ 写入并封存
                                 ↑                              ↓
                              伴侣看到蓝色卡片              双方 → SEALED
                              "⏳ 等待你写入"
```

1. **发起方** 在创建页开启双人模式 → 胶囊以 DRAFT 状态保存
2. **伴侣** 在列表「待写入」标签页看到蓝色高亮胶囊 → 点击进入写入页
3. **伴侣** 填写内容+附件 → 点击「写入并封存」
4. 后端创建伴侣胶囊记录，双方胶囊同时变为 SEALED
5. 列表刷新，胶囊移至「已封存」

---

## 联动功能

| 联动 | 触发 | 说明 |
|---|---|---|
| 🌱 爱情树成长值 | 创建胶囊 +10 | `GROWTH_CAPSULE_CREATE = 10` |
| 🌱 爱情树成长值 | 成功开启 +20 | `GROWTH_CAPSULE_OPEN = 20` |
| 🌊 岛屿动态 | 封存胶囊 | 💌 你们封存了一颗时光胶囊 |
| 🌊 岛屿动态 | 开启胶囊 | 💌 一颗时光胶囊被开启 |
| 📷 回忆重现 | 一年后自动进入 | 由回忆重现模块后续接入 |
| 📬 微信订阅消息 | 胶囊到期 | 需配置订阅模板 ID |

---

## 定时任务

**`checkAndUpdateMatureCapsules()`** — `@Scheduled(cron = "0 * * * * ?")`

每分钟扫描一次，将所有已到达 `open_at` 且状态为 SEALED 的胶囊更新为 OPENABLE，使其在前端显示「可开启」状态。

---

## 版本

- 数据库迁移：`database/migrations/V006__time_capsule.sql`
- 后端路径：`backend/.../controller/TimeCapsuleController.java`（及对应 service/repository/model）
- 前端页面：`frontend/pages/time-capsule*`
