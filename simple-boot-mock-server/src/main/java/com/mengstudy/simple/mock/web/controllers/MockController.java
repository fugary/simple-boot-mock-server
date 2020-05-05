package com.mengstudy.simple.mock.web.controllers;

import com.mengstudy.simple.mock.contants.MockConstants;
import com.mengstudy.simple.mock.entity.mock.MockData;
import com.mengstudy.simple.mock.service.mock.MockGroupService;
import com.mengstudy.simple.mock.utils.MockJsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * Created on 2020/5/3 20:23 .<br>
 *
 * @author gary.fu
 */
@RestController
@RequestMapping(MockConstants.MOCK_PREFIX)
public class MockController {

    @Autowired
    private MockGroupService mockGroupService;

    @RequestMapping("/**")
    public ResponseEntity doMock(HttpServletRequest request) {
        MockData data = mockGroupService.matchMockData(request.getServletPath(), request.getMethod());
        if (data != null) {
            String responseBody = MockJsUtils.mock(data.getResponseBody());
            return ResponseEntity.status(data.getStatusCode())
                    .header(HttpHeaders.CONTENT_TYPE, data.getContentType())
                    .body(responseBody);
        }
        return ResponseEntity.notFound().build();
    }
}
