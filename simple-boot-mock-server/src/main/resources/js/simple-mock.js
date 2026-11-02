;(function (globalThis) {
    const scriptLineMarker = '__simpleMockScriptLine__:';
    const getUserScriptLineNumber = function (stack) {
        const evalPrefix = '<eval>:';
        const frame = String(stack || '').split('\n').slice(2).find(line => line.includes(evalPrefix));
        return frame ? frame.split(evalPrefix)[1].split(':')[0] : '';
    };
    const markScriptLine = function (error, lineNumber) {
        const message = error == null ? 'Unknown fetch error'
            : (typeof error === 'string' ? error : (error.message || String(error)));
        if (!lineNumber || String(message).includes(scriptLineMarker)) {
            return message;
        }
        return message + ' ' + scriptLineMarker + lineNumber;
    };

    const nativeFetch = globalThis.fetch;
    if (typeof nativeFetch === 'function') {
        globalThis.fetch = function (...args) {
            const lineNumber = getUserScriptLineNumber(new Error().stack);
            let fetchPromise;
            try {
                fetchPromise = nativeFetch(...args);
            } catch (e) {
                throw markScriptLine(e, lineNumber);
            }
            return Promise.resolve(fetchPromise).catch(e => {
                throw markScriptLine(e, lineNumber);
            }).then(response => new Proxy(response, {
                get(target, key) {
                    if (key === 'json') {
                        return function () {
                            const lineNumber = getUserScriptLineNumber(new Error().stack);
                            try {
                                return target.json();
                            } catch (e) {
                                throw markScriptLine('Invalid JSON in response', lineNumber);
                            }
                        };
                    }
                    if (key === 'text' || key === 'arrayBuffer' || key === 'blob') {
                        return function (...methodArgs) {
                            return target[key](...methodArgs);
                        };
                    }
                    return target[key];
                }
            }));
        };
    }

    const isBareModuleSpecifier = function (url) {
        return typeof url !== 'string' || !/^(?:[a-z][a-z\d+\-.]*:|\/\/|\/|\.{1,2}\/)/i.test(url);
    };
    /**
     * require 支持，加载第三方库（CommonJS风格）
     * 不支持 ESM 模块
     * @param {string} url 第三方库的URL
     * @param {{cache: boolean}} [options] 选项
     * @returns {Promise} Promise，resolve 到模块导出的对象
     */
    globalThis.require = function (url, options) {
        if (isBareModuleSpecifier(url)) {
            throw new Error("Unsupported require module specifier: " + url);
        }
        const { cache, ...fetchOptions } = options || {};
        const sourceCache = globalThis.__requireSourceCache__;
        const cacheEnabled = cache !== false && !!sourceCache;
        const requireCache = globalThis.__requireCache__ || {};
        const generation = cacheEnabled ? sourceCache.generation() : null;
        const source = cacheEnabled ? sourceCache.get(url) : null;
        const cachedModule = requireCache[url];
        if (source && cachedModule && cachedModule.version === source.version) {
            return Promise.resolve(cachedModule.exports);
        }
        if (cacheEnabled) {
            delete requireCache[url];
        }
        const scriptPromise = source ? Promise.resolve(source.script) : fetch(url, fetchOptions).then(res => {
            if (!res.ok) {
                throw new Error("Failed to load module: " + url + " (" + res.status + ")");
            }
            return res.text();
        });
        return scriptPromise.then(script => {
            const entry = cacheEnabled ? (source || sourceCache.getOrPut(url, script, generation)) : null;
            // 等待 fetch 期间可能已有同 URL 的加载完成，必须使用它发布的源码及版本。
            const currentModule = requireCache[url];
            if (entry && currentModule && currentModule.version === entry.version) {
                return currentModule.exports;
            }
            const module = {exports: {}};
            try {
                const moduleWrapper = new Function("module", "exports", entry ? entry.script : script);
                moduleWrapper(module, module.exports);
            } catch (e) {
                if (entry) {
                    sourceCache.invalidate(url, entry.version);
                }
                throw new Error("Error evaluating module: " + url + " → " + e.message);
            }
            if (entry) {
                requireCache[url] = {version: entry.version, exports: module.exports};
            }
            return module.exports;
        });
    };

    /**
     * 清空共享源码缓存；其他引擎的模块在下次 require 时失效。
     */
    globalThis.clearRequireCache = function () {
        if (globalThis.__requireSourceCache__) {
            globalThis.__requireSourceCache__.clear();
        }
        const requireCache = globalThis.__requireCache__;
        if (requireCache) {
            Object.keys(requireCache).forEach(key => delete requireCache[key]);
        }
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
        if (typeof input?.then === 'function') {
            return input.then(result => mockStringify(result, ...args));
        }
        if (input instanceof Uint8Array) { // 字节数组处理成base64
            return btoa(input);
        }
        if (input instanceof Function) { // 如果是函数，自动调用，防止直接输出函数体
            return mockStringify(input());
        }
        if (typeof input === 'object') {
            if (typeof globalThis.isProxyValue === 'function' && globalThis.isProxyValue(input)) {
                return JSON.stringify(input, ...args);
            }
            if (typeof globalThis.java2Json === 'function') {
                return globalThis.java2Json(input);
            }
        }
        return JSON.stringify(input, ...args);
    };
}(globalThis));
