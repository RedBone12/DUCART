# 生产部署与恢复

这套配置保留 Spring Boot、MySQL 和本地图片文件，使用单台 Linux 服务器的
Docker Compose 部署。`compose.yaml` 仍用于本机开发；生产配置独立位于
`deploy/compose.yaml`。GitHub Pages 仍只发布静态前端。

仓库中的配置和本地恢复演练不代表已经云端上线。公网部署需要实际服务器、
域名 DNS、正式 SMTP 配置；系统备份定时器也需要在服务器上安装后才会运行。

## 入口与持久化

- Caddy 对外只发布 TCP 80/443，HTTP 重定向至 HTTPS；公网域名自动申请及续期证书。
- React 和 `/api/*` 使用同一 HTTPS 域名，网关移除 `/api` 前缀后转发至 Spring Boot。
  `/uploads/*` 转发图片；`/healthz` 公开不含内部详情的数据库就绪状态。
- MySQL、后端 8080 和管理端口 8081 不发布到宿主机。MySQL 使用独立内部网络。
- MySQL、图片、Caddy 证书及配置分别使用持久卷，重新创建容器不删除数据。
- `prod` profile 使用 Flyway 和 `ddl-auto=validate`，关闭 SQL 明文日志。
  CORS 仅允许 `PUBLIC_ORIGIN`，不会自动允许开发用 localhost 来源。
- 后端就绪检查包含数据库，数据库不可用时返回失败。存活检查不把数据库故障
  当成 JVM 死亡。Docker 本身不会因为容器变成 unhealthy 就自动重启它。

## 首次部署

服务器需要 Docker Engine、Compose v2、Node.js 22 和 Git。域名 A/AAAA 记录必须
指向这台服务器，80/443 必须能从公网访问；SSH 访问按实际端口配置。
以下命令均在仓库根目录执行，使用同一个有 Docker 权限的部署账号。

```sh
node scripts/ops.cjs init --env deploy/production.env --origin https://shop.example.com
```

此命令生成随机数据库密码和 JWT 密钥，拒绝覆盖已有文件。Linux 文件权限为
0600；Windows 使用所在目录的 ACL，应放在自己的受保护目录。
编辑生成文件中的 SMTP 主机、用户名、密码和发信地址。当前正式 SMTP 配置使用
STARTTLS，默认 587 端口；未配置的示例 SMTP 会阻止正式部署。
不要提交环境文件或把 `docker compose config` 的完整输出发布到日志。

```sh
node scripts/ops.cjs build --env deploy/production.env --project ducart-production --release v1
node scripts/ops.cjs deploy --env deploy/production.env --project ducart-production --release v1
node scripts/ops.cjs verify --env deploy/production.env --project ducart-production
```

`--release` 使用新的不可变版本名，例如提交 SHA。构建拒绝覆盖已存在的后端版本。
部署记录实际镜像 ID，检查 HTTPS 证书、跳转、SPA 刷新、商品 API、数据库就绪、
CORS 和前端 `release.json`。正式模式使用系统 CA 信任，不跳过证书验证。
首次启动会获取缺失的基础镜像，不会主动升级已有基础镜像；MySQL 8.4/Caddy
升级应单独验证，不能用替换应用镜像的回滚流程代替数据库引擎升级。

已有正式环境再次部署时，脚本先阻止 POST/PUT/PATCH/DELETE，停止并排空后端
请求，备份数据库和图片，再启动候选版本。只读检查通过后才恢复写入。
失败时恢复上一个快照的数据库、图片和镜像，验证后恢复写入，并返回失败退出码。
首次部署失败则停止应用，保留数据库供诊断，不能自动删除未知数据。
这是一套有短暂维护窗口的部署流程，不是零停机或多副本方案。

## 数据库迁移

`V1__initial_schema.sql` 创建当前应用的 14 张表、唯一约束和外键；Flyway 另建
历史表。之后使用 `V2__...sql` 等新版本，禁止修改已经执行的迁移。
MySQL DDL 不能保证整体事务回滚，因此部署前的完整快照是迁移失败恢复路径。
失败部署会先保存私有的后端日志和当时的数据库结构到 `deploy/.state/failures/`，
再恢复快照；这些诊断文件不上传为公开 CI artifact。

`baseline-on-migrate` 默认关闭。已有 Hibernate 管理的库直接切换到 prod profile
会被拒绝，不能为了启动成功而盲目 baseline 或执行 Flyway clean。

迁移旧数据时应保留原库：先备份原库及 uploads，在另一个 Compose 项目中让
Flyway 创建新库，再导入经过核对的数据。一个可审查的数据导出命令是：

```sh
docker compose --env-file .env exec -T mysql sh -ec \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -u root --single-transaction --no-create-info --complete-insert --skip-triggers --skip-add-locks --skip-disable-keys --set-gtid-purged=OFF --hex-blob "$MYSQL_DATABASE"' > legacy-data.sql
```

这条命令只读取旧库。先核对源/目标列、唯一约束、外键、订单金额和图片路径，
在隔离环境导入、比对记录及实际登录下单，再切换流量。若源库已有 Flyway 历史，
不能把历史表作为普通业务数据导入另一条迁移历史。
本次合成演练不会替你迁移现有业务库。

## 备份与恢复

```sh
node scripts/ops.cjs backup --env deploy/production.env --project ducart-production
```

完整快照保存在 `backups/<project>/<timestamp>/`：

| 文件 | 内容 |
| --- | --- |
| database.sql | 业务库及 Flyway 历史，包含重建表所需结构 |
| uploads.tar | 与数据库一致的图片快照 |
| images.tar | 对应前后端镜像，可在新主机加载 |
| manifest.json | 版本、镜像 ID 和上述文件的 SHA-256 校验 |

应用镜像先保存，再在维护窗口内停止后端并导出数据库和图片；只有全部成功后
才写 manifest。恢复会先验证全部校验和及目标项目，再停止服务、重建指定数据库、
替换指定图片卷并恢复对应镜像。校验失败不会改动业务数据。
快照包含用户资料及密码哈希，环境文件还包含实际密钥；应保存到受保护的位置，
加密复制到另一台机器或远程存储。单机上的备份不能抵御整台机器丢失。
环境文件需要单独保管；更换 JWT 密钥会使原登录令牌失效。

实际恢复前先保存当前环境快照，检查目标项目名称：

```sh
node scripts/ops.cjs restore --env deploy/production.env --project ducart-production \
  --backup /secure-backups/snapshot-directory --confirm-project ducart-production
```

新服务器使用相同架构，恢复源代码、Node/Docker 和正式环境文件后执行同一命令；
镜像不在本地时会从 `images.tar` 加载。跨项目演练需显式加 `--allow-cross-project`，
目标数据库名必须相同。`--restore-images` 可强制验证镜像归档的加载路径。
恢复前应确认空目标，或已备份目标现有数据。业务恢复不依赖 Caddy 私有 CA：
公网证书可重新申请，域名和公网端口仍须满足签发条件。

## 回滚与恢复写入

```sh
node scripts/ops.cjs rollback --env deploy/production.env --project ducart-production \
  --confirm-project ducart-production --restore-data
```

这个命令回到上次部署前的数据库、图片和镜像；成功发布后新增的数据会被回退，
所以必须显式给出 `--restore-data`。自动失败回滚期间写入一直被阻止，不存在
已经向用户确认的新版本写入需要保留的问题。若恢复步骤失败，保持维护状态，
诊断并再次恢复；不要直接删除维护标记。

```sh
node scripts/ops.cjs resume --env deploy/production.env --project ducart-production
```

`resume` 只有在完整部署验证通过后才解除写入限制。
不要在正式项目执行 `down --volumes`，否则会删除持久数据。

## 定时备份

`deploy/ducart-backup.service` 和 `.timer` 是 Linux systemd 模板。调整账号、
仓库位置和 Node 路径后，在服务器安装并验证：

```sh
sudo cp deploy/ducart-backup.service deploy/ducart-backup.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl start ducart-backup.service
sudo systemctl enable --now ducart-backup.timer
systemctl list-timers ducart-backup.timer
journalctl -u ducart-backup.service --no-pager
```

模板每天执行一次，包含短暂维护窗口；仓库文件本身不代表定时器已经启用。
定期检查剩余磁盘和异地复制结果，保留最近的有效快照及状态文件引用的回滚快照。
每日备份的灾难恢复点最多可能落后约一天；实际恢复耗时以演练报告为准。

## 隔离恢复演练

Docker 已启动时，在 Windows PowerShell 或 Linux 仓库根目录执行：

```sh
node scripts/recovery-drill.cjs
```

脚本构建生产镜像，使用随机项目名、随机本机端口、本地 CA 和合成账号，实际验证：

1. HTTPS 证书及重定向、同域 API、生产 CORS、私有端口和数据库就绪。
2. 注册、登录、管理员图片上传、下单及持久订单。
3. 维护期间拒绝写入；重建容器后数据库与图片仍在。
4. 数据库故障时就绪检查失败，数据库恢复后重新就绪。
5. 拒绝损坏快照，故意损坏数据库/图片后从有效快照恢复。
6. 使用全新卷和镜像归档恢复第二个独立项目，并读取原订单和图片字节。
7. 主动回滚；故意让 Flyway 在执行部分 DDL 后失败，验证新增表被移除，
   原结构、镜像、订单与图片都恢复。

客户端加载演练 CA 验证证书，不使用关闭 TLS 校验的开关。
默认结束后删除随机测试项目的容器、卷、合成备份与环境文件；`--keep` 可保留现场。
报告位于 `deploy/drill-results/latest.json`，不包含环境密钥或业务备份。
可用 `--release <已有演练版本>` 重用已构建的镜像。
CI 的 `Deployment recovery` 检查执行同一流程，上传报告，发布依赖全部五项检查。
