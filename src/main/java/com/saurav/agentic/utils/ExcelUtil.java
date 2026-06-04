package com.saurav.agentic.utils;

import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.models.TestCase;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ExcelUtil - Handles reading and writing test cases to Excel
 * Uses Apache POI for Excel operations
 *
 */
public class ExcelUtil {

    private ExcelUtil() {}

    /**
     * Write test cases to Excel file
     */
    public static void writeTestCases(List<TestCase> testCases, String filePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();

        createSummarySheet(workbook, testCases);
        createTestCasesSheet(workbook, testCases);

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

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    /**
     * Create TestCases sheet
     */
    private static void createTestCasesSheet(Workbook workbook, List<TestCase> testCases) {
        Sheet sheet = workbook.createSheet(FrameworkConstants.SHEET_TEST_CASES);
        CellStyle headerStyle = createHeaderStyle(workbook);

        // Headers
        String[] headers = {
        	    "Test Case ID", "Test Case Name", "Description", "Preconditions",
        	    "Test Steps", "Test Data", "Expected Result", "Test Type", "Priority",    // ← added Test Data
        	    "Component", "Automation Feasible"
        	};

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);
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
            row.createCell(9).setCellValue(tc.getComponent());          
            row.createCell(10).setCellValue(tc.isAutomationFeasible() ? "Yes" : "No"); 
        }

        // Auto size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Read test cases from Excel file
     */
    public static List<TestCase> readTestCases(String filePath) throws IOException {
        List<TestCase> testCases = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(FrameworkConstants.SHEET_TEST_CASES);
            if (sheet == null) {
                throw new IOException(FrameworkConstants.LOG_ERROR +
                        " Sheet not found: " + FrameworkConstants.SHEET_TEST_CASES);
            }

            // Skip header row (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
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
                tc.setComponent(getCellValue(row, 9));
                tc.setAutomationFeasible(
                        getCellValue(row, 10).equalsIgnoreCase("Yes"));

                testCases.add(tc);
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
}