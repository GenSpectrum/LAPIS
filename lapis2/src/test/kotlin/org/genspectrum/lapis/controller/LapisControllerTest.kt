package org.genspectrum.lapis.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest
class LapisControllerTest(@Autowired val mockMvc: MockMvc) {
    @Test
    fun aggregated() {
        mockMvc.perform(get("/aggregated"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("\$.count").value(0))
    }
}