package com.fugary.simple.mock.service.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AiCallTraceTest {
    @Test
    void boundsRawResponseAndIgnoresClientSuppliedTrace() throws Exception {
        AiCallTrace trace = new AiCallTrace();
        trace.append("中".repeat(64001));
        trace.append("ignored");
        var raw = new ObjectMapper().readTree(trace.rawResponse());
        assertTrue(raw.path("truncated").asBoolean());
        assertEquals(64000, raw.path("content").asText().length());
        AiChatRequest request = new ObjectMapper().readValue("{\"prompt\":\"hello\",\"trace\":null}", AiChatRequest.class);
        assertNotNull(request.getTrace());
        assertNull(request.getTrace().rawResponse());
    }
}
