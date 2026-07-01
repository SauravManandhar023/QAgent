package com.saurav.agentic.utils;

import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.models.TestCase;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ExcelUtil - Handles reading and writing test cases to Excel
 * Uses Apache POI for Excel operations
 *
 */
public class ExcelUtil {

    private ExcelUtil() {}

    /**
     * Write test cases to Excel file with separate sheets per component
     */
    public static void writeTestCases(List<TestCase> testCases, String filePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();

        createSummarySheet(workbook, testCases);
        createComponentSheets(workbook, testCases);

        // Create directories if not exist
        File file = new File(filePath);
        file.getParentFile().mkdirs();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println(FrameworkConstants.LOG_SUCCESS + " Excel saved: " + filePath);
        System.out.println(FrameworkConstants.LOG_SUCCESS + " Total test cases: " + testCases.size());
    }

    /**
     * Append Instead of Overwrite
     */
    public static void appendTestCases(String filePath,
                                       List<TestCase> newCases,
                                       boolean isFirstRun) throws IOException {
        if (isFirstRun) {
            // Fresh write — existing behavior
            writeTestCases(newCases, filePath);  // ← correct method + order
            return;
        }

        // Incremental — read existing, append new, save all
        List<TestCase> existing = readTestCases(filePath);

        // Deduplicate by name before appending
        Set<String> existingNames = existing.stream()
                .map(tc -> tc.getTestCaseName().toLowerCase().trim())
                .collect(Collectors.toSet());

        List<TestCase> deduplicated = newCases.stream()
                .filter(tc -> !existingNames.contains(
                        tc.getTestCaseName().toLowerCase().trim()))
                .toList();

        System.out.println(FrameworkConstants.LOG_INFO +
                " New unique test cases to append: " + deduplicated.size() +
                " (skipped " + (newCases.size() - deduplicated.size()) +
                " duplicates)");

        existing.addAll(deduplicated);
        writeTestCases(existing, filePath);  // ← correct method + order

        System.out.println(FrameworkConstants.LOG_SUCCESS +
                " Excel updated: " + existing.size() + " total test cases");
    }


    /**
     * Create Summary sheet
     */
    private static void createSummarySheet(Workbook workbook, List<TestCase> testCases) {
        Sheet sheet = workbook.createSheet(FrameworkConstants.SHEET_SUMMARY);
        CellStyle headerStyle = createHeaderStyle(workbook);

        // Title
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("AI Generated Test Suite - Summary");
        titleCell.setCellStyle(headerStyle);

        // Stats
        long positive = testCases.stream()
                .filter(tc -> tc.getTestType().equalsIgnoreCase("Positive")).count();
        long negative = testCases.stream()
                .filter(tc -> tc.getTestType().equalsIgnoreCase("Negative")).count();
        long edge = testCases.stream()
                .filter(tc -> tc.getTestType().equalsIgnoreCase("Edge")).count();
        long accessibility = testCases.stream()
                .filter(tc -> tc.getTestType().equalsIgnoreCase("Accessibility")).count();
        long automatable = testCases.stream()
                .filter(TestCase::isAutomationFeasible).count();

        // Component breakdown
        Map<String, Long> componentCounts = testCases.stream()
                .collect(Collectors.groupingBy(TestCase::getComponent, Collectors.counting()));

        String[][] stats = {
                {"Total Test Cases", String.valueOf(testCases.size())},
                {"Positive Cases", String.valueOf(positive)},
                {"Negative Cases", String.valueOf(negative)},
                {"Edge Cases", String.valueOf(edge)},
                {"Accessibility Cases", String.valueOf(accessibility)},
                {"Automation Feasible", String.valueOf(automatable)}
        };

        for (int i = 0; i < stats.length; i++) {
            Row row = sheet.createRow(i + 2);
            row.createCell(0).setCellValue(stats[i][0]);
            row.createCell(1).setCellValue(stats[i][1]);
        }

        // Add component breakdown
        int rowOffset = stats.length + 4;
        Row headerRow = sheet.createRow(rowOffset);
        headerRow.createCell(0).setCellValue("Component");
        headerRow.createCell(1).setCellValue("Test Case Count");
        headerRow.createCell(0).setCellStyle(headerStyle);
        headerRow.createCell(1).setCellStyle(headerStyle);

        int rowIdx = rowOffset + 1;
        for (Map.Entry<String, Long> entry : componentCounts.entrySet()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue().toString());
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    /**
     * Create separate sheets for each component
     */
    private static void createComponentSheets(Workbook workbook, List<TestCase> testCases) {
        // Group test cases by component
        Map<String, List<TestCase>> groupedByComponent = testCases.stream()
                .collect(Collectors.groupingBy(TestCase::getComponent));

        CellStyle headerStyle = createHeaderStyle(workbook);

        // Create a sheet for each component
        for (Map.Entry<String, List<TestCase>> entry : groupedByComponent.entrySet()) {
            String component = entry.getKey();
            List<TestCase> componentTestCases = entry.getValue();

            // Create valid sheet name (remove invalid chars, limit to 31 chars for Excel)
            String sheetName = sanitizeSheetName(component);
            Sheet sheet = workbook.createSheet(sheetName);

            // Headers
            String[] headers = {
                    "Test Case ID", "Test Case Name", "Description", "Preconditions",
                    "Test Steps", "Test Data", "Expected Result", "Test Type", "Priority",
                    "Automation Feasible"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            for (int i = 0; i < componentTestCases.size(); i++) {
                TestCase tc = componentTestCases.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(tc.getTestCaseId());
                row.createCell(1).setCellValue(tc.getTestCaseName());
                row.createCell(2).setCellValue(tc.getDescription());
                row.createCell(3).setCellValue(tc.getPreconditions());
                row.createCell(4).setCellValue(tc.getTestSteps());
                row.createCell(5).setCellValue(tc.getTestData());
                row.createCell(6).setCellValue(tc.getExpectedResult());
                row.createCell(7).setCellValue(tc.getTestType());
                row.createCell(8).setCellValue(tc.getPriority());
                row.createCell(9).setCellValue(tc.isAutomationFeasible() ? "Yes" : "No");
            }

            // Auto size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
        }
    }

    /**
     * Read test cases from Excel file (reads from all component sheets)
     */
    public static List<TestCase> readTestCases(String filePath) throws IOException {
        List<TestCase> testCases = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            // Skip the summary sheet, read all other sheets
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                // Skip summary sheet
                if (FrameworkConstants.SHEET_SUMMARY.equals(sheet.getSheetName())) {
                    continue;
                }

                // Skip header row (row 0)
                for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                    Row row = sheet.getRow(rowIdx);
                    if (row == null) continue;

                    TestCase tc = new TestCase();
                    tc.setTestCaseId(getCellValue(row, 0));
                    tc.setTestCaseName(getCellValue(row, 1));
                    tc.setDescription(getCellValue(row, 2));
                    tc.setPreconditions(getCellValue(row, 3));
                    tc.setTestSteps(getCellValue(row, 4));
                    tc.setTestData(getCellValue(row, 5));
                    tc.setExpectedResult(getCellValue(row, 6));
                    tc.setTestType(getCellValue(row, 7));
                    tc.setPriority(getCellValue(row, 8));
                    // Component is derived from sheet name
                    String componentFromSheet = sheet.getSheetName();
                    tc.setComponent(componentFromSheet);
                    tc.setAutomationFeasible(
                            getCellValue(row, 9).equalsIgnoreCase("Yes"));

                    testCases.add(tc);
                }
            }
        }

        System.out.println(FrameworkConstants.LOG_SUCCESS +
                " Read " + testCases.size() + " test cases from: " + filePath);
        return testCases;
    }

    /**
     * Safely get cell value as String
     */
    private static String getCellValue(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    /**
     * Create header cell style
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    /**
     * Sanitize a string to be used as an Excel sheet name
     * Removes invalid characters: \ / ? * [ ] :
     * Limits length to 31 characters (Excel maximum)
     */
    private static String sanitizeSheetName(String name) {
        if (name == null || name.isEmpty()) {
            return "Component";
        }

        // Remove invalid Excel sheet name characters: \ / ? * [ ] :
        String sanitized = name
                .replace("\\", "")  // backslash
                .replace("/", "")   // forward slash
                .replace("?", "")   // question mark
                .replace("*", "")   // asterisk
                .replace("[", "")   // opening bracket
                .replace("]", "")   // closing bracket
                .replace("\"", "")  // double quote
                .replace(":", "");  // colon

        // Trim whitespace
        sanitized = sanitized.trim();

        // If after sanitization we have an empty string, use default
        if (sanitized.isEmpty()) {
            return "Component";
        }

        // Limit to 31 characters (Excel sheet name limit)
        if (sanitized.length() > 31) {
            sanitized = sanitized.substring(0, 31);
        }

        return sanitized;
    }
}