package com.fugary.simple.mock.service.mock;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fugary.simple.mock.entity.mock.*;
import com.fugary.simple.mock.utils.JsonUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Supplier;

import static com.fugary.simple.mock.contants.MockConstants.DB_MODIFY_FROM_KEY;

/** Writes the existing export format without retaining all response bodies or a JSON byte array. */
@Service
public class MockGroupExportService {
    private final MockRequestService requests;
    private final MockDataService data;
    private final MockSchemaService schemas;
    private final MockScenarioService scenarios;

    public MockGroupExportService(MockRequestService requests, MockDataService data,
                                  MockSchemaService schemas, MockScenarioService scenarios) {
        this.requests = requests;
        this.data = data;
        this.schemas = schemas;
        this.scenarios = scenarios;
    }

    // Avoid a transaction-bound MyBatis first-level cache retaining previously exported rows.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void writeGroups(List<MockGroup> groups, OutputStream output) throws IOException {
        try (JsonGenerator json = JsonUtils.getMapper().getFactory().createGenerator(output)) {
            // The servlet owns the response stream. Do not finish a partial document on failure.
            json.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
            json.disable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT);
            json.writeStartObject();
            json.writeArrayFieldStart("groups");
            for (MockGroup group : groups) {
                json.writeStartObject();
                writeFields(json, group);
                json.writeArrayFieldStart("requests");
                each(requests, () -> new QueryWrapper<MockRequest>().eq("group_id", group.getId())
                        .isNull(DB_MODIFY_FROM_KEY), request -> writeRequest(json, request));
                json.writeEndArray();
                json.writeArrayFieldStart("scenarios");
                each(scenarios, () -> new QueryWrapper<MockScenario>().eq("group_id", group.getId()),
                        json::writeObject);
                json.writeEndArray();
                writeSchemas(json, group.getId(), null, null);
                json.writeEndObject();
            }
            json.writeEndArray();
            json.writeEndObject();
        }
    }

    private void writeRequest(JsonGenerator json, MockRequest request) throws IOException {
        json.writeStartObject();
        writeFields(json, request);
        json.writeArrayFieldStart("dataList");
        each(data, () -> new QueryWrapper<MockData>().eq("group_id", request.getGroupId())
                .eq("request_id", request.getId()).isNull(DB_MODIFY_FROM_KEY), item -> {
            json.writeStartObject();
            writeFields(json, item);
            writeSchemas(json, request.getGroupId(), request.getId(), item.getId());
            json.writeEndObject();
        });
        json.writeEndArray();
        writeSchemas(json, request.getGroupId(), request.getId(), null);
        json.writeEndObject();
    }

    private void writeSchemas(JsonGenerator json, Integer groupId, Integer requestId, Integer dataId)
            throws IOException {
        json.writeArrayFieldStart("schemas");
        each(schemas, () -> {
            QueryWrapper<MockSchema> query = new QueryWrapper<MockSchema>().eq("group_id", groupId);
            if (requestId == null) {
                query.isNull("request_id");
            } else {
                query.eq("request_id", requestId);
            }
            if (dataId == null) {
                query.isNull("data_id");
            } else {
                query.eq("data_id", dataId);
            }
            return query;
        }, json::writeObject);
        json.writeEndArray();
    }

    private void writeFields(JsonGenerator json, Object value) throws IOException {
        SerializerProvider provider = JsonUtils.getMapper().getSerializerProviderInstance();
        provider.findValueSerializer(value.getClass()).unwrappingSerializer(NameTransformer.NOP)
                .serialize(value, json, provider);
    }

    /** Keyset pagination bounds even a single request with many large responses to one row at a time. */
    private <T extends MockBase> void each(IService<T> service, Supplier<QueryWrapper<T>> query,
                                          RowWriter<T> writer) throws IOException {
        Integer lastId = null;
        while (true) {
            QueryWrapper<T> wrapper = query.get().orderByAsc("id");
            if (lastId != null) {
                wrapper.gt("id", lastId);
            }
            List<T> rows = service.page(new Page<T>(1, 1, false), wrapper).getRecords();
            if (rows.isEmpty()) {
                return;
            }
            T row = rows.get(0);
            writer.write(row);
            lastId = row.getId();
            row = null;
            rows = null;
        }
    }

    @FunctionalInterface
    private interface RowWriter<T> {
        void write(T row) throws IOException;
    }
}
