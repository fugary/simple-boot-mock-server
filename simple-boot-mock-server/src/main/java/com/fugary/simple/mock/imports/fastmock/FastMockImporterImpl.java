package com.fugary.simple.mock.imports.fastmock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fugary.simple.mock.entity.mock.MockBase;
import com.fugary.simple.mock.imports.MockGroupImporter;
import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.web.vo.export.ExportDataVo;
import com.fugary.simple.mock.web.vo.export.ExportGroupVo;
import com.fugary.simple.mock.web.vo.export.ExportMockVo;
import com.fugary.simple.mock.web.vo.export.ExportRequestVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FastMockImporterImpl implements MockGroupImporter {

    @Override
    public boolean isSupport(String type) {
        return "fastmock".equals(type);
    }

    @Override
    public ExportMockVo doImport(String data) {
        try {
            List<FastMockDto> fastMockList = JsonUtils.getMapper().readValue(data, new TypeReference<>() {
            });
            log.info("{}", fastMockList);
            Map<Integer, List<FastMockDto>> fastMockMap = fastMockList.stream().collect(Collectors.groupingBy(FastMockDto::getFolderId, TreeMap::new, Collectors.toList()));
            List<ExportGroupVo> mockGroups = fastMockMap.entrySet().stream().map(entry -> toMockGroup(entry.getKey(), entry.getValue())).collect(Collectors.toList());
            ExportMockVo exportMockVo = new ExportMockVo();
            exportMockVo.setGroups(mockGroups);
            return exportMockVo;
        } catch (JsonProcessingException e) {
            log.error("FastMock解析失败", e);
        }
        return null;
    }

    /**
     * 解析成MockGroup
     *
     * @param folderId
     * @param valueList
     * @return
     */
    protected ExportGroupVo toMockGroup(Integer folderId, List<FastMockDto> valueList) {
        ExportGroupVo group = new ExportGroupVo();
        valueList = valueList.stream().sorted(Comparator.comparing(FastMockDto::getId))
                .collect(Collectors.toList());
        FastMockDto firstMock = valueList.get(0);
        if (folderId == 0) {
            group.setGroupName("FastMock Default");
        } else {
            group.setGroupName(firstMock.getName());
        }
        parseBaseFields(group, firstMock);
        String folderItemsStr = JsonUtils.toJson(valueList);
        group.setGroupPath(DigestUtils.md5Hex(folderItemsStr + folderId));
        group.setDescription(firstMock.getDescription());
        group.setRequests(toMockRequests(valueList));
        return group;
    }

    /**
     * 解析Request对象
     *
     * @param valueList
     * @return
     */
    protected List<ExportRequestVo> toMockRequests(List<FastMockDto> valueList) {
        return valueList.stream().map(mockValue -> {
            ExportRequestVo requestVo = new ExportRequestVo();
            requestVo.setRequestName(mockValue.getName());
            requestVo.setRequestPath(mockValue.getUrl());
            requestVo.setMethod(StringUtils.upperCase(mockValue.getMethod()));
            requestVo.setDelay(mockValue.getDelay());
            requestVo.setDescription(mockValue.getDescription());
            parseBaseFields(requestVo, mockValue);
            requestVo.setDataList(toMockDataList(mockValue));
            return requestVo;
        }).collect(Collectors.toList());
    }

    /**
     * 解析Data数据
     *
     * @param mockValue
     * @return
     */
    protected List<ExportDataVo> toMockDataList(FastMockDto mockValue) {
        ExportDataVo dataVo = new ExportDataVo();
        parseBaseFields(dataVo, mockValue);
        dataVo.setResponseBody(mockValue.getMockRule());
        dataVo.setStatusCode(HttpStatus.OK.value());
        dataVo.setContentType(MediaType.APPLICATION_JSON_VALUE);
        dataVo.setResponseFormat("json");
        dataVo.setDescription(mockValue.getDescription());
        return List.of(dataVo);
    }

    /**
     * 基本字段复制
     *
     * @param base
     * @param mockValue
     */
    protected void parseBaseFields(MockBase base, FastMockDto mockValue) {
        base.setCreateDate(mockValue.getCreateTime());
        base.setModifyDate(mockValue.getUpdateTime());
        base.setStatus(mockValue.getOn());
        base.setCreator(mockValue.getCreateUser());
    }
}
