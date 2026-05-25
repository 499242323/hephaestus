package com.example.springaidemo.org.controller;

import com.example.springaidemo.org.service.OrgUnitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrgUnitController.class)
@Import(OrgExceptionHandler.class)
class OrgUnitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrgUnitService orgUnitService;

    @Test
    void shouldRejectUnitTreeRequestWithoutPersonHeader() throws Exception {
        mockMvc.perform(get("/api/org/units/tree"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("缺少请求头: X-Person-Id"));
    }
}
