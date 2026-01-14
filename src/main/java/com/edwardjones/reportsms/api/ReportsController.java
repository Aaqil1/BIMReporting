package com.edwardjones.reportsms.api;

import com.edwardjones.reportsms.api.dto.GenerateReportRequest;
import com.edwardjones.reportsms.api.dto.GenerateReportResponse;
import com.edwardjones.reportsms.api.dto.ReportDetailsResponse;
import com.edwardjones.reportsms.api.dto.ReportStatusResponse;
import com.edwardjones.reportsms.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportsController {

    private final ReportService reportService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('SCOPE_reports.write')")
    public GenerateReportResponse generate(@Valid @RequestBody GenerateReportRequest request) {
        return reportService.submitReport(request);
    }

    @GetMapping("/{requestId}/status")
    @PreAuthorize("hasAuthority('SCOPE_reports.read')")
    public ReportStatusResponse status(@PathVariable String requestId) {
        return reportService.getStatus(requestId);
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("hasAuthority('SCOPE_reports.read')")
    public ReportDetailsResponse details(@PathVariable String requestId) {
        return reportService.getDetails(requestId);
    }
}

