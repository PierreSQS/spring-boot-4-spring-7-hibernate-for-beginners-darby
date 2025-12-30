package com.luv2code.springboot.cruddemo.rest;

import com.luv2code.springboot.cruddemo.entity.Employee;
import com.luv2code.springboot.cruddemo.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class EmployeeRestControllerRestTestClientTest {

    private RestTestClient client;

    @MockitoBean
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder().build();

        EmployeeRestController controller =
                new EmployeeRestController(employeeService, jsonMapper);

        client = RestTestClient.bindToController(controller).build();
    }

    private Employee createEmployee(int id) {
        return Employee.builder()
                .id(id)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();
    }

    // -------------------------------------------------------------------------
    // GET /api/employees
    // -------------------------------------------------------------------------

    @Test
    void findAllEmployees_returnsList() {
        Employee e1 = createEmployee(1);
        Employee e2 = createEmployee(2);
        when(employeeService.findAll()).thenReturn(List.of(e1, e2));

        List<Employee> employees = client.get()
                .uri("/api/employees")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Employee>>() {})
                .returnResult()
                .getResponseBody();

        assertThat(employees)
                .isNotNull()
                .hasSize(2);
        assertThat(employees.getFirst().getId()).isEqualTo(1);

        verify(employeeService).findAll();
    }

    @Test
    void findAllEmployees_emptyList() {
        when(employeeService.findAll()).thenReturn(Collections.emptyList());

        List<Employee> employees = client.get()
                .uri("/api/employees")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Employee>>() {})
                .returnResult()
                .getResponseBody();

        assertThat(employees)
                .isNotNull()
                .isEmpty();

        verify(employeeService).findAll();
    }

    // -------------------------------------------------------------------------
    // GET /api/employees/{employeeId} - find Employee By Id
    // -------------------------------------------------------------------------

    @Test
    void getEmployee_existingId_returnsEmployee() {
        Employee e = createEmployee(1);
        when(employeeService.findById(1)).thenReturn(e);

        Employee result = client.get()
                .uri("/api/employees/{employeeId}", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Employee.class)
                .returnResult()
                .getResponseBody();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getFirstName()).isEqualTo("John");

        verify(employeeService).findById(1);
    }

    @Test
    void getEmployee_nonExistingId_throwsRuntimeException_resultsInServerError() {
        when(employeeService.findById(42)).thenReturn(null);

        String body = client.get()
                .uri("/api/employees/{employeeId}", 42)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(body)
                .isNotNull()
                .contains("Employee id not found - 42");

        verify(employeeService).findById(42);
    }

    // -------------------------------------------------------------------------
    // POST /api/employees
    // -------------------------------------------------------------------------

    @Test
    void addEmployee_resetsIdToZero_beforeSaving_newIdReturned() {
        Employee incoming = createEmployee(123); // client sends id 123 (should be ignored)
        Employee saved = createEmployee(10);     // DB assigns id 10

        when(employeeService.save(any(Employee.class))).thenReturn(saved);

        Employee result = client.post()
                .uri("/api/employees")
                .body(incoming)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Employee.class)
                .returnResult()
                .getResponseBody();

        assertThat(result)
                .isNotNull();
        assertThat(result.getId()).isEqualTo(10);

        // verify that theEmployee.setId(0) was applied before save
        verify(employeeService).save(argThat(emp -> emp.getId() == 0));
    }

    // -------------------------------------------------------------------------
    // PUT /api/employees
    // -------------------------------------------------------------------------

    @Test
    void updateEmployee_existingEmployee_isSavedAndReturned() {
        Employee incoming = createEmployee(5);
        incoming.setFirstName("Updated");

        when(employeeService.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Employee result = client.put()
                .uri("/api/employees")
                .body(incoming)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Employee.class)
                .returnResult()
                .getResponseBody();

        assertThat(result)
                .isNotNull();
        assertThat(result.getId()).isEqualTo(5);
        assertThat(result.getFirstName()).isEqualTo("Updated");

        verify(employeeService).save(argThat(emp ->
                emp.getId() == 5 &&
                        "Updated".equals(emp.getFirstName())
        ));

    }

    // -------------------------------------------------------------------------
    // PATCH /api/employees/{employeeId} - patch employee by id
    // -------------------------------------------------------------------------

    @Test
    void patchEmployee_partialUpdate_success() {
        // Existing employee in DB
        Employee existing = createEmployee(7);
        when(employeeService.findById(7)).thenReturn(existing);

        // Save should return the patched employee
        when(employeeService.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String patchJson = """
                {
                  "firstName": "Patched",
                  "email": "patched@example.com"
                }
                """;

        Employee result = client.patch()
                .uri("/api/employees/{employeeId}", 7)
                .contentType(MediaType.APPLICATION_JSON)
                .body(patchJson)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Employee.class)
                .returnResult()
                .getResponseBody();

        assertThat(result)
                .isNotNull();
        // ID must remain unchanged
        assertThat(result.getId()).isEqualTo(7);
        // Patched fields
        assertThat(result.getFirstName()).isEqualTo("Patched");
        assertThat(result.getEmail()).isEqualTo("patched@example.com");
        // Unchanged field
        assertThat(result.getLastName()).isEqualTo("Doe");

        verify(employeeService).findById(7);
        verify(employeeService).save(any(Employee.class));
    }

    @Test
    void patchEmployee_withIdInPayload_isRejected() {
        Employee existing = createEmployee(8);
        when(employeeService.findById(8)).thenReturn(existing);

        String patchJson = """
                {
                  "id": 99,
                  "firstName": "Hacker"
                }
                """;

        String body = client.patch()
                .uri("/api/employees/{employeeId}", 8)
                .contentType(MediaType.APPLICATION_JSON)
                .body(patchJson)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(body)
                .isNotNull()
                .contains("Employee id cannot be modified");

        // findById is called, but save() must NOT be called
        verify(employeeService).findById(8);
        verify(employeeService, never()).save(any(Employee.class));
    }

    @Test
    void patchEmployee_nonExistingId_resultsInServerError() {
        when(employeeService.findById(99)).thenReturn(null);

        String patchJson = """
                {
                  "firstName": "NoOne"
                }
                """;

        String body = client.patch()
                .uri("/api/employees/{employeeId}", 99)
                .contentType(MediaType.APPLICATION_JSON)
                .body(patchJson)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(body)
                .isNotNull()
                .contains("Employee id not found - 99");

        verify(employeeService).findById(99);
        verify(employeeService, never()).save(any(Employee.class));
    }
}
