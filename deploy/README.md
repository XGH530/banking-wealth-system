# 部署说明（阿里云轻量应用服务器 + systemd）

线上演示：<http://8.133.222.0:8080>（前端主页）、<http://8.133.222.0:8080/admin.html>（后台管理控制台）

## 一、为什么用 systemd 而不是 nohup

早期用 `nohup java -jar ... &` 手工启动，问题很直接：

- 进程被 **OOM Killer** 杀掉之后**不会自动恢复**，线上直接 502；
- 服务器重启后需要人工登录再拉一次；
- 出问题只有一份 `app.log`，没有统一的运行状态与重启计数。

改成 systemd 之后：`Restart=always` 崩溃自愈 + 开机自启 + `systemctl status / journalctl` 统一观测。

## 二、安装步骤

```bash
# 1) 上传 jar（先停服务，避免覆盖正在运行的 jar）
systemctl stop bank
scp target/bank-account-system.jar root@<IP>:/www/wwwroot/bank-system/target/

# 2) 安装守护配置（记得把 CHANGE_ME 换成真实数据库口令）
scp deploy/bank.service root@<IP>:/etc/systemd/system/bank.service
ssh root@<IP> "chmod 600 /etc/systemd/system/bank.service && systemctl daemon-reload && systemctl enable --now bank"

# 3) 验证
ssh root@<IP> "systemctl status bank --no-pager; curl -sI http://127.0.0.1:8080/login.html | head -1"
```

## 三、内存上限为什么必须加

轻量应用服务器只有 1.6G 内存，上面同时跑着 MySQL、两个 Spring Boot 应用和宝塔面板。
2026-10-11 就是因为内存被吃满，内核触发了**全局 OOM Killer**，把银行进程直接杀掉（日志里能看到
`http-nio-8080-A invoked oom-killer`）。

配置里的 `MemoryMax=560M` + `MemorySwapMax=256M` 让内存约束落在 **cgroup** 上：
应用即使内存失控，也只会在自己的 cgroup 内被杀掉并由 `Restart=always` 拉起，
不会再把整台机器上的其它服务一起带走。

## 四、单独执行本仓库时的启动方式

- Docker Compose：`docker compose up -d`
- 本地开发：见根目录 README 的「本地开发启动」
- 前端口令注意：`application-local.yml` 里的数据库口令会**覆盖环境变量**，
  线上因此使用 `-Dspring.datasource.password=` 注入（系统属性优先级最高）。