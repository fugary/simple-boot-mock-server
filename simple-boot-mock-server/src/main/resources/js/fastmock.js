;(function (globalThis) {
    if (globalThis.Mock && globalThis.Mock.Handler) {
        globalThis.Mock.Handler.function = function (options) {
            const req = typeof request !== 'undefined' ? request : (globalThis.request || {});
            const argObj = {
                _req: req, // fastMock
                request: req,
                Mock: globalThis.Mock
            };
            const context = options && options.context && options.context.currentContext ? options.context.currentContext : this;
            return options.template.call(context, argObj);
        };
        globalThis.Random = globalThis.Mock.Random;
    }
}(globalThis));

