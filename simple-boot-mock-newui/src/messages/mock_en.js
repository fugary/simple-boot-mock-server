import { baseMessages } from '@/messages/base'
export const mock = baseMessages()

mock.label.method = 'Method'
mock.label.requestBody = 'Request Body'
mock.label.responseBody = 'Response Body'
mock.label.mockResponseBody = 'Mock Response'
mock.label.requestBody1 = 'Request'
mock.label.responseBody1 = 'Response'
mock.label.requestPath = 'Request Path'
mock.label.requestName = 'Request Name'
mock.label.pathParams = 'Path Params'
mock.label.queryParams = 'Query Params'
mock.label.requestHeaders = 'Request Headers'
mock.label.responseHeaders = 'Response Headers'
mock.label.authorization = 'Authorization'
mock.label.default = 'Default'
mock.label.statusCode = 'Status Code'
mock.label.matchPattern = 'Match Pattern'
mock.label.mockGroups = 'Mock Groups'
mock.label.mockRequests = 'Mock Requests'
mock.label.mockData = 'Mock Data'
mock.label.mockProjects = 'Mock Projects'
mock.label.project = 'Project'
mock.label.defaultProject = 'Default Project'
mock.label.groupName = 'Group Name'
mock.label.pathId = 'Path ID'
mock.label.proxyUrl = 'Proxy URL'
mock.label.export = 'Export'
mock.label.exportAll = 'Export All'
mock.label.exportSelected = 'Export Selected'
mock.label.import = 'Import'
mock.label.linkAddress = 'Link Address'
mock.label.source = 'Source'
mock.label.importFile = 'Import file'
mock.label.duplicateStrategy = 'Duplicate Path'
mock.label.selectFile = 'Select file'
mock.label.importDuplicateStrategyAbort = 'Abort Import'
mock.label.importDuplicateStrategySkip = 'Skip Duplicate Paths'
mock.label.importDuplicateStrategyGenerate = 'Automatically Generate New Paths'
mock.label.importTypeSimple = 'Current Simple Mock Service'
mock.label.importTypeFastMock = 'Old FastMock Service (Test)'
mock.label.importTypeSwagger = 'Swagger2/OpenAPI3.0 (Test)'
mock.label.sendRequest = 'Send Request'
mock.label.authType = 'Auth Type'
mock.label.authTypeNone = 'No Auth'
mock.label.authTypeBasic = 'Basic Auth'
mock.label.authTypeToken = 'Token Auth'
mock.label.authTypeJWT = 'JWT Auth'
mock.label.authParamName = 'Param Name'
mock.label.authPrefix = 'Prefix'
mock.label.setDefault = 'Set default'
mock.label.dataFormat = 'Data Format'
mock.label.projectCode = 'Project Code'
mock.label.projectName = 'Project Name'

mock.msg.noExportData = 'No data to export'
mock.msg.exportConfirm = 'Confirm to export mock data?'
mock.msg.proxyUrlTooltip = 'Requests outside the configured address will be sent to the proxy address to fetch data. Supports http and https'
mock.msg.proxyUrlMsg = 'Proxy address must be a valid http or https address'
mock.msg.pathIdMsg = 'It is recommended not to fill in, it will be generated automatically'
mock.msg.duplicateStrategy = 'The path is globally unique, and all users share it, so it is usually automatically generated by uuid'
mock.msg.importFileTitle = 'Import mock data'
mock.msg.importFileLimit = 'File size limit is 5MB'
mock.msg.importFileSuccess = 'Imported successfully, {0} group(s).'
mock.msg.importFileNoFile = 'Please select import file'
mock.msg.showRawData = 'Show raw data without formatting'
mock.msg.saveMockResponse = 'Save response data, [Send Request] test will automatically save'
mock.msg.pasteToProcess = 'Supports url query string or JSON'
mock.msg.authParamNameTooltip = 'Default value is: {0}, cannot be empty'
mock.msg.authPrefixTooltip = 'Default value is: {0}, can be empty'
mock.msg.requestTest = 'Test Request'
mock.msg.matchPatternTest = 'Test Match Pattern'
mock.msg.requestNameTooltip = 'Simple interface name, not required.'
mock.msg.requestIntro = `request.body — The body content object (only JSON)<br>
                        request.bodyStr — The body content string<br>
                        request.headers — The headers object<br>
                        request.parameters — The request parameters object<br>
                        request.pathParameters — The path parameters object<br>
                        request.params — The merged request parameters and path parameters`
mock.msg.matchPatternTooltip = `Matching rules support JavaScript expressions. You can use the following to request data:<br>${mock.msg.requestIntro}`
mock.msg.projectCodeTooltip = 'Only letters, numbers, and underscores are allowed, and it is unique.'
mock.msg.responseBodyTooltip = `Response content supports replacing by request parameters. Use {0} format to replace data:<br>${mock.msg.requestIntro}`