package com.fugary.simple.mock.service.ai;

import com.fugary.simple.mock.web.controllers.admin.AiHelpController;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AiHelpHttpTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsAvailabilityAsDataInSuccessfulResponse(boolean available) throws Exception {
        AiHelpService help = mock(AiHelpService.class);
        when(help.available()).thenReturn(available);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AiHelpController(help)).build();

        mvc.perform(get("/admin/ai/help").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.resultData").value(available));
    }
}
