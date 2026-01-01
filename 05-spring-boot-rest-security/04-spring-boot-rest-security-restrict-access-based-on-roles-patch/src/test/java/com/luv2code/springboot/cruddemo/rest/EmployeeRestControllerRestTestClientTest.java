package com.luv2code.springboot.cruddemo.rest;

import com.luv2code.springboot.cruddemo.entity.Employee;
import com.luv2code.springboot.cruddemo.security.DemoSecurityConfig;
import com.luv2code.springboot.cruddemo.service.EmployeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(EmployeeRestController.class)
@AutoConfigureRestTestClient
@Import(DemoSecurityConfig.class)
class EmployeeRestControllerRestTestClientTest {

    @Autowired
    RestTestClient restTestClient;

    @MockitoBean
    EmployeeService employeeService;

    @Autowired
    JsonMapper jsonMapper;

    @Captor
    ArgumentCaptor<Employee> employeeArgumentCaptor;

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
    void findAllEmployees_asEmployee_returnsList() {
        List<Employee> employees = List.of(createEmployee(1), createEmployee(2));
        when(employeeService.findAll()).thenReturn(employees);

        List<Employee> response = restTestClient.get()
                .uri("/api/employees")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Employee>>() {})
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response).hasSize(2);
        assertThat(response.get(0).getId()).isEqualTo(1);
        assertThat(response.get(0).getFirstName()).isEqualTo("John");
        assertThat(response.get(1).getId()).isEqualTo(2);

        verify(employeeService).findAll();
    }

    @Test
    @DisplayName("GET /api/employees/{id} - edge case: employee not found -> 500")
    @WithMockUser(username = "john", roles = {"EMPLOYEE"})
    void getEmployee_notFound_returnsErrorResponse() {
        int missingId = 99;
        when(employeeService.findById(missingId)).thenReturn(null);

        var result = restTestClient.get()
                .uri("/api/employees/{id}", missingId)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(String.class)
                .returnResult();

        assertThat(result.getResponseBody())
                .isNotNull()
                .contains("Employee id not found - " + missingId);

        verify(employeeService).findById(missingId);
    }

    @Test
    @DisplayName("POST /api/employees - EMPLOYEE forbidden (needs MANAGER)")
    @WithMockUser(username = "john", roles = {"EMPLOYEE"})
    void addEmployee_employeeRole_forbidden() {
        Employee incoming = createEmployee(0);
        String json = jsonMapper.writeValueAsString(incoming);

        restTestClient.post()
                .uri("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .exchange()
                .expectStatus().isForbidden();
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

        var result = restTestClient.patch()
                .uri("/api/employees/{employeeId}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(String.class)
                .returnResult();

        assertThat(result.getResponseBody())
                .isNotNull()
                .contains("Employee id cannot be modified");
    }

    @Test
    @DisplayName("PATCH /api/employees/{id} - Happy Path")
    @WithMockUser(username = "mary", roles = {"MANAGER"})
    void patchEmployee_validPayload_updatesEmployee() {
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

        Employee response = restTestClient.patch()
                .uri("/api/employees/{employeeId}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Employee.class)
                .returnResult()
                .getResponseBody();

        // Response assertions
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getFirstName()).isEqualTo("UpdatedName");

        // Capture and assert the actual saved Employee
        verify(employeeService).save(employeeArgumentCaptor.capture());

        Employee saved = employeeArgumentCaptor.getValue();
        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getFirstName()).isEqualTo("UpdatedName");
        assertThat(saved.getLastName()).isEqualTo(existing.getLastName());
        assertThat(saved.getEmail()).isEqualTo(existing.getEmail());
    }
}
