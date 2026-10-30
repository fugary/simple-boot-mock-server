package com.fugary.simple.mock.service.ai;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fugary.simple.mock.web.controllers.admin.AiController;
import com.fugary.simple.mock.web.vo.query.AiLogQueryVo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiLogHttpTest {
    @Test
    void bindsLogFiltersAndUsesLiteralOptionsRoute() throws Exception {
        AiConfigService configs = mock(AiConfigService.class);
        AiLogService logs = mock(AiLogService.class);
        when(logs.search(any())).thenReturn(new Page<>());
        when(configs.options()).thenReturn(java.util.Collections.emptyList());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AiController(configs, logs, mock(AiService.class))).build();
        mvc.perform(get("/admin/ai/logs").param("configId", "12").param("model", "model")
            .param("status", "0").param("startDate", "2026-10-30 12:00:00").param("endDate", "2026-10-30 12:00:01"))
            .andExpect(status().isOk());
        ArgumentCaptor<AiLogQueryVo> query = ArgumentCaptor.forClass(AiLogQueryVo.class);
        verify(logs).search(query.capture());
        assertEquals(12, query.getValue().getConfigId());
        assertEquals(0, query.getValue().getStatus());
        assertEquals(1000, query.getValue().getEndDate().getTime() - query.getValue().getStartDate().getTime());
        mvc.perform(get("/admin/ai/configs/options")).andExpect(status().isOk());
        verify(configs).options();
        verify(configs, never()).getOwned(any());
    }
}
