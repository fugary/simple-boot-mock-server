# simple-boot-mock-server

基于Spring Boot开发的Mock数据服务器，主要要两个项目：
1. 后台服务simple-boot-mock-server
2. UI界面simple-boot-mock-ui

最后打包的时候合并到一个项目中，成为一个jar包，最后打包成为一个zip文件，加压运行即可。

目录结构：

/xxxx.jar——可执行jar，可用`java -jar`命令启动，也可以直接用start.bat启动

/data——H2数据库文件（运行后产生）

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
6. H2数据库
7. MockJS（使用JSEngine执行）

## simple-boot-mock-ui

主要基于[vue-admin-template](https://github.com/PanJiaChen/vue-admin-template)开发界面。

使用vue打包后自动复制到simple-boot-mock-server项目下的/src/main/resources/static目录

## 功能介绍

1. 模拟分组，每个分组可以配置多个请求链接
2. 模拟请求预览，支持URL参数，请求参数，请求头信息等预览和保存
   1. 预览同时会保存请求相关参数，方便本地测试
   2. 预览使用highlightjs高亮显示输出
3. 支持响应内容用请求参数替换
   1. 如链接中有参数：`/mock/xxxxx/request?id=xyz`
   2. 输出可以有`{"id": "${id}"}`
4. 可以配置多个响应输出，指定一个作为默认输出。方便在不改变URL的情况下模拟成功、失败等多种情况
5. 支持mockjs语法（直接使用mockjs解析执行）
6. 快速复制URL、响应内容等
7. 支持重定向链接
   1. 响应里面配置重定向的URL
   2. 配置Status Code为302

可以使用`java -jar`，也可以使用快捷的bat或者sh文件运行

Windows下`bin/start.bat`

Linux下`bin/start.sh`

启动后在：http://localhost:9086/ 地址访问

运行参考文档：

https://www.jianshu.com/p/56e0efec455d
