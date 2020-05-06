package com.mengstudy.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mengstudy.simple.mock.entity.mock.MockData;
import com.mengstudy.simple.mock.entity.mock.MockRequest;
import com.mengstudy.simple.mock.mapper.mock.MockRequestMapper;
import com.mengstudy.simple.mock.service.mock.MockDataService;
import com.mengstudy.simple.mock.service.mock.MockRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created on 2020/5/3 22:37 .<br>
 *
 * @author gary.fu
 */
@Service
public class MockRequestServiceImpl extends ServiceImpl<MockRequestMapper, MockRequest> implements MockRequestService {

    @Autowired
    private MockDataService mockDataService;

    @Override
    public boolean deleteMockRequest(Integer requestId) {
        mockDataService.remove(Wrappers.<MockData>query().eq("request_id", requestId));
        return removeById(requestId);
    }

    @Override
    public boolean existsMockRequest(MockRequest request) {
        List<MockRequest> existRequests = list(Wrappers.<MockRequest>query()
                .eq("group_id", request.getGroupId())
                .eq("request_path", request.getRequestPath()));
        return existRequests.stream()
                .anyMatch(existRequest -> !existRequest.getId().equals(request.getId())
                        && existRequest.getMethod().equalsIgnoreCase(request.getMethod()));
    }
}
