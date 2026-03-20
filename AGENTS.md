# Simple Boot Mock Server - AGENT.md

## 1. 项目概览 (Project Overview)
Simple Boot Mock Server 是一个基于 Spring Boot 开发的轻量级、高性能 Mock 数据服务器。它旨在为前端开发和接口测试提供灵活、强大的 API 模拟能力。

- **核心目标**: 让 API 模拟变得简单而强大，支持局部代理和复杂逻辑模拟。
- **主要仓库**: [fugary/simple-boot-mock-server](https://github.com/fugary/simple-boot-mock-server)

## 2. 技术栈 (Tech Stack)

### 后端 (simple-boot-mock-server)
- **核心框架**: Spring Boot 3.x
- **数据库**: H2 (模型/开发), MySQL (生产支持)
- **持久层**: MyBatis Plus, FlywayDB (数据库版本管理)
- **脚本引擎**: GraalVM JavaScript Engine (执行 MockJS 和自定义脚本)
- **日志**: Logback
- **工具库**: Lombok, CryptoJS (内嵌支持)

### 前端 (simple-boot-mock-newui)
- **框架**: Vue 3
- **基础库**: [simple-element-plus-template](https://github.com/fugary/simple-element-plus-template)
- **编辑器**: Monaco Editor (用于渲染和编辑请求/响应体)

## 3. 核心功能 (Core Features)

| 功能模块 | 说明 |
| :--- | :--- |
| **局部 Mock & 代理** | 支持配置代理地址，仅 Mock 指定请求，其他转发至真实后端。 |
| **MockJS 集成** | 内置 MockJS 语法支持，生成随机生动的测试数据。 |
| **多场景管理** | 一个 API 集合支持多个场景切片，可快速切换响应状态。 |
| **动态响应脚本** | 支持 async/await 调用外部 API，支持 require 加载外部库。 |
| **变量替换** | `{{request.params.id}}` 等语法实现响应内容的动态回显。 |
| **多重匹配** | 根据路径、Method、Body、Headers、Params 等多维度表达式分发请求。 |
| **加解密支持** | 内置 AES, RSA, MD5, SHA256 等算法，支持解密请求/加密响应。 |
| **版本记录** | 自动记录响应数据的修改历史，支持对比与回滚。 |
| **用户权限** | 支持多用户隔离，管理员可全局查看。 |

## 4. 项目结构 (Project Structure)
- `/simple-boot-mock-server`: 后端 Java 项目源码。
- `/simple-boot-mock-newui`: 前端 Vue 3 项目源码。
- `/docs`: 项目文档及静态引导页。
- `/bin`: 启动脚本 (`start.bat`, `start.sh`)。
- `/config`: 外部配置文件目录。

## 5. 当前开发进度 (Current Status)
- [x] v2.12.3 版本发布：修复 Post Processor 多线程冲突及脚本引擎泄漏。
- [x] 场景管理功能完善。
- [x] Mock 字典布局优化及 MockJS 函数提示增强。
- [x] 仪表盘图表增强：Top Active Users, Top Contributors。
- [x] UI/UX 优化：菜单动画、响应式侧边栏等。

---
*Last Updated: 2026-03-09*

## 6. 项目规则 (Project Rules)
为了保证项目的开发的一致性和质量，AI 代理在协作时需遵循项目内置的规则：

详细规则请参考：[.agent/rules/rules.md](.agent/rules/rules.md)

主要包含：
1. **提交日志**: 生成提交日志时请使用 **中文**。
2. **前端开发**: 修改前端 JS、Vue 等文件时需通过 **ESLint** 验证。
3. **进度记录**: 每次提交后及时同步更新 [DEVELOPMENT_LOG.md](DEVELOPMENT_LOG.md)。
4. **架构同步**: 发生重大功能变更时更新 [AGENT.md](AGENT.md)。
