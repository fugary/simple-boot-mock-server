package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.entity.mock.MockSchema;
import com.fugary.simple.mock.mapper.mock.MockRequestMapper;
import com.fugary.simple.mock.script.ScriptEngineProvider;
import com.fugary.simple.mock.service.mock.MockSchemaService;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.service.mock.MockDataService;
import com.fugary.simple.mock.service.mock.MockRequestService;
import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.fugary.simple.mock.contants.MockConstants.DB_MODIFY_FROM_KEY;

/**
 * Created on 2020/5/3 22:37 .<br>
 *
 * @author gary.fu
 */
@Slf4j
@Service
public class MockRequestServiceImpl extends ServiceImpl<MockRequestMapper, MockRequest> implements MockRequestService {

    @Autowired
    private MockDataService mockDataService;

    @Autowired
    private MockSchemaService mockSchemaService;

    @Autowired
    private ScriptEngineProvider scriptEngineProvider;

    @Override
    public boolean deleteMockRequest(Integer requestId) {
        List<MockData> dataList = mockDataService.list(Wrappers.<MockData>query().eq("request_id", requestId));
        mockDataService.deleteMockDatas(dataList.stream().map(MockData::getId).collect(Collectors.toList()));
        mockSchemaService.remove(Wrappers.<MockSchema>query().eq("request_id", requestId));
        this.remove(Wrappers.<MockRequest>query().eq(DB_MODIFY_FROM_KEY, requestId));
        return removeById(requestId);
    }

    @Override
    public boolean deleteMockRequests(List<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }
        List<MockData> dataList = mockDataService.list(Wrappers.<MockData>query().in("request_id", ids));
        mockDataService.deleteMockDatas(dataList.stream().map(MockData::getId).collect(Collectors.toList()));
        mockSchemaService.remove(Wrappers.<MockSchema>query().in("request_id", ids));
        this.remove(Wrappers.<MockRequest>query().in(DB_MODIFY_FROM_KEY, ids));
        return removeByIds(ids);
    }

    @Override
    public boolean existsMockRequest(MockRequest request) {
        List<MockRequest> existRequests = list(Wrappers.<MockRequest>query()
                .eq("group_id", request.getGroupId())
                .eq("request_path", request.getRequestPath())
                .eq("match_pattern", request.getMatchPattern()));
        return existRequests.stream()
                .anyMatch(existRequest -> !existRequest.getId().equals(request.getId())
                        && existRequest.getMethod().equalsIgnoreCase(request.getMethod()));
    }

    @Override
    public boolean copyMockRequest(Integer requestId, Integer newGroupId) {
        MockRequest mockRequest = getById(requestId);
        if (mockRequest != null) {
            mockRequest.setId(null);
            if (newGroupId != null) {
                mockRequest.setGroupId(newGroupId);
            }
            saveOrUpdate(mockRequest);
            List<MockData> dataList = mockDataService.list(Wrappers.<MockData>query()
                    .eq("request_id", requestId).isNull(DB_MODIFY_FROM_KEY));
            List<MockSchema> schemaList = mockSchemaService.list(Wrappers.<MockSchema>query().eq("request_id", requestId));
            Map<String, List<MockSchema>> schemaMap = schemaList.stream().collect(Collectors
                    .groupingBy(schema -> StringUtils.join(schema.getRequestId(), "-", schema.getDataId())));
            mockSchemaService.saveCopySchemas(schemaMap, requestId, null, mockRequest.getGroupId(), mockRequest.getId(), null);
            dataList.forEach(data -> {
                Integer oldDataId = data.getId();
                data.setId(null);
                data.setGroupId(mockRequest.getGroupId());
                data.setRequestId(mockRequest.getId());
                boolean saved = mockDataService.saveOrUpdate(data);
                if (saved) {
                    mockSchemaService.saveCopySchemas(schemaMap, requestId, oldDataId, mockRequest.getGroupId(), mockRequest.getId(), data.getId());
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public MockData findMockData(Integer requestId, Integer defaultId) {
        List<MockData> mockDataList = mockDataService.list(Wrappers.<MockData>query()
                .eq("request_id", requestId)
                .eq("status", 1)
                .isNull(DB_MODIFY_FROM_KEY));
        MockData mockData = findForceMockData(mockDataList, defaultId);
        if (mockData != null) {
            return mockData;
        }
        return findMockData(mockDataList);
    }

    @Override
    public List<MockData> loadDataByRequest(Integer requestId) {
        return mockDataService.list(Wrappers.<MockData>query()
                .eq("request_id", requestId)
                .eq("status", 1)
                .isNull(DB_MODIFY_FROM_KEY));
    }

    @Override
    public List<MockData> loadAllDataByRequest(Integer requestId) {
        return mockDataService.list(Wrappers.<MockData>query()
                .eq("request_id", requestId)
                .isNull(DB_MODIFY_FROM_KEY));
    }

    /**
     * 查询默认可用MockData
     *
     * @param mockDataList
     * @return
     */
    public MockData findMockData(List<MockData> mockDataList) {
        MockData result = null;
        // 默认数据从没有matchPattern的数据中查找，默认和匹配规则有冲突
        mockDataList = mockDataList.stream().filter(md -> StringUtils.isBlank(md.getMatchPattern())).collect(Collectors.toList());
        for (MockData mockData : mockDataList) {
            if (result == null || (!SimpleMockUtils.isDefault(result) && SimpleMockUtils.isDefault(mockData))) {
                result = mockData;
            }
        }
        return result;
    }

    @Override
    public MockData findForceMockData(List<MockData> mockDataList, Integer defaultId) {
        for (MockData mockData : mockDataList) {
            if (defaultId != null && defaultId.equals(mockData.getId())) { // 强制指定
                return mockData;
            }
        }
        return null;
    }

    @Override
    public MockData findMockDataByRequest(List<MockData> mockDataList, HttpRequestVo requestVo) {
        for (MockData mockData : mockDataList) {
            if (StringUtils.isNotBlank(mockData.getMatchPattern())) {
                if (matchRequestPattern(mockData.getMatchPattern())) {
                    return mockData;
                }
            }
        }
        return null;
    }

    @Override
    public boolean saveMockParams(MockData mockData) {
        boolean saveToRequest = mockData.getId() == null; // 没有mockData，保存到request
        if (mockData.getId() != null) {
            MockData savedMockData = mockDataService.getById(mockData.getId());
            if (savedMockData != null) {
                savedMockData.setMockParams(mockData.getMockParams());
                SimpleMockUtils.addAuditInfo(savedMockData);
                mockDataService.updateById(savedMockData);
            }
        }
        if (saveToRequest && mockData.getRequestId() != null) {
            MockRequest savedMockRequest = getById(mockData.getRequestId());
            if (savedMockRequest != null) {
                savedMockRequest.setMockParams(mockData.getMockParams());
                SimpleMockUtils.addAuditInfo(savedMockRequest);
                updateById(savedMockRequest);
            }
        }
        return true;
    }

    @Override
    public boolean matchRequestPattern(String matchPattern) {
        if (StringUtils.isNotBlank(matchPattern)) {
            Object result = scriptEngineProvider.eval("Boolean(" + matchPattern + ")"); // 转Boolean值
            log.info("计算匹配：{}={}", matchPattern, result);
            return Boolean.TRUE.equals(result); // 必须等于true才执行
        }
        return true;
    }

    @Override
    public boolean saveOrUpdate(MockRequest entity) {
        if (entity.getId() == null || entity.getVersion() == null) {
            entity.setVersion(1);
        }
        boolean needSave = true;
        if (entity.getId() != null) {
            MockRequest history = getById(entity.getId());
            if (history != null) {
                needSave = !SimpleMockUtils.isSameMockData(history, entity);
                if (needSave) {
                    history.setId(null);
                    history.setModifyFrom(entity.getId());
                    history.setVersion(entity.getVersion());
                    if (StringUtils.isBlank(history.getModifier())) {
                        history.setModifier(history.getCreator());
                    }
                    if (history.getModifyDate() == null) {
                        history.setModifyDate(history.getCreateDate());
                    }
                    this.save(history);
                }
            }
        }
        boolean result = true;
        if (needSave) {
            result = super.saveOrUpdate(entity);
        }
        return result;
    }
}
