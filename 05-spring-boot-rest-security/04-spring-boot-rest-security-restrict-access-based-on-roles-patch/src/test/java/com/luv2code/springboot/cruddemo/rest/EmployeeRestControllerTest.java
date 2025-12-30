package com.luv2code.springboot.cruddemo.rest;

import com.luv2code.springboot.cruddemo.entity.Employee;
import com.luv2code.springboot.cruddemo.security.DemoSecurityConfig;
import com.luv2code.springboot.cruddemo.service.EmployeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeRestController.class)
@Import(DemoSecurityConfig.class)
class EmployeeRestControllerMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EmployeeService employeeService;

    @Autowired
    JsonMapper jsonMapper;
    
    private Employee createEmployee(int id) {
        return Employee.builder()
                .id(id)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();
    }

    @Test
    @DisplayName("GET /api/employees - allowed for EMPLOYEE and returns list")
    @WithMockUser(username = "john", roles = {"EMPLOYEE"})
    void findAllEmployees_asEmployee_returnsList() throws Exception {
        List<Employee> employees = List.of(createEmployee(1), createEmployee(2));
        when(employeeService.findAll()).thenReturn(employees);

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(employeeService).findAll();
    }

    @Test
    @DisplayName("GET /api/employees/{id} - edge case: employee not found -> 500")
    @WithMockUser(username = "john", roles = {"EMPLOYEE"})
    void getEmployee_notFound_throwsException() {
        int missingId = 99;
        when(employeeService.findById(missingId)).thenReturn(null);

        assertThatThrownBy(() ->
                mockMvc.perform(get("/api/employees/" + missingId)))
                .hasMessageContaining("Employee id not found - " + missingId);

        verify(employeeService).findById(missingId);
    }

    @Test
    @DisplayName("POST /api/employees - EMPLOYEE forbidden (needs MANAGER)")
    @WithMockUser(username = "john", roles = {"EMPLOYEE"})
    void addEmployee_employeeRole_forbidden() throws Exception {
        Employee incoming = createEmployee(0);
        String json = jsonMapper.writeValueAsString(incoming);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/employees/{id} - Rejects ID modification")
    @WithMockUser(username = "mary", roles = {"MANAGER"})
    void patchEmployee_payloadContainsId_returnsError() {
        int id = 5;
        when(employeeService.findById(id)).thenReturn(createEmployee(id));

        Map<String, Object> payload = Map.of(
                "id", 999,
                "firstName", "Hacker"
        );

        String json = jsonMapper.writeValueAsString(payload);

        assertThatThrownBy(() ->
                mockMvc.perform(patch("/api/employees/{employeeId}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)))
                .hasMessageContaining("Employee id cannot be modified");

    }

    @Test
    @DisplayName("PATCH /api/employees/{id} - Happy Path")
    @WithMockUser(username = "mary", roles = {"MANAGER"})
    void patchEmployee_validPayload_updatesEmployee() throws Exception {
        int id = 5;

        Employee existing = createEmployee(id);
        when(employeeService.findById(id)).thenReturn(existing);

        Map<String, Object> payload = Map.of(
                "firstName", "UpdatedName"
        );

        String json = jsonMapper.writeValueAsString(payload);

        Employee patched = Employee.builder()
                .id(id)
                .firstName("UpdatedName")
                .lastName(existing.getLastName())
                .email(existing.getEmail())
                .build();

        when(employeeService.save(any(Employee.class))).thenReturn(patched);

        mockMvc.perform(patch("/api/employees/{employeeId}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.firstName").value("UpdatedName"));

    }
}
