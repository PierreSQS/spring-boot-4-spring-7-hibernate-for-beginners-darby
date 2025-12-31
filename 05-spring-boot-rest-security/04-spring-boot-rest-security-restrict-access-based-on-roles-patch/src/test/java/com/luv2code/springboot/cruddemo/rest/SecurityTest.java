package com.luv2code.springboot.cruddemo.rest;

import com.luv2code.springboot.cruddemo.entity.Employee;
import com.luv2code.springboot.cruddemo.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EmployeeService employeeService;

    @Autowired
    JsonMapper objectMapper;

    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = Employee.builder()
                .id(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();
    }

    @Test
    @WithUserDetails(value = "mary")
    @DisplayName("PATCH /employees/{id} - Partially updates an employee for MANAGER role")
    void patchEmployeeUpdatesWithRealEmployeeForManagerRole() throws Exception {
        // Given
        Map<String, Object> patchPayload = Map.of("lastName", "Smith");
        Employee patchedEmployee = Employee.builder()
                .id(1)
                .firstName("John")
                .lastName("Smith")
                .email("john.doe@example.com")
                .build();

        given(employeeService.findById(1)).willReturn(employee);
        given(employeeService.save(any(Employee.class))).willReturn(patchedEmployee);

        // When & Then
        mockMvc.perform(patch("/api/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchPayload))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andDo(print());
    }

    @Test
    @WithUserDetails(value = "john")
    @DisplayName("PATCH /employees/{id} - Access denied for EMPLOYEE role")
    void patchEmployeeAccessDeniedForEmployeeRole() throws Exception {
        // Given
        Map<String, Object> patchPayload = Map.of("lastName", "Smith");

        // When & Then
        mockMvc.perform(patch("/api/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchPayload))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andDo(print());
    }
}
