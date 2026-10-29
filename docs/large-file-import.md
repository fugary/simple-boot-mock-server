# 大文件导入配置

导入默认支持单文件 256 MiB、一次 multipart 请求 512 MiB，仍最多选择 3 个文件。请求上限包括所有文件、表单字段和 multipart 边界，因此多个文件的总大小必须小于请求上限。

在外部 application.yml 配置并重启服务：

```yaml
simple:
  mock:
    max-upload-file-size: 268435456
    max-upload-size: 536870912
server:
  tomcat:
    max-swallow-size: 512MB
```

两个上传参数使用字节数，-1 表示不限制。也可使用环境变量 MOCK_MAX_UPLOAD_FILE_SIZE、MOCK_MAX_UPLOAD_SIZE、MOCK_MAX_SWALLOW_SIZE。旧的 simple.mock.max-upload-size 仍代表整个请求上限；自定义旧值会继续生效。系统使用自定义 CommonsMultipartResolver，不能仅调整 spring.servlet.multipart 参数。

导入窗口从 /admin/importConfig 读取当前上限，展示单文件和请求总量，并在提交前校验。请求边界开销由后端最终校验。超过限制沿用错误码 2005。上传不受前端通用短超时限制，但代理或负载均衡仍可能设置独立大小和超时限制；部署时需同步调整。Tomcat max-swallow-size 控制拒绝请求后的剩余请求体读取，有助于客户端收到超限错误，不是有效上传大小的主限制。

大于 64 KiB 的上传由 multipart 解析器存放临时磁盘，需为 JVM 临时目录保留足够空间。Simple 原生 JSON 直接从文件流绑定为导入对象，避免成功导入时再创建完整文件字符串。所有上传流均显式关闭。Postman、HAR、Swagger、FastMock 保留各自文本解析逻辑；错误格式诊断仍按完整内容检测以保持兼容。

导入仍需保存整个对象模型并进行重复处理、场景合并和数据库写入，因此不保证任意大小文件或恒定内存。上限应结合堆大小、临时磁盘、请求并发和实际数据内容调整。对特别大的数据优先按分组分批导入。
