(function (Mock, request, response) {
    const processMockParam = mockData => {
        if (typeof mockData === 'object') {
            const argObj = {
                _req: request, // fastMock
                request: request,
                Mock: Mock
            }
            for (const argsKey in mockData) {
                const dataVal = mockData[argsKey]
                if (typeof dataVal === 'function') {
                    mockData[argsKey] = function () {
                        const result = dataVal.call(this, argObj)
                        if (typeof result === 'object') {
                            processMockParam(result);
                        }
                        return result
                    }
                } else if (typeof dataVal === 'object') {
                    processMockParam(dataVal)
                }
            }
        }
    }
    if (!Mock.__oriMock) {
        Mock.__oriMock = Mock.mock
    }
    Mock.mock = function (mockData, ...args) {
        processMockParam(mockData)
        return Mock.__oriMock(mockData, ...args)
    };
    globalThis.Random = Mock.Random;
    try {
        if (request && request.bodyJson) {
            request.body = JSON.parse(request.bodyJson)
        }
        if (response && response.bodyJson) {
            response.body = JSON.parse(response.bodyJson)
        }
    } catch (e) {
        console.error('Parse body error', e)
    }
})(Mock, globalThis.request, globalThis.response)
