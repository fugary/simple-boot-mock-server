package com.fugary.simple.mock.web.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fugary.simple.mock.web.vo.NameValue;
import com.fugary.simple.mock.contants.MockConstants;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Created on 2020/5/7 20:12 .<br>
 *
 * @author gary.fu
 */
public class MockMetaDataFilter extends OncePerRequestFilter {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        boolean isPreview = BooleanUtils.toBoolean(StringUtils
                .defaultIfBlank(request.getHeader(MockConstants.MOCK_DATA_PREVIEW_HEADER), Boolean.FALSE.toString()));
        if (isPreview) {
            Enumeration<String> reqHeaders = request.getHeaderNames();
            ArrayList<NameValue> requestHeaders = new ArrayList<>();
            while (reqHeaders.hasMoreElements()) {
                String key = reqHeaders.nextElement();
                requestHeaders.add(new NameValue(key, request.getHeader(key)));
            }
            response.setHeader(MockConstants.MOCK_META_DATA_REQ, objectMapper.writeValueAsString(requestHeaders));
        }
        filterChain.doFilter(request, response);
    }

}
