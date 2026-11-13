package com.appointech.appointech_backend.infrastructure.controllers;

import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SaludControllerTest {
    @Test
    void saludPublicaRespondeJsonParaLasSondas() throws Exception {
        MockMvcBuilders.standaloneSetup(new SaludController()).build()
                .perform(get("/api/salud"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"estado\":\"UP\"}"));
    }
}
