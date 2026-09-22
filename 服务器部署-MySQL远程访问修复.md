# 服务器 MySQL 远程访问修复报告

| 项目 | 内容 |
| --- | --- |
| 服务器 | 192.144.130.3（VM-0-11-ubuntu，Ubuntu 24.04.4 LTS） |
| 部署目录 | `/opt/services/compose/docker-compose.yml` |
| 内网 IP | `10.2.0.11/22` |
| 公网 IP | `192.144.130.3` |
| 修复时间 | 2026-09-15 |

---

## 一、故障原因（共 3 个，缺一不可）

### 1. 整个 Docker 栈根本没有运行

`docker ps -a` 为空 —— `mysql` / `redis` / `nginx` 三个容器全部不存在。
`/opt/services/mysql/data` 数据完好（binlog 一直到停机前一刻），说明只是容器被移除过，**数据没有丢失**。

### 2. MySQL 端口只绑定在回环地址

```yaml
ports:
  - "127.0.0.1:3306:3306"   # 只监听本机 127.0.0.1
```

这样只有服务器自己（以及 SSH 隧道）能连，其它机器必然连不上。

### 3. 【关键】`backend` 网络是 `internal: true`，Docker 不会发布端口

这是"改成 0.0.0.0 也没用"的真正原因。

Docker 在 `internal: true` 的网络上是**不会创建端口映射**的，而且**不报错、静默忽略**：

```
修改端口后仍然如此：
mysql   Up (healthy)   3306/tcp, 33060/tcp          <-- 没有宿主机映射
nginx   Up (healthy)   0.0.0.0:80->80/tcp           <-- 正常（frontend 网络，非 internal）
```

`mysql` 只有 `3306/tcp`（容器内暴露），没有 `0.0.0.0:3306->3306/tcp`，
宿主机 `ss -tlnp` 里也**没有 3306 监听**，所以外部怎么都连不上。

**结论：必须让容器至少挂一条非 internal 的网络，端口才会真正发布出去。**

---

## 二、已做的修改

文件：`/opt/services/compose/docker-compose.yml`（修改前已自动备份，见第六节）

| # | 位置 | 修改前 | 修改后 |
| --- | --- | --- | --- |
| 1 | `mysql.ports` | `"127.0.0.1:3306:3306"` | `"0.0.0.0:3306:3306"` |
| 2 | `mysql.networks` | 只有 `backend` | `backend` + **`dbnet`**（非 internal，否则端口不发布） |
| 3 | `networks` | 只有 `backend` / `frontend` | 新增 **`dbnet`**（`driver: bridge`，非 internal） |
| 4 | `redis.networks` | 只有 `backend` | `backend` + `dbnet`（原 `127.0.0.1:6379` 同样被静默忽略，属于同一个坑） |
| 5 | `nginx.healthcheck` | `curl -sf http://localhost/` | `curl -sf http://localhost/nginx-health` |

第 4、5 项是修复过程中发现的**同类隐藏缺陷**：

* redis 声明的 `127.0.0.1:6379:6379` 以前同样没有生效（宿主机上根本没有 6379 监听）。
  现在已生效，且**仍然只监听 127.0.0.1**，不对外暴露。
* nginx 健康检查访问 `/` 返回 403（html 目录里没有 index.html），导致容器长期 `unhealthy`。
  配置里本来就有 `/nginx-health` 端点（返回 200），直接改用它即可。

数据/缓存容器仍然挂在内网 `backend` 上，**原有的网络隔离结构没有被破坏**。

---

## 三、修复后的验证结果

### 服务状态

```
NAME      STATUS                   PORTS
mysql     Up (healthy)             0.0.0.0:3306->3306/tcp, 33060/tcp
nginx     Up (healthy)             0.0.0.0:80->80/tcp, 0.0.0.0:443->443/tcp
redis     Up (healthy)             127.0.0.1:6379->6379/tcp
```

### 宿主机监听

```
0.0.0.0:3306     docker-proxy      <-- 已发布
127.0.0.1:6379   docker-proxy      <-- 仅本机
0.0.0.0:80/443   docker-proxy
```

### 数据库账号

```
bucktime@%          [mysql_native_password]   GRANT ALL ON `test`.*
root@%              [mysql_native_password]
root@localhost      [mysql_native_password]
```

`skip-name-resolve = 1`、`bind-address = 0.0.0.0`，账号为 `@%`，**服务端本身已无阻碍**。

### 端到端连通性测试

| 测试 | 结果 |
| --- | --- |
| 容器 → `10.2.0.11:3306`（真实端口发布路径）| ✅ `PUBLISHED_PORT_AUTH_OK`，认证身份 `bucktime@%` |
| 本机 → `127.0.0.1:3306` | ✅ 收到 MySQL 8.0.46 握手包 |
| 外网（本机 Windows）→ `192.144.130.3:3306` | ❌ **被阿里云安全组拦截** |
| 外网 → `192.144.130.3:80` | ✅ 通（说明公网链路本身没问题）|
| 外网 → `192.144.130.3:22` | ✅ 通 |
| 外网 → `192.144.130.3:443` | ❌ 不通（安全组同样未放行）|
| 外网 → SSH 隧道 → MySQL 真实握手认证 + 查询 | ✅ `EXTERNAL_MYSQL_ACCESS_OK` |

**结论：服务器端全部修复完成，最后一层拦截在阿里云安全组。**

---

## 四、还需要你去控制台操作的一步 ⚠️

安全组是云平台的访问控制，**只能由你在阿里云控制台放行**（服务器上没有 aliyun CLI 和 AccessKey，我无法代改）。

阿里云控制台 → **ECS → 实例 → 安全组 → 配置规则 → 入方向 → 手动添加**：

| 字段 | 建议值 |
| --- | --- |
| 协议类型 | 自定义 TCP |
| 端口范围 | `3306/3306` |
| 授权对象 | **你自己的公网 IP/32**（例如 `1.2.3.4/32`） |
| 描述 | MySQL remote access |

* 如果"其它机器"是**同一个 VPC 内网**的服务器，授权对象填 `10.2.0.0/22` 即可，比开公网安全得多。
* **不建议**填 `0.0.0.0/0`：3306 裸露在公网会被暴力破解和勒索（MySQL 是被扫描最多的端口之一）。
  当前 `root@'%'` 存在，且密码与业务账号相同，一旦全开风险很高。

放行后告诉我，我再从外部实测一次连通性确认。

---

## 五、现在立刻就能用的方案：SSH 隧道（无需改安全组）

已实测通过（真实完成 MySQL 认证 + 查询）：

```bash
ssh -N -L 13306:127.0.0.1:3306 ubuntu@192.144.130.3
```

然后本机数据库客户端连接：

```
主机: 127.0.0.1   端口: 13306   用户: bucktime   库: test
```

这样 MySQL 完全不暴露公网，安全性最好。Navicat / DBeaver / IDEA 都支持配置 SSH 隧道。

---

## 六、连接信息与运维备注

| 项 | 值 |
| --- | --- |
| 连接地址（安全组放行后）| `192.144.130.3:3306` |
| 业务账号 / 库 | `bucktime` / `test` |
| root 账号 | `root` |
| 密码 | 见服务器 `/opt/services/compose/.env`（`APP_DB_PASSWORD`、`MYSQL_ROOT_PASSWORD`）|
| 配置文件 | `/opt/services/compose/docker-compose.yml` |
| MySQL 参数 | `/opt/services/mysql/config/my.cnf` |
| 数据目录 | `/opt/services/mysql/data` |

**修改前的备份（服务器上，回滚用）：**

```
/opt/services/compose/docker-compose.yml.bak.20260915141134   <-- 最原始版本
/opt/services/compose/docker-compose.yml.bak.20260915141429
/opt/services/compose/docker-compose.yml.bak.20260915142041
```

回滚：

```bash
cd /opt/services/compose
sudo cp docker-compose.yml.bak.20260915141134 docker-compose.yml
sudo docker compose up -d --force-recreate
```

**开机自启**：三个容器 `restart: unless-stopped`，Docker 服务 `enabled`，重启服务器后会自动拉起。

---

## 七、安全建议（建议尽快处理）

1. **`root@'%'` 允许从任意主机以 root 登录**，且密码与业务账号相同。
   建议删除或改为 `root@'10.2.0.%'`：
   ```sql
   DROP USER 'root'@'%';
   ```
   业务连库只使用 `bucktime`（权限已限定在 `test` 库）。
2. 3306 安全组授权对象**尽量限定来源 IP**，不要 `0.0.0.0/0`。
3. `Root@2026!`、`Redis@2026!` 这两个密码强度偏低，建议更换。
4. `test` 库目前还是空的（0 张表），应用尚未建表。