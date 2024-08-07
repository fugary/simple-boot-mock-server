(function (Mock) {
    if (!Mock.__oriMock) {
        Mock.__oriMock = Mock.mock
        const processMockParam = mockData => {
            if (typeof mockData === 'object') {
                for (const argsKey in mockData) {
                    const dataVal = mockData[argsKey]
                    if (typeof dataVal === 'function') {
                        mockData[argsKey] = function () {
                            const result = dataVal.call(this, {
                                _req: request, // fastMock
                                request: request,
                                Mock: Mock
                            })
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
        Mock.mock = function (mockData, ...args) {
            processMockParam(mockData)
            return Mock.__oriMock(mockData, ...args)
        };
    }
})(Mock)
