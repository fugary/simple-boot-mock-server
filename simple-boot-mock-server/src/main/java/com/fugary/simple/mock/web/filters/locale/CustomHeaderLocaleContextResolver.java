package com.fugary.simple.mock.web.filters.locale;

import com.fugary.simple.mock.contants.MockConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;

import java.util.List;
import java.util.Locale;

/**
 * Create date 2023/12/27<br>
 *
 * @author gary.fu
 */
@Slf4j
public class CustomHeaderLocaleContextResolver extends AcceptHeaderLocaleContextResolver {

    @Override
    @NonNull
    public LocaleContext resolveLocaleContext(ServerWebExchange exchange) {
        String language = exchange.getRequest().getHeaders().getFirst(MockConstants.MOCK_LOCALE_KEY);
        if (StringUtils.isNotBlank(language)) {
            try {
                return new SimpleLocaleContext(resolveSupportedLocale(LocaleUtils.toLocale(language)));
            } catch (Exception e) {
                log.error("locale格式错误", e);
            }
        }
        return super.resolveLocaleContext(exchange);
    }

    protected Locale resolveSupportedLocale(Locale locale) {
        List<Locale> supportedLocales = getSupportedLocales();
        if (supportedLocales.isEmpty() || supportedLocales.contains(locale)) {
            return locale;
        }
        return getDefaultLocale();
    }
}
