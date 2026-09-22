# 双人岛（Lovers-plan）小程序后端部署报告

| 项目 | 内容 |
| --- | --- |
| 服务器 | 192.144.130.3（Ubuntu 24.04，Docker 29.6） |
| 部署位置 | `/opt/services/lovers/` |
| 小程序 API | `http://192.144.130.3/api`（经 Nginx :80，安全组已放行） |
| 文件访问 | `http://192.144.130.3/files/<对象路径>` |
| 部署时间 | 2026-09-15 |

---

## 一、部署架构

```
手机小程序 ──HTTPS/HTTP──> Nginx :80 (lp 已有 personal-stack 的 nginx)
                              ├─ /api/**   → lp-app:8080   (Spring Boot 3.2.5 / Java 17)
                              └─ /files/** → lp-minio:9000 (bucket 公共读)
lp-app ──> mysql:3306  (复用 personal-stack 的 mysql 容器, 库 lovers_plan)
     └──> redis:6379  (复用 personal-stack 的 redis 容器, 密码 Redis@2026!)
     └──> lp-minio:9000 (内网)
     └──> 公网:443 调微信 API (jscode2session / 订阅消息)
```

| 容器 | 镜像 | 状态 | 端口 |
| --- | --- | --- | --- |
| lp-app | lovers-app:1.0（本地 maven 多阶段构建）| healthy | 0.0.0.0:8080→8080 |
| lp-minio | minio/minio:latest | healthy | 仅内网（不发布 9000/9001）|
| mysql | mysql:8.0（复用）| healthy | 0.0.0.0:3306 |
| redis | redis:7-alpine（复用）| healthy | 127.0.0.1:6379 |
| nginx | nginx:1.27-alpine（复用）| healthy | 0.0.0.0:80/443 |

**没有新占 80/443 端口**：复用已有 nginx，通过共享 Docker 网络互通，
lp-app/lp-minio 挂在 `personal-stack_backend` + `personal-stack_frontend` 网络上。

## 二、目录结构（服务器）

```
/opt/services/lovers/
├── docker-compose.yml     # lp-app + lp-minio（external 网络复用 personal-stack）
├── .env                   # 密钥/账号（root:600）
├── src/
│   ├── Dockerfile         # maven 多阶段构建（阿里云Maven镜像 + JVM 限堆512M）
│   ├── .mvn-settings.xml  # 阿里云 Maven 镜像配置
│   └── backend/           # 后端源码（pom.xml + src）
├── sql/
│   ├── init.sql           # 建库建表+种子数据（已改为幂等，可重复执行）
│   └── 01_create_user.sql # 应用账号
└── minio/data/            # MinIO 数据
```

## 三、数据库

- 库：`lovers_plan`（utf8mb4），22 张表，含种子数据（25 条每日问题、30 个成就等，中文无乱码）
- 应用账号：`lovers`@`%`（专用，权限仅限 `lovers_plan` 库；密码在 `/opt/services/lovers/.env`）
- 修复了 `deploy/init-db/init.sql` 的 bug：`anniversary` 建表已含 `icon` 列，随后又 `ALTER ADD icon`
  导致全新初始化必然失败（ERROR 1060）。已把 3 处 ALTER 全部改为**幂等**写法（information_schema 判断）。

## 四、本次一并修复的代码问题（仓库内，建议提交）

| 文件 | 问题 | 修复 |
| --- | --- | --- |
| `backend/.../FileServiceImpl.java` | 预签名URL用内网endpoint `http://minio:9000` 签名，**手机端根本打不开**（旧部署的潜在bug：`MINIO_PUBLIC_ENDPOINT` 环境变量从未接入代码）| 新增 `minio.public-endpoint` 配置，`getFileUrl()` 返回经 Nginx `/files/` 的公网直链（bucket 公共读，无需签名）|
| `backend/.../application(-docker).yml` | 同上 | 增加 `public-endpoint` 配置项 |
| `deploy/init-db/init.sql` | 全新部署必挂（Duplicate column 'icon'）| 3 处 ALTER 改幂等 |
| `frontend/utils/api.js` | `BASE_URL` 还指向旧服务器 `http://47.93.25.125:8080/api` | 改为 `http://192.144.130.3/api`（走 Nginx :80，无需额外开安全组端口）|
| `deploy/Dockerfile`（服务器版在 `/opt/services/lovers/src/`）| Maven 直连中央仓库在国内云服务器又慢又易断 | 加阿里云 Maven 镜像；JVM 限堆 512M（4G 服务器防 OOM）|

## 五、验证结果（全部实测通过）

| 测试 | 结果 |
| --- | --- |
| 应用启动（DB/Redis/MinIO 连接）| ✅ `Started LoversApplication in 8.2s`，无 ERROR |
| 公网 `GET /api/common/lunar`（本机→公网IP→Nginx→App）| ✅ 200 `{"code":200,"data":"农历八月初五"}`（UTF-8 字节级校验通过）|
| 未登录访问受保护接口 `GET /api/home` | ✅ 401 业务错误 JSON |
| 微信登录链路 `POST /api/auth/login`（假 code）| ✅ 实际调用到微信 API，返回 `微信登录失败: invalid code, rid:...`（真实 code 即可登录成功）|
| 容器出网（微信 API 可达性）| ✅ |
| MinIO 上传 → 公网 `GET /files/lovers-plan/<对象>` 下载 | ✅ 200 内容一致 |
| 端口暴露 | 仅 80/443（已有）+ 8080（安全组未放行，公网不可达）；MinIO 9000/9001 不对外 |

## 六、小程序端接入

1. 微信开发者工具导入 `Lovers-plan/frontend` 目录
2. 详情 → 本地设置 → 勾选 **「不校验合法域名」**（开发/预览期用 HTTP）
3. `utils/api.js` 的 `BASE_URL` 已指向 `http://192.144.130.3/api`
4. **正式发布前必须**：
   - 已备案域名 + HTTPS（微信小程序强制要求），在 nginx 加 443 证书配置
   - `application.yml` 里 `wechat.template.*` 的 5 个订阅消息模板 ID 目前是占位符，
     需在微信公众平台创建模板后填入，否则订阅消息功能不可用
   - 小程序后台「服务器域名」配置 request 合法域名（HTTPS 域名）

## 七、常用运维命令

```bash
cd /opt/services/lovers
sudo docker compose logs -f app          # 后端日志
sudo docker compose restart app          # 重启后端
sudo docker compose up -d --build app    # 代码更新后重新构建发布
sudo docker compose ps                   # 状态

# 数据库
sudo docker exec -it mysql mysql -ulovers -p'<.env里的密码>' lovers_plan
```

**更新发布流程**：本地改 `Lovers-plan/backend` → 把改动同步到服务器
`/opt/services/lovers/src/backend/` → `sudo docker compose up -d --build app`。

## 八、回滚

```bash
cd /opt/services/lovers && sudo docker compose down     # 停掉 lp-app + lp-minio
# 还原 nginx（备份在 /opt/services/nginx/config/conf.d/default.conf.bak.20260915153854）
# 数据库保留（lovers_plan 库），需要可 DROP DATABASE lovers_plan;
```

## 九、安全备注

- `lp-app:8080` 发布了 `0.0.0.0`，但当前阿里云安全组未放行 8080，公网实际不可达（已实测）；
  如需直连 8080 调试，请按需放行并尽快关闭。
- MinIO 9000/9001 未发布，文件一律走 `/files/` 代理。
- `lovers_plan` 桶为**公共读**（任何人拿到链接可下载，对象名为 UUID 不可枚举）；
  如需私有化可把桶策略改回 private 并改用预签名 URL。
- 微信 AppSecret、数据库密码均在 `/opt/services/lovers/.env`（root:600），勿提交到 git。
- 上次遗留问题仍在：MySQL `root@'%'` 可远程登录且密码偏弱，建议 `DROP USER 'root'@'%'`。

## 十、文件访问与 MinIO 管理

### 文件 URL 格式

小程序上传头像/图片后，数据库存储的 URL 格式为：

```
http://192.144.130.3/files/<mediaType>/<uuid>.jpg    # e.g. /files/image/6c7d...
```

**注意：URL 中不包含 bucket 名。** Nginx `/files/` 位置在转发时自动补上桶名
`lovers-plan`，MinIO 实际读取的路径是 `/lovers-plan/<mediaType>/<uuid>.jpg`。
这意味着**数据库中已存的 URL 无需迁移即可直接使用**。

| 应用生成 | Nginx 重写 | MinIO 读取 |
| --- | --- | --- |
| `/files/image/xxx.jpg` | → | `/lovers-plan/image/xxx.jpg` |
| `/files/video/yyy.mp4` | → | `/lovers-plan/video/yyy.mp4` |

### 如何访问 MinIO 控制台？

MinIO API (9000) 和 Console (9001) **均不暴露到公网**。  
Console 已绑定回环地址 `127.0.0.1:9001`，可通过 SSH 隧道访问：

```bash
ssh -L 9001:127.0.0.1:9001 ubuntu@192.144.130.3
# 然后浏览器打开 http://localhost:9001
# 账号/密码见 /opt/services/lovers/.env 中的 MINIO_ROOT_USER / MINIO_ROOT_PASSWORD
```

### 纯命令行操作（无需控制台）

```bash
# 列出所有对象
sudo docker run --rm --network personal-stack_backend \
  --entrypoint sh minio/mc -c "mc alias set local http://lp-minio:9000 \$U \$P && mc ls -r local/lovers-plan/"

# 删除某个对象
sudo docker run --rm --network personal-stack_backend \
  --entrypoint sh minio/mc -c "mc alias set local http://lp-minio:9000 \$U \$P && mc rm local/lovers-plan/<路径>"
```