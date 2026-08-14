# 开发进度日志 (Detailed Development Log)

本文档完整记录了 `simple-boot-mock-server` 从 2024 年至今的详细开发历程、功能迭代及维护记录。

## 2026年
### 2026-08
- **opt**: [2026-08-14] 优化 Mock 诊断流程中的延迟记录逻辑：
  - 在 `MockDiagnoseRecorder.delayResolved` 中增加过滤条件，当配置延迟与实际等待时间均 $\le 0$（未配置延迟或 `delay=0` 且未产生实际等待）时，跳过记录 `CODE_DELAY_RESOLVED` 诊断步骤；
  - 避免导入含有 `delay: 0` 的数据在调用时在诊断流程图里显示多余的“应用延迟”空步骤，使诊断链路更简洁清晰；
  - 增加对应单元测试用例并通过测试验证。
- **fix**: [2026-08-14] 修复 FastMock 函数解析与参数解构异常：
  - 接管 Mock.js 核心分发器 `Mock.Handler.function`，在函数执行时动态注入当前请求入参 `{ _req, request, Mock }`，并保持 `this` 上下文指向父级对象，完美兼容 Mock.js 原生规则与 FastMock 解构语法；
  - 优化 `MockGroupServiceImpl` 在 `responseFormat == "javascript"` 下对 JSON 格式对象模板的处理，统一进入 `mock` 解析流程，自动包裹 `Mock.mock()` 且保留大 JSON 正则快筛性能；
  - 消除每次请求重复 eval `fastmock.js` 引发的 Polyglot 跨上下文拦截异常（`unexpected interop primitive`），在引擎初始化时统一加载。
- **opt**: [2026-08-14] 限制 Mock 数据导入文件类型与完善格式白名单校验：
  - 前端增加原生文件选择类型过滤：为 `el-upload` 配置 `accept=".json,.yaml,.yml,.har"` 属性，默认仅允许用户在操作系统文件选择器中选取合法格式文件；
  - 前端增加文件扩展名白名单双重即时校验：在 `onFileListUpdate` 和 `doImportGroups` 阶段对选中文件进行后缀名校验（`.json`, `.yaml`, `.yml`, `.har`），若发现不支持的格式（如 `.xlsx` 等）立即拦截并剔除，弹出国际化友好提示，并在上传按钮旁明确提示支持的文件类型；
  - 后端增加文件扩展名安全校验与防御：在 `MockGroupServiceImpl.toImportGroups` 中调用 `SimpleMockUtils.isSupportedImportFile` 对上传文件进行扩展名校验，遇到非法后缀提前拦截并返回多语言国际化错误消息（`simple.error.code.2003.unsupported`），避免对二进制等非文本文件进行无效的内容读取与解析；
  - 完善单元测试与代码质量：在 `SimpleMockUtilsTest` 中增加 `isSupportedImportFile` 覆盖率测试用例，并通过 ESLint 与全量 Maven 单元测试。
- **opt**: [2026-08-14] 优化大文件上传校验与超限提示体验：
  - 前端增加文件大小即时校验拦截：在文件选择（`onFileListUpdate`）及导入提交（`doImportGroups`）阶段通过 `file.size` 进行秒级拦截，当单文件超出 10MB 限制时自动从列表中剔除超限文件并友好提示文件名称与实际大小（如 `文件【xxx.har (21.86 MB)】超过最大限制（最大10 MB）`），避免大文件无效上传及网络带宽浪费；
  - 前端新增易读文件大小格式化函数 `formatFileSize`（支持 B/KB/MB/GB/TB 转换），导入窗口中的提示文案联动动态显示单文件大小上限与数量限制；
  - 后端 `GlobalExceptionHandler` 增加对 `MaxUploadSizeExceededException` 与 `MultipartException` 的专门捕获与处理，新增错误码 `CODE_2005` 并支持中英文国际化消息（`上传文件大小超过限制（最大{0}）`），动态格式化限制大小，杜绝向前端直接暴露底层 Java 异常堆栈信息；
  - 编写并完善前后端代码质量校验：通过 ESLint 校验，新增 `GlobalExceptionHandlerTest` 与 `SimpleMockUtilsTest` 单元测试并通过全量测试。
- **feat**: [2026-08-14] 实现数据导入格式提前验证与智能类型识别：
  - 前端增加轻量文件内容指纹嗅探（`detectImportFileType`），支持 64KB 切片秒级识别 Simple Mock、Swagger/OpenAPI (JSON/YAML)、Postman、HAR、FastMock 5 种数据格式；
  - 导入弹窗交互升级：上传文件时自动匹配切换对应格式并给出微提示，若用户手动选择的格式与文件内容冲突，展示醒目警告并提供“一键切换”按钮，点击确定时提供纠错二次确认拦截，彻底杜绝手误选错类型；
  - 后端 `MockGroupImporter` 接口增强 `getType()`、`getTypeName()`、`match(data)` 和 `detectImporter()` 特征指纹识别体系；
  - 后端导入解析失败时提供智能错误诊断与多语言国际化转换，精准识别并返回“当前选择类型为【当前Mock服务】，但文件格式检测为【FastMock服务】格式，请核对导入类型”或格式损坏的具体提示，透传友好名称与错误诊断原因。
- **opt**: [2026-08-14] 根据 `/code-review-optimize` 规范深度审查与重构代码：提取并统一各 Importer 的 `isSupport` 默认实现至 `MockGroupImporter` 接口，`findImporter` 与 `detectImporter` 采用 Java 11 Stream API 简化；导入类型名称支持多语言配置；统一 `match` 方法的参数校验规则；执行并全部通过了 `npm run lint` 和单元测试，保证整体逻辑健壮与代码优雅。
- **opt**: [2026-08-14] 精简 HAR 导入过滤逻辑：移除主观臆断的静态资源扩展名正则过滤（`STATIC_RESOURCE_PATTERN`），仅保留基础 HTTP/HTTPS 协议与有效 URL 校验；将 Header 过滤改造为前缀匹配（如 `sec-`、`if-`、`access-control-` 等），大幅精简硬编码常量并防止枚举遗漏。
- **fix**: [2026-08-14] 修复 H2 数据库中因 BIT(BOOLEAN) 与数值 1 直接比较报错 `Values of types "BOOLEAN" and "INTEGER" are not comparable` 的语法问题：置顶排序 SQL 调整为 `case when top_flag = true then 0 else 1 end`，完美兼容 H2 与 MySQL。
- **opt**: [2026-08-14] 优化 HAR 文件导入细节：移除抓包实际响应耗时作为 Mock 接口默认 delay 的逻辑（保持 0 延时即时响应）；将未包含自定义注释的请求名称（`requestName`）置空，避免在左侧请求列表中重复展示两遍相同的 URL Path。
- **fix**: [2026-08-14] 修复 Mock 分组与项目置顶排序中因 `top_flag` 字段可能为 `NULL` 导致新创建/导入数据被排在最后一页的 Bug：使用 `CASE WHEN (top_flag = true) THEN 0 ELSE 1 END` 强健排序，实体默认初始化 `topFlag = false`，并通过 Flyway 补丁脚本 `V2_0_53` 修复历史数据。
- **opt**: [2026-08-14] 优化 Mock 分组导入交互：导入成功后自动将列表分页重置定位到第 1 页 (`loadMockGroups(1)`)，便于用户即时查看最新导入的数据记录。
- **fix**: [2026-08-14] 修复 HAR 导入时因响应头过多导致数据库 `headers` 列超长报错的问题：强化响应头过滤（过滤 CORS、CSP、STS 等浏览器安全与缓存策略头），并增加 `MAX_SAFE_HEADERS_LENGTH` 安全截断防护。
- **ui**: [2026-08-14] 优化导入弹窗的“数据来源”控件：将分段单选框 (`segmented`) 调整为下拉选择框 (`select`)，解决选项过多时排版溢出与换行问题。
- **feat**: [2026-08-14] 新增支持 HAR (HTTP Archive 1.2) 抓包文件一键导入：基于 `har-reader` 解析浏览器（Chrome DevTools）及抓包工具导出的 HAR 文件，智能过滤静态资源，支持多分组（按 Host 聚合）与合并单分组模式，自动解析 Path、Method、Query、Headers、RequestBody（支持 JSON/Form 等）以及 Base64 编码的响应数据。
- **opt**: [2026-08-14] 优化 Mock 项目与分组列表的“我的数据”Switch显示与默认行为：针对非管理员用户默认开启“我的数据”筛选以聚焦个人工作台；优化路由锁定具体项目时的联动，隐藏开关并忽略 `onlyMine` 过滤参数，避免误过滤共享项目下的分组。
- **ui**: [2026-08-13] 在 Mock 项目与 Mock 分组的新增/编辑弹窗的“状态”行增加“置顶” (`topFlag`) 开关；为“公开项目”补全“是/否”文字标识，并将三项开关比例调整为 4 : 3 : 3，彻底解决了弹窗内部开关间距不匀及显示更多选项时的重叠挤压问题。
- **opt**: [2026-08-13] 优化分组修改历史记录与对比视图：在历史列表与版本对比窗口中同步补齐“置顶” (`topFlag`) 状态及激活场景的展示，并优化历史记录列排版。
- **feat**: [2026-08-14] 增加请求 (MockRequest) 的置顶 (topFlag) 功能，与项目 (MockProject) 和分组 (MockGroup) 形成统一的星标置顶交互与优先排序体系。
- **feat**: [2026-08-13] 增加项目 (MockProject) 和分组 (MockGroup) 的置顶 (topFlag) 功能，支持将常用项目和分组置顶排在列表最前。
- **opt**: [2026-08-13] 彻底重构项目/分组列表的置顶与排序逻辑：移除局限的 Vue 前端分页数据局部排序，全面拥抱后端数据库强排序（优先默认项目 > 优先置顶 > 优先我的创建）。同时精简和优化了前台置顶星标的 CSS Hover 交互展示，清除了历史遗留的无效冗余样式。
- **ui**: [2026-08-07] 优化 Mock 项目列表卡片的布局样式：移除突兀的顶端蓝渐变背景，优化主题卡片阴影与 Hover/Selected 边框交互，增加卡片内部行间距及创建/修改时间的排版。
- **feat**: [2026-08-03] Mock请求列表项增加右键菜单，对应包含编辑、复制、规则测试、历史记录和删除等操作功能。

### 2026-07
- **opt**: [2026-07-02] 优化 loadSchemas 接口返回的数据量：对于包含大量组件的合并分组，不再返回完整的 component schema，而是利用正则表达式 `Pattern.compile("\"(?:#/components/schemas/|#/definitions/)([^\"]+)\"")` 递归提取当前 schema 实际引用的 components 并进行过滤裁剪。
- **bug**: [2026-07-01] 修复 Swagger 导入为单分组时，未合并各个子分组的 component schema 导致生成的 Schema 测试数据缺失引用的问题。
- **bug**: [2026-07-01] 修复 Swagger 导入时嵌套 Schema 引用的解析丢失问题，增加对 `components/schemas` 的递归发现逻辑。
- **bug**: [2026-07-01] 修复 Swagger 导入时解析包含循环引用的 JSON 导致 StackOverflowError 的问题 (`resolveFully` 设为 `false`)。

### 2026-04
- **refactor**: [2026-04-03] 将项目主关联从 `projectCode` 迁移为 `projectId`，保留默认项目 `default` 的兼容逻辑。
- **db**: [2026-04-03] 新增 Flyway 迁移脚本，自动为分组与协作成员回填 `project_id` 并修复历史数据。
- **ui**: [2026-04-03] 前端项目选择、分组复制与导入流程补充 `projectId` 透传，非默认项目优先按主键关联。
### 2026-03
- **feat**: [2026-03-30] 实现项目级别的协作共享功能，支持精细化的可读、可写、可删除权限分配。
- **docs**: [2026-03-09] 更新 `AGENT.md` 以引用 `.agent/rules/rules.md` 中的规则，减少内容冗余。
- **docs**: [2026-03-09] 生成项目管理文档 `AGENT.md` 和详细开发进度日志 `DEVELOPMENT_LOG.md`。
- **bug**: [2026-03-06] 修复 Post Processor 多线程序列化 GraalVM 对象冲突。
- **opt**: [2026-03-06] 优化脚本引擎池管理，确保异常情况下连接正确释放。
- **release**: [2026-03-06] 发布 v2.12.3 稳定版。
- **feat**: [2026-03-05] 实现场景映射 (Scenario Mapping) 核心功能，支持 Mock 分组关联活跃场景。
- **bug**: [2026-03-05] 修复编辑场景分组时的界面崩溃问题。
- **feat**: [2026-03-05] 增加场景复制功能，支持克隆已有数据配置。
- **opt**: [2026-03-05] 统一 Mock 分组列表排序规则。
- **feat**: [2026-03-02] 仪表盘新增 "Top Active Users" 与 "Top Contributors" 统计图表。

### 2026-02
- **ui**: [2026-02-27] 仪表盘图表交互美化，优化配色方案。
- **refactor**: [2026-02-27] 整合通用 VO (Value Object)，精简代码结构。
- **bug**: [2026-02-24] 修复左侧菜单展开动画丢失问题。
- **opt**: [2026-02-24] 优化 Mock 分组编辑体验，移除窗口遮罩提升操作效率。
- **bug**: [2026-02-24] 修复空 `proxyUrl` 数组导致的界面逻辑错误。
- **feat**: [2026-02-14] 文档指南支持移动端响应式侧边栏。
- **docs**: [2026-02-12] 全面优化 Mock 服务使用教程文档。

## 2025年
### 2025-10 ~ 2025-12
- **opt**: [2025-10-24] 前后端同步支持从历史记录中一键恢复数据。
- **fix**: [2025-10-24] 升级构建脚本，简化打包流程。
- **bug**: [2025-10-24] 修复请求预览自动完成表单未弹出问题。
- **bug**: [2025-10-24] 修复请求预览时参数丢失问题。
- **feat**: 完善异步推送 (SSE/WebSocket) 接口原型测试。

### 2025-08 ~ 2025-09
- **release**: [2025-08-28] 版本号更新与基线维护。
- **feat**: 增强内置函数库，支持更多哈希算法（SHA-3, HMAC）。
- **opt**: 优化 Javascript 脚本执行沙箱安全性。

### 2025-01 ~ 2025-07
- **feat**: 引入外部 JS 库动态加载机制 (`require` 功能)。
- **feat**: 支持 `async/await` 调用外部 Web 服务进行数据聚合。
- **ui**: 实现 Monaco Editor 高亮显示自定义 Mock 语法。
- **opt**: 大幅提升大数据量下 H2 数据库的查询响应速度。

## 2024年
### 2024-07
- **feat**: [2024-07-04] 实现请求测试工具，支持在线模拟 API 调用。
- **feat**: [2024-07-03] 增加 Request 匹配规则，支持复杂的 JS 逻辑判定。
- **feat**: [2024-07-02] 实现请求预览功能及 Monaco Editor 内容编辑优化。
- **feat**: [2024-07-01] 全面支持 Body 请求内容匹配与 MockJS 实时动态解析。

### 2024-06
- **refactor**: [2024-06-30] **重大更新**：基于 `simple-element-plus-template` 彻底重构前端 UI，迁移至 Vue 3 + Element Plus。
- **feat**: [2024-06-30] 实现 Mock 链接快速复制与分享功能。
- **refactor**: [2024-06-29] 升级 Spring Boot 核心依赖，H2 数据库架构调整。
- **feat**: 实现 Mock 请求代理模式，支持按需拦截。

### 2024-05
- **init**: 项目立项，完成核心 Mock 引擎原型开发。
- **feat**: 实现分组管理、项目隔离与基础 Mock 数据维护。
- **db**: 引入 Flyway 数据库迁移工具，确保架构平滑演进。

---
*注：本日志基于完整的 Git 提交历史 (2024-2026) 进行深度挖掘与分类汇总。*
