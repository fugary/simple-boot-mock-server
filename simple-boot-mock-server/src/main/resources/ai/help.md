# Simple Boot Mock Server 本地功能指南

本文随应用发布，依据仓库 README、MockTips 帮助页及其中英文使用指南整理。维护功能时应同步更新本文，避免将设计文档中未实现的能力作为现有功能。以下内容用于帮助问答，不包含用户的项目数据、接口配置、密钥或日志。

## 项目、分组与代理
Simple Boot Mock Server 是 API Mock 服务。项目组织分组，分组中维护请求及其响应数据。分组可配置代理地址，只 Mock 指定请求，其余请求转发到真实后端。请求支持 URL 路径和 HTTP Method 匹配，也可通过匹配表达式根据请求内容区分请求。不能仅凭用户描述推断其实际分组配置或匹配结果，应引导其查看诊断。

## 基础响应与 MockJS
响应可以填写静态 JSON，也可使用 JavaScript 和 Mock.mock(data) 生成数据。基础示例：
```javascript
Mock.mock({
  'data|10': [{ id: '@integer(1, 10000)', name: '@name', birthday: '@date' }]
})
```
脚本可用立即执行函数返回结果：
```javascript
(function () {
  return { id: request.params.id, body: request.body };
}())
```
帮助页的基础示例、内置对象、内置函数及后置处理页提供可复制示例，复制后按实际接口调整。

## 请求对象与匹配
request.body 是请求体对象；request.bodyStr 是请求体字符串；request.headers 是请求头对象；request.parameters 是请求参数；request.pathParameters 是路径参数；request.params 合并请求参数和路径参数。还可读取 request.ip、request.userAgent、request.cookies。
匹配表达式根据 request 内容返回匹配结果，支持表达式测试。需要区分相同路径的不同请求时，可结合 Method 和表达式检查参数、请求头或请求体。不要猜测未提供的匹配优先级。

## 响应与后置处理
response.statusCode 是状态码；response.headers 是响应头；response.body 是响应体对象；response.bodyStr 是响应体字符串。后置处理可修改状态码和响应头并返回新响应体，例如：
```javascript
(function () {
  response.statusCode = 200;
  response.headers['Content-Type'] = 'application/json';
  return xml2Json(response.bodyStr);
}())
```

## 异步请求与模块
脚本支持 fetch(url, options) 和 async/await：
```javascript
(async function () {
  const response = await fetch('https://httpbin.org/put', {
    method: 'put', body: JSON.stringify({ username: 'example' })
  });
  return await response.json();
}())
```
require(url, options) 是异步 CommonJS 风格模块加载，返回 Promise，使用 await require(...)。支持 module.exports 和 exports，不支持 ESM export/import。默认按 URL 缓存模块，帮助页列有 clearRequireCache 缓存清理函数。不要把完整 Node.js 运行环境或 npm 包自动安装能力视为本项目能力。

## 内置工具与加解密
内置 Mock.mock、dayjs、xml2Json(xmlStr)、json2Xml(jsonStr, rootName)、encodeHex、decodeHex、btoa、atob。提供 md5Hex/md5Base64、sha1Hex/sha1Base64、sha256Hex/sha256Base64；encryptAES/decryptAES、encryptDES/decryptDES、encrypt3DES/decrypt3DES、encryptSM4/decryptSM4、encryptRSA/decryptRSA，以及 signRSA/verifyRSA。加密调用接受 data、key、options，具体算法选项须以本项目帮助示例和实现为准；本文未列出默认模式和填充，不应猜测。

## 请求诊断
在请求测试结果或请求日志中打开诊断，查看流程及明细，定位分组、场景、请求匹配、响应选择和后置处理等阶段的问题。轮询分组的计算结果和回退原因也可在诊断中查看。助手不能自行读取这些日志，用户需提供必要的脱敏信息。

## 响应轮询
将响应选择方式设为轮询，可依次返回候选响应。轮询分组留空时接口共用进度；填写 request.params.orderId 或 request.body.pnr 等表达式后，不同结果分别计数。表达式必须返回非空字符串或有限数字，无效值或求值失败回退到接口共用进度。

## Schema 与复杂参数
测试请求时，在支持对象编辑的参数行点击“编辑对象参数”，按结构编辑复杂参数。请求关联 Schema 时，可查看结构并使用可用的示例生成入口；响应编辑器也支持依据响应 Schema 生成示例。生成后先检查，再发送或保存。

## 场景与置顶
在分组的场景管理中维护场景，为请求指定所属场景，再切换分组当前场景进行联调。请求列表中的场景筛选用于查看对应请求。项目、分组、请求支持通过星标或置顶操作排列常用记录。

## 导入
在分组列表打开导入，选择目标项目并上传文件。支持 HAR（HTTP Archive 1.2）、Swagger 和 Postman 等格式。选择文件后会尝试识别格式，出现类型冲突时核对并切换到识别结果。单文件和总大小限制以导入窗口显示的实际配置为准，不能猜测固定限制。

## AI 配置、日志与帮助提问
在系统管理的 AI 配置中添加 OpenAI 兼容、Anthropic 或 Gemini 配置，填写接口地址、密钥和模型。OpenAI 和 Anthropic 地址通常包含 /v1，Gemini 通常包含 /v1beta。模型支持刷新加载或手填；保存后可测试提示词。测试时临时选用的模型只影响本次调用。
编辑时仅在供应商和接口地址未变更时可将密钥留空保留原值，变更供应商或地址需重新填写密钥。AI 日志支持按配置、关键词、状态筛选及查看请求、响应、错误详情和删除。
帮助页的 AI 提问使用当前登录用户已启用且设为默认的 AI 配置及其默认模型，没有该配置时需先设置。每次提问独立，不包含之前的问答。后端把本指南和当前问题发送给配置的 AI 服务，调用记录进入现有 AI 日志。不会自动读取或发送用户的 Mock 数据。回答应引用本文章节；本文没有覆盖的功能应明确告知无法确认。生成的示例需检查后再使用。
