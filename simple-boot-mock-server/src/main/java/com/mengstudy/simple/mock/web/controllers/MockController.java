package com.mengstudy.simple.mock.web.controllers;

import com.mengstudy.simple.mock.contants.MockConstants;
import com.mengstudy.simple.mock.entity.mock.MockData;
import com.mengstudy.simple.mock.service.mock.MockGroupService;
import com.mengstudy.simple.mock.utils.SimpleMockUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.mengstudy.simple.mock.utils.SimpleMockUtils.calcHeaders;

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
    public ResponseEntity doMock(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String dataId = request.getHeader(MockConstants.MOCK_DATA_ID_HEADER);
        MockData data = mockGroupService.matchMockData(request, NumberUtils.toInt(dataId));
        if (data != null) {
            HttpHeaders httpHeaders = calcHeaders(data.getHeaders());
            if(HttpStatus.MOVED_TEMPORARILY.value() == data.getStatusCode()){
                if(SimpleMockUtils.isMockPreview(request)){
                    return ResponseEntity.ok("重定向请设为默认响应后复制URL到浏览器访问");
                }
                return ResponseEntity.status(data.getStatusCode()).headers(httpHeaders).header(HttpHeaders.LOCATION, data.getResponseBody()).body(null);
            }
            return ResponseEntity.status(data.getStatusCode())
                    .headers(httpHeaders)
                    .header(HttpHeaders.CONTENT_TYPE, data.getContentType())
                    .body(data.getResponseBody());
        }
        return ResponseEntity.notFound().build();
    }
}
