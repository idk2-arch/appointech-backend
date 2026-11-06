package com.appointech.appointech_backend.infrastructure.dto;

import java.util.List;

public record AlertasAdminResponse(int total, List<AlertaAdminResponse> alertas) {
}
