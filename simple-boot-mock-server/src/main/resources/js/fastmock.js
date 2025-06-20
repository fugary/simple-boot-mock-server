(function (Mock, request) {
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
    const requireCache = {};
    globalThis.require = function (url) {
        if (requireCache[url]) {
            return requireCache[url];
        }
        const response = fetch(url).then(res => res.text());
        const module = {};
        return response.then(script => {
            const moduleWrapper = new Function("module", "exports", script);
            module.exports = {};
            moduleWrapper(module, module.exports);
            requireCache[url] = module.exports;
            return module.exports;
        });
    };
    globalThis.mockStringify = (input, ...args) => {
        if (typeof input === 'string') {
            return input;
        }
        if (input instanceof Promise) {
            return input.then(result => mockStringify(result, ...args));
        }
        if (input instanceof Uint8Array) { // 字节数组处理成base64
            return btoa(input);
        }
        if (input instanceof Function) { // 如果是函数，自动调用，防止直接输出函数体
            return mockStringify(input());
        }
        return JSON.stringify(input, ...args);
    };
})(Mock, request)
