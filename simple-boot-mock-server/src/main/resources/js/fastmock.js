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
    const requireCache = globalThis.__requireCache__ || {};
    /**
     * require 支持，加载第三方库（CommonJS风格）
     * 不支持 ESM 模块
     * @param {string} url 第三方库的URL
     * @returns {Promise} Promise，resolve 到模块导出的对象
     */
    globalThis.require = function (url) {
        if (requireCache[url]) {
            return Promise.resolve(requireCache[url]);
        }
        return fetch(url).then(res => {
            if (!res.ok) {
                throw new Error("Failed to load module: " + url + " (" + res.status + ")");
            }
            return res.text();
        }).then(script => {
            const module = {exports: {}};
            try {
                const moduleWrapper = new Function("module", "exports", script);
                moduleWrapper(module, module.exports);
            } catch (e) {
                throw new Error("Error evaluating module: " + url + " → " + e.message);
            }
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
