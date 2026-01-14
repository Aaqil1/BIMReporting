package com.edwardjones.reportsms.processing;

import com.edwardjones.reportsms.domain.ReportType;
import com.edwardjones.reportsms.processing.strategy.AssetAllocationReportStrategy;
import com.edwardjones.reportsms.processing.strategy.BenchmarkSummaryReportStrategy;
import com.edwardjones.reportsms.processing.strategy.DiversificationBarReportStrategy;
import com.edwardjones.reportsms.processing.strategy.PerformanceReportStrategy;
import com.edwardjones.reportsms.processing.strategy.ReportGenerationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportStrategyFactory {

    private final PerformanceReportStrategy performanceReportStrategy;
    private final BenchmarkSummaryReportStrategy benchmarkSummaryReportStrategy;
    private final DiversificationBarReportStrategy diversificationBarReportStrategy;
    private final AssetAllocationReportStrategy assetAllocationReportStrategy;

    public ReportGenerationStrategy getStrategy(ReportType reportType) {
        return switch (reportType) {
            case PERFORMANCE -> performanceReportStrategy;
            case BENCHMARK_SUMMARY -> benchmarkSummaryReportStrategy;
            case DIVERSIFICATION_BAR -> diversificationBarReportStrategy;
            case ASSET_ALLOCATION -> assetAllocationReportStrategy;
        };
    }
}

