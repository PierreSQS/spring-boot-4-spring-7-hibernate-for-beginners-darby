// java
package com.luv2code.springboot.cruddemo.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.luv2code.springboot.cruddemo.entity.Employee;
import com.luv2code.springboot.cruddemo.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeRestController.class)
class EmployeeRestControllerTest {

    @TestConfiguration
    static class Config {
        @Bean
        public JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper; // real mapper injected

    @MockBean
    private EmployeeService employeeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void findAll_returnsList() throws Exception {
        Employee e1 = Employee.builder()
                .id(1)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .build();

        Employee e2 = Employee.builder()
                .id(2)
                .firstName("Bob")
                .lastName("Jones")
                .email("bob@example.com")
                .build();

        when(employeeService.findAll()).thenReturn(Arrays.asList(e1, e2));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].firstName", is("Alice")))
                .andExpect(jsonPath("$[1].firstName", is("Bob")));
    }

    @Test
    void getEmployee_notFound_returnsServerError() throws Exception {
        when(employeeService.findById(99)).thenReturn(null);

        mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void addEmployee_withIdPassed_controllerResetsIdToZero_beforeSave() throws Exception {
        Employee incoming = Employee.builder()
                .id(123)
                .firstName("Charlie")
                .lastName("Brown")
                .email("charlie@example.com")
                .build();

        when(employeeService.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee arg = invocation.getArgument(0);
            arg.setId(10); // simulate DB assigned id
            return arg;
        });

        String json = objectMapper.writeValueAsString(incoming);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.firstName", is("Charlie")));

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeService).save(captor.capture());
        assert captor.getValue().getId() == 0;
    }

    @Test
    void updateEmployee_put_success() throws Exception {
        Employee updated = Employee.builder()
                .id(5)
                .firstName("Dana")
                .lastName("White")
                .email("dana@example.com")
                .build();

        when(employeeService.save(any(Employee.class))).thenReturn(updated);

        String json = objectMapper.writeValueAsString(updated);

        mockMvc.perform(put("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(5)))
                .andExpect(jsonPath("$.firstName", is("Dana")));
    }

    @Test
    void patchEmployee_withIdInPayload_isRejected() throws Exception {
        Employee existing = Employee.builder()
                .id(7)
                .firstName("Eve")
                .lastName("Adams")
                .email("eve@example.com")
                .build();

        when(employeeService.findById(7)).thenReturn(existing);

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", 999); // attempt to change id should be rejected
        String json = objectMapper.writeValueAsString(payload);

        mockMvc.perform(patch("/api/employees/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void patchEmployee_partialUpdate_success() throws Exception {
        Employee existing = Employee.builder()
                .id(8)
                .firstName("Frank")
                .lastName("Miller")
                .email("frank@example.com")
                .build();

        when(employeeService.findById(8)).thenReturn(existing);

        Map<String, Object> patchPayload = Collections.singletonMap("firstName", "Franklin");
        String json = objectMapper.writeValueAsString(patchPayload);

        // let the real JsonMapper perform the update; ensure save echoes the saved object
        when(employeeService.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(patch("/api/employees/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(8)))
                .andExpect(jsonPath("$.firstName", is("Franklin")));
    }

    @Test
    void patchEmployee_nonExistent_returnsServerError() throws Exception {
        when(employeeService.findById(42)).thenReturn(null);

        Map<String, Object> patchPayload = Collections.singletonMap("firstName", "Ghost");
        String json = objectMapper.writeValueAsString(patchPayload);

        mockMvc.perform(patch("/api/employees/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError());
    }
}