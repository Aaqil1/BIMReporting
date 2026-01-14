package com.edwardjones.reportsms.processing.strategy;

import com.edwardjones.reportsms.processing.ReportContext;
import com.edwardjones.reportsms.processing.ReportGenerationResult;

public interface ReportGenerationStrategy {
    ReportGenerationResult generate(ReportContext context);
}

