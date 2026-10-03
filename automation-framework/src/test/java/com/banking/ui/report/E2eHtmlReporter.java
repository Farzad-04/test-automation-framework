package com.banking.ui.report;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class E2eHtmlReporter implements IReporter {

    private static final Path REPORT_FILE = Path.of("target", "e2e-report", "index.html");

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        try {
            Files.createDirectories(REPORT_FILE.getParent());
            List<ITestResult> results = new ArrayList<>();
            for (ISuite suite : suites) {
                suite.getResults().values().forEach(result -> {
                    results.addAll(result.getTestContext().getPassedTests().getAllResults());
                    results.addAll(result.getTestContext().getFailedTests().getAllResults());
                    results.addAll(result.getTestContext().getSkippedTests().getAllResults());
                });
            }
            results.removeIf(result -> !result.getTestClass().getName().startsWith("com.banking.ui."));
            results.sort(Comparator.comparing(result -> result.getMethod().getMethodName()));
            Files.writeString(REPORT_FILE, render(results), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write E2E HTML report to " + REPORT_FILE, exception);
        }
    }

    private String render(List<ITestResult> results) {
        long passed = results.stream().filter(result -> result.getStatus() == ITestResult.SUCCESS).count();
        long failed = results.stream().filter(result -> result.getStatus() == ITestResult.FAILURE).count();
        long skipped = results.stream().filter(result -> result.getStatus() == ITestResult.SKIP).count();
        StringBuilder html = new StringBuilder("""
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Northstar Banking E2E Report</title>
                  <style>
                    body{margin:0;background:#f4f7f4;color:#1b302c;font:15px/1.5 Arial,sans-serif}
                    main{max-width:1100px;margin:0 auto;padding:38px 22px}
                    h1{margin:0;font-size:30px} .sub{color:#718078}
                    .summary{display:flex;gap:12px;margin:24px 0}
                    .metric,.case{border:1px solid #e1e9e2;border-radius:12px;background:white}
                    .metric{min-width:105px;padding:13px 18px}.metric strong{display:block;font-size:22px}
                    .case{margin:14px 0;padding:18px}.title{display:flex;justify-content:space-between;gap:12px}
                    .status{font-weight:bold}.pass{color:#267448}.fail{color:#b8443d}.skip{color:#a17623}
                    .shots{display:grid;grid-template-columns:repeat(auto-fit,minmax(250px,1fr));gap:12px;margin-top:14px}
                    figure{margin:0} img{display:block;width:100%;border:1px solid #e5ebe6;border-radius:8px}
                    figcaption{margin:5px;color:#718078;font-size:12px}
                    pre{overflow:auto;white-space:pre-wrap;color:#8d312e;font-size:12px}
                  </style>
                </head>
                <body><main>
                  <h1>Northstar Banking · E2E Report</h1>
                  <p class="sub">Generated from the TestNG browser UI suite. Screenshots show captured flow checkpoints.</p>
                """);
        html.append("<section class=\"summary\">")
                .append(metric("Passed", passed)).append(metric("Failed", failed)).append(metric("Skipped", skipped))
                .append("</section>");

        for (ITestResult result : results) {
            String status = result.getStatus() == ITestResult.SUCCESS ? "PASSED"
                    : result.getStatus() == ITestResult.FAILURE ? "FAILED" : "SKIPPED";
            String css = status.equals("PASSED") ? "pass" : status.equals("FAILED") ? "fail" : "skip";
            html.append("<article class=\"case\"><div class=\"title\"><strong>")
                    .append(escape(result.getMethod().getMethodName())).append("</strong><span class=\"status ")
                    .append(css).append("\">").append(status).append("</span></div><p class=\"sub\">")
                    .append(escape(result.getMethod().getDescription() == null ? "" : result.getMethod().getDescription()))
                    .append(" · ").append(result.getEndMillis() - result.getStartMillis()).append(" ms</p>");
            if (result.getThrowable() != null) {
                html.append("<pre>").append(escape(result.getThrowable().toString())).append("</pre>");
            }
            appendScreenshots(html, result);
            html.append("</article>");
        }
        return html.append("</main></body></html>").toString();
    }

    private String metric(String label, long value) {
        return "<div class=\"metric\"><span>" + label + "</span><strong>" + value + "</strong></div>";
    }

    private void appendScreenshots(StringBuilder html, ITestResult result) {
        Object value = result.getAttribute("e2eScreenshots");
        if (!(value instanceof List<?> screenshots) || screenshots.isEmpty()) {
            return;
        }
        html.append("<div class=\"shots\">");
        for (Object item : screenshots) {
            String fileName = String.valueOf(item);
            html.append("<figure><a href=\"screenshots/").append(escape(fileName))
                    .append("\"><img loading=\"lazy\" src=\"screenshots/").append(escape(fileName))
                    .append("\" alt=\"").append(escape(fileName)).append("\"></a><figcaption>")
                    .append(escape(fileName)).append("</figcaption></figure>");
        }
        html.append("</div>");
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
