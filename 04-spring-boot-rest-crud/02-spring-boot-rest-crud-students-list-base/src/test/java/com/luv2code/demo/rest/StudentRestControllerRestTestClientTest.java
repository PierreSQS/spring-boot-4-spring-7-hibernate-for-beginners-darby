package com.luv2code.demo.rest;

import com.luv2code.demo.entity.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@WebMvcTest(StudentRestController.class)
@AutoConfigureRestTestClient
class StudentRestControllerRestTest {

    @Autowired
    private RestTestClient restClient;

    @Test
    @DisplayName("GET /api/students -> returns list of 3 students")
    void getStudents_returnsList() {
        // build expected Student instances using Lombok builder (for clearer assertions)
        Student s1 = Student.builder().firstName("Poornima").lastName("Patel").build();
        Student s2 = Student.builder().firstName("Mario").lastName("Rossi").build();
        Student s3 = Student.builder().firstName("Mary").lastName("Smith").build();

        restClient.get()
                .uri("/api/students")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                    .jsonPath("$.length()").isEqualTo(3)
                    .jsonPath("$[0].firstName").isEqualTo(s1.getFirstName())
                    .jsonPath("$[0].lastName").isEqualTo(s1.getLastName())
                    .jsonPath("$[1].firstName").isEqualTo(s2.getFirstName())
                    .jsonPath("$[2].firstName").isEqualTo(s3.getFirstName());
    }

    @Test
    @DisplayName("GET wrong path -> 404 Not Found (edge case)")
    void getWrongPath_returnsNotFound() {
        restClient.get()
                .uri("/api/student") // singular path does not exist
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("POST to GET-only endpoint -> 405 Method Not Allowed (edge case)")
    void postToGetOnlyEndpoint_returnsMethodNotAllowed() {
        Student student = Student.builder().firstName("X").lastName("Y").build();

        restClient.post()
                .uri("/api/students")
                .contentType(MediaType.APPLICATION_JSON)
                .body(student)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }
}