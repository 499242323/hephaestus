package olympus.hephaestus.org.controller;

import olympus.hephaestus.org.dto.OrgScopeResponse;
import olympus.hephaestus.org.service.OrgAvatarService;
import olympus.hephaestus.org.service.OrgPersonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrgPersonController.class)
@Import(OrgExceptionHandler.class)
class OrgPersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrgPersonService orgPersonService;

    @MockBean
    private OrgAvatarService orgAvatarService;

    @Test
    void shouldReturnCurrentScope() throws Exception {
        when(orgPersonService.getCurrentScope(100L)).thenReturn(new OrgScopeResponse(null, List.of()));

        mockMvc.perform(get("/api/org/persons/current-scope").header("X-Person-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.units").isArray());
    }
}
