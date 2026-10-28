# simple-boot-mock-server

基于Spring Boot开发的Mock数据服务器，主要要两个项目：
1. 后台服务simple-boot-mock-server
2. UI界面simple-boot-mock-newui（2.x升级新ui，从Vue2升级到Vue3）

最后打包的时候合并到一个项目中，成为一个jar包，最后打包成为一个zip文件，加压运行即可。

目录结构：

/xxxx.jar——可执行jar，可用`java -jar`命令启动，也可以直接用start.bat启动

/data——H2数据库文件（运行后产生），可以支持配置MySQL数据

/config——application.yml配置文件

/bin——start.bat和start.sh启动文件

/logs——日志输出目录（运行后产生）

## simple-boot-mock-server服务

### 基于以下框架开发

1. Spring Boot
2. Mybatis、MybatisPlus
3. Lombok
4. Logback
5. FlywayDB
6. H2数据库/MySQL数据库
7. MockJS（使用JavascriptEngine执行）

## simple-boot-mock-newui

新newui升级Vue3，使用基础库：[https://github.com/fugary/simple-element-plus-template](https://github.com/fugary/simple-element-plus-template)

使用vue打包后自动复制到simple-boot-mock-server项目下的/src/main/resources/static目录

## 功能介绍

1. 模拟分组，每个分组可以配置多个请求链接，支持配置代理地址
2. 模拟请求预览，支持URL参数，请求参数，请求头信息、请求体等预览和保存
   1. 预览同时会保存请求相关参数，方便本地测试
   2. 预览使用`monaco-editor`编辑请求体
3. 支持响应内容用请求参数替换，参数用`{{}}`包裹
   1. 如链接中有参数：`/mock/xxxxx/request?id=xyz`
   2. 输出可以有`{"id": "{{request.parameters.id}}"}`
4. 可以配置多个响应输出，指定一个作为默认输出。方便在不改变URL的情况下模拟成功、失败等多种情况
5. 支持`mockjs`语法（直接使用`mockjs`解析执行）
6. 执行响应xml、json、html等，使用JavaScript生成响应数据
7. 快速复制URL、响应内容，在线测试
8. 支持重定向链接
   1. 响应里面配置重定向的URL
   2. 配置Status Code为302
9. 支持作为反向代理，代理`URL`地址，可以只`mock`部分请求，其他请求发送给代理`url`地址获取响应
10. 除了支持`URL`路径和请求方法匹配，新增支持匹配表达式，可以根据`request`内容匹配请求
    1. `request.body`——`body`内容对象（仅`json`时会解析成对象）
    2. `request.bodyStr`——`body`内容字符串
    3. `request.headers`——头信息对象
    4. `request.parameters`——请求参数对象
    5. `request.pathParameters`——路径参数对象
    6. `request.params`——路径参数和请求合并对象
    7. `request.ip`——客户端IP
    8. `request.userAgent`——User-Agent
    9. `request.cookies`——Cookie对象
11. 支持匹配表达式测试功能，方便测试表达式是否正确
12. 增加响应延迟，可以模拟延迟一段时间才响应数据
13. 支持简单用户管理，默认账号：`admin/12345678`和`mock/mock`两个用户，支持修改密码，使用`SHA256`加密算法
14. 各用户管理自己的`mock`分组数据，管理员可以看到其他人的数据
15. 除了H2数据库，增加MySQL数据库支持
16. 支持通过内置函数，实现解密请求或加密响应

## GitHub Actions 发布

在 GitHub 的 **Actions → Build and Release → Run workflow** 中选择 `master`，在 `tag_name` 输入正式版本号（如 `2.14.8` 或 `v2.14.8`）即可发布，无需提前修改版本文件或创建 tag。

工作流会同步父子 Maven POM、前端 `package.json` 和 `package-lock.json` 的版本，校验并构建。构建成功后，自动提交版本变更并创建 `v版本号` tag，然后发布 ZIP 安装包和 Docker 镜像。自动提交需要仓库规则允许 `GITHUB_TOKEN` 写入 `master` 和 tag；Docker Hub 使用已有的 `DOCKERHUB_USERNAME`、`DOCKERHUB_TOKEN` secrets。

仍支持推送 `v*` tag 触发构建，构建时自动使用 tag 中的版本号。仅支持 `主版本.次版本.修订号` 格式的正式版本；旧版本不会覆盖 GitHub Release 和 Docker 的 `latest`。未勾选覆盖选项时，手动发布不覆盖已有 tag，失败后请使用原运行的 **Re-run all jobs** 重试。若构建期间 `master` 有新提交，自动提交会被拒绝，请重新运行工作流。

### 同版本覆盖发布

小修复提交到 `master` 后，保持前后端版本号不变，在 `tag_name` 输入已有版本并勾选 **覆盖已有版本**（默认关闭）。仅允许覆盖最新正式版本，版本文件必须与输入一致；构建成功后更新原 tag，替换同名安装包和版本镜像。

Release 正文继续使用原有分类和过滤规则，包含原版本已有记录及新增修复，不显示“同版本重新发布”等额外段落。标签注释保留运行标识、原提交和重新发布时间，结合标签指向及 Actions 日志追溯构建。

需要仓库规则允许 Actions 更新目标标签；不可变 Release 无法覆盖。工作流检查分支变更、标签冲突、历史回退和新版本标签，按预期旧标签对象更新，绝不强制推送分支。更新标签后失败可重跑原任务，复用原构建提交和追溯信息。Git、Release、Docker 无法跨服务原子更新；已有安装和容器需重新下载或拉取镜像，本地旧标签也需刷新。

本地发布流程回归验证（Python 3.10+、PyYAML、Git Bash）：`python .github/scripts/test-release.py`。测试使用临时本地仓库和模拟 API，不执行线上发布。

## 运行

可以使用`java -jar`，也可以使用快捷的bat或者sh文件运行

Windows下`bin/start.bat`

Linux下`bin/start.sh`

Docker启动：`docker run -p 9086:9086 fugary/simple-boot-mock-server:latest`

Docker地址： https://hub.docker.com/r/fugary/simple-boot-mock-server

启动后在：http://localhost:9086/ 地址访问

默认账号：admin/12345678和mock/mock两个用户

运行参考文档：

[https://fugary.com/?p=626](https://fugary.com/?p=630)
