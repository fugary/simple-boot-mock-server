package com.fugary.simple.mock.web.filters;

import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created on 2020/5/7 20:12 .<br>
 *
 * @author gary.fu
 */
public class MockMetaDataFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (SimpleMockUtils.isMockPreview(request)) {
            response.setHeader(MockConstants.MOCK_DIAGNOSE_ID_HEADER, SimpleMockUtils.getOrCreateMockDiagnoseId(request));
        }
        filterChain.doFilter(new ContentCachingRequestWrapper(request), response);
    }

}
