// language: java
package com.luv2code.demo.rest;

import com.luv2code.demo.entity.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentRestController.class)
class StudentRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Test
    @DisplayName("GET /api/students -> returns list of 3 students")
    void getStudents_returnsList() throws Exception {
        // build expected Student instances using Lombok builder (used only for clearer assertions)
        Student s1 = Student.builder().firstName("Poornima").lastName("Patel").build();
        Student s2 = Student.builder().firstName("Mario").lastName("Rossi").build();
        Student s3 = Student.builder().firstName("Mary").lastName("Smith").build();

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(3)))
                .andExpect(jsonPath("$[0].firstName", is(s1.getFirstName())))
                .andExpect(jsonPath("$[0].lastName", is(s1.getLastName())))
                .andExpect(jsonPath("$[1].firstName", is(s2.getFirstName())))
                .andExpect(jsonPath("$[2].firstName", is(s3.getFirstName())))
                .andDo(print());
    }

    @Test
    @DisplayName("GET wrong path -> 404 Not Found (edge case)")
    void getWrongPath_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/student")) // singular path does not exist
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    @Test
    @DisplayName("POST to GET-only endpoint -> 405 Method Not Allowed (edge case)")
    void postToGetOnlyEndpoint_returnsMethodNotAllowed() throws Exception {
        Student student = Student.builder().firstName("X").lastName("Y").build();

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(student)))
                .andExpect(status().isMethodNotAllowed())
                .andDo(print());
    }
}