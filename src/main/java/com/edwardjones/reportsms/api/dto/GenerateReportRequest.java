package com.edwardjones.reportsms.api.dto;

import com.edwardjones.reportsms.domain.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class GenerateReportRequest {

    @NotNull
    private ReportType reportType;

    @NotBlank
    @Size(max = 128)
    private String requestedBy;

    @NotNull
    private Map<String, Object> parameters;
}

