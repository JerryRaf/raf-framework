package io.github.jerryraf.examples.dubbo.consumer;

import io.github.jerryraf.examples.dubbo.api.UserFacade;
import io.github.jerryraf.examples.dubbo.api.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for OrderController with mocked Dubbo reference.
 *
 * @author RAF Framework Team
 * @since 2026-04-28
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserFacade userFacade;

    @Test
    void checkUser_shouldReturn200WhenUserExists() throws Exception {
        UserDTO user = new UserDTO();
        user.setId(1L);
        user.setUsername("test_user");
        user.setStatus(1);
        when(userFacade.getById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/orders/check-user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("test_user"));
    }

    @Test
    void checkUser_shouldReturn200WithNullWhenUserNotFound() throws Exception {
        when(userFacade.getById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/orders/check-user/999"))
                .andExpect(status().isOk());
    }
}
