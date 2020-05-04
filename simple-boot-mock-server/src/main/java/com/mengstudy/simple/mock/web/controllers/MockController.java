package com.mengstudy.simple.mock.web.controllers;

import com.mengstudy.simple.mock.contants.MockConstants;
import com.mengstudy.simple.mock.entity.mock.MockGroup;
import com.mengstudy.simple.mock.service.mock.MockGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

    private PathMatcher pathMatcher = new AntPathMatcher();

    @RequestMapping("/**")
    public ResponseEntity doMock(HttpServletRequest request) {
        List<MockGroup> mockGroups = mockGroupService.list();
        for (MockGroup mockGroup : mockGroups) {
            if (pathMatcher.match(MockConstants.MOCK_PREFIX + mockGroup.getGroupPath() + MockConstants.ALL_PATH_PATTERN, request.getServletPath())) {
                return ResponseEntity.ok(mockGroup);
            }
        }
        return ResponseEntity.notFound().build();
    }
}
