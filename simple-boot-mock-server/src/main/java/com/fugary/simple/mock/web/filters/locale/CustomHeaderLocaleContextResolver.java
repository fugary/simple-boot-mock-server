package com.fugary.simple.mock.web.filters.locale;

import com.fugary.simple.mock.contants.MockConstants;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * Create date 2023/12/27<br>
 *
 * @author gary.fu
 */
@Setter
@Getter
@Slf4j
public class CustomHeaderLocaleContextResolver extends AcceptHeaderLocaleResolver {

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String language = request.getHeader(MockConstants.MOCK_LOCALE_KEY);
        if (StringUtils.isNotBlank(language)) {
            try {
                language = language.replace("-", "_");
                return LocaleUtils.toLocale(language);
            } catch (Exception e) {
                log.error("locale格式错误", e);
            }
        }
        return super.resolveLocale(request);
    }

}
