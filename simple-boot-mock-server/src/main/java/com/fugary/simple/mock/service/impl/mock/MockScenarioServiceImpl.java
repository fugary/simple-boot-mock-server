package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fugary.simple.mock.entity.mock.MockScenario;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.entity.mock.MockSchema;
import com.fugary.simple.mock.mapper.mock.MockScenarioMapper;
import com.fugary.simple.mock.service.mock.MockDataService;
import com.fugary.simple.mock.service.mock.MockRequestService;
import com.fugary.simple.mock.service.mock.MockScenarioService;
import com.fugary.simple.mock.service.mock.MockSchemaService;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.fugary.simple.mock.contants.MockConstants.DB_MODIFY_FROM_KEY;

@Service
public class MockScenarioServiceImpl extends ServiceImpl<MockScenarioMapper, MockScenario>
        implements MockScenarioService {

    @Autowired
    private MockRequestService mockRequestService;

    @Autowired
    private MockDataService mockDataService;

    @Autowired
    private MockSchemaService mockSchemaService;

    @Override
    public boolean existsMockScenario(MockScenario scenario) {
        List<MockScenario> exists = list(Wrappers.<MockScenario>query()
                .eq("group_id", scenario.getGroupId())
                .eq("scenario_name", scenario.getScenarioName()));
        return exists.stream().anyMatch(item -> !item.getId().equals(scenario.getId()));
    }

    @Override
    public void copyScenarioRequests(Integer groupId, String fromScenarioCode, String toScenarioCode) {
        List<MockRequest> requests = mockRequestService.list(Wrappers.<MockRequest>query()
                .eq("group_id", groupId)
                .eq(StringUtils.isNotBlank(fromScenarioCode), "scenario_code", fromScenarioCode)
                .isNull(StringUtils.isBlank(fromScenarioCode), "scenario_code")
                .isNull(DB_MODIFY_FROM_KEY));

        for (MockRequest mockRequest : requests) {
            Integer oldRequestId = mockRequest.getId();
            SimpleMockUtils.prepareForCreate(mockRequest);
            mockRequest.setScenarioCode(StringUtils.trimToNull(toScenarioCode));
            mockRequestService.saveOrUpdate(SimpleMockUtils.addAuditInfo(mockRequest));

            List<MockData> dataList = mockDataService.list(Wrappers.<MockData>query()
                    .eq("request_id", oldRequestId).isNull(DB_MODIFY_FROM_KEY));
            List<MockSchema> schemaList = mockSchemaService
                    .list(Wrappers.<MockSchema>query().eq("request_id", oldRequestId));
            Map<String, List<MockSchema>> schemaMap = schemaList.stream().collect(Collectors
                    .groupingBy(schema -> StringUtils.join(schema.getRequestId(), "-", schema.getDataId())));

            mockSchemaService.saveCopySchemas(schemaMap, oldRequestId, null, mockRequest.getGroupId(),
                    mockRequest.getId(), null);

            dataList.forEach(data -> {
                Integer oldDataId = data.getId();
                boolean saved = mockDataService.copyMockData(oldDataId, mockRequest);
                if (saved) {
                    mockSchemaService.saveCopySchemas(schemaMap, oldRequestId, oldDataId, mockRequest.getGroupId(),
                            mockRequest.getId(), data.getId());
                }
            });
        }
    }
}
