;(function (globalThis) {
    /**
     * require 支持，加载第三方库（CommonJS风格）
     * 不支持 ESM 模块
     * @param {string} url 第三方库的URL
     * @returns {Promise} Promise，resolve 到模块导出的对象
     */
    globalThis.require = function (url) {
        const requireCache = globalThis.__requireCache__ || {};
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

    /**
     * 字符串转换输出
     *
     * @param input
     * @param args
     * @returns {string|*|string|Promise<unknown>}
     */
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
}(globalThis));
