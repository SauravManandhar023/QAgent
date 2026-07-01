package com.saurav.agentic.utils;

import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.models.ApiTestCase;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ApiExcelUtil - Handles reading and writing API test cases to Excel
 */
public class ApiExcelUtil {

    private ApiExcelUtil() {}

    public static void writeApiTestCases(List<ApiTestCase> testCases,
                                          String filePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        createApiSheet(workbook, testCases);
        createApiSummarySheet(workbook, testCases);

        File file = new File(filePath);
        file.getParentFile().mkdirs();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println(FrameworkConstants.LOG_SUCCESS +
                " API Excel saved: " + filePath);
        System.out.println(FrameworkConstants.LOG_SUCCESS +
                " Total API test cases: " + testCases.size());
    }

    private static void createApiSheet(Workbook workbook,
                                        List<ApiTestCase> cases) {
        Sheet sheet = workbook.createSheet("API Test Cases");
        CellStyle headerStyle = createHeaderStyle(workbook);

        String[] headers = {
            "Test Case ID", "Test Case Name", "Endpoint", "Method",
            "Request Params", "Request Body", "Expected Status",
            "Expected Field", "Assertion Type", "Test Type",
            "Priority", "Description", "Automation Feasible"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        for (int i = 0; i < cases.size(); i++) {
            ApiTestCase tc = cases.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(tc.getTestCaseId());
            row.createCell(1).setCellValue(tc.getTestCaseName());
            row.createCell(2).setCellValue(tc.getEndpoint());
            row.createCell(3).setCellValue(tc.getMethod());
            row.createCell(4).setCellValue(tc.getRequestParams());
            row.createCell(5).setCellValue(tc.getRequestBody());
            row.createCell(6).setCellValue(tc.getExpectedStatusCode());
            row.createCell(7).setCellValue(tc.getExpectedResponseField());
            row.createCell(8).setCellValue(tc.getAssertionType());
            row.createCell(9).setCellValue(tc.getTestType());
            row.createCell(10).setCellValue(tc.getPriority());
            row.createCell(11).setCellValue(tc.getDescription());
            row.createCell(12).setCellValue(
                    tc.isAutomationFeasible() ? "Yes" : "No");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static void createApiSummarySheet(Workbook workbook,
                                               List<ApiTestCase> cases) {
        Sheet sheet = workbook.createSheet("Summary");
        CellStyle headerStyle = createHeaderStyle(workbook);

        Row title = sheet.createRow(0);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("API Test Suite Summary");
        titleCell.setCellStyle(headerStyle);

        long positive = cases.stream()
                .filter(c -> "Positive".equals(c.getTestType())).count();
        long negative = cases.stream()
                .filter(c -> "Negative".equals(c.getTestType())).count();
        long edge = cases.stream()
                .filter(c -> "Edge".equals(c.getTestType())).count();

        String[][] stats = {
            {"Total API Test Cases", String.valueOf(cases.size())},
            {"Positive Cases",       String.valueOf(positive)},
            {"Negative Cases",       String.valueOf(negative)},
            {"Edge Cases",           String.valueOf(edge)},
            {"LLM Tokens Used",      "0 (template-based)"}
        };

        for (int i = 0; i < stats.length; i++) {
            Row row = sheet.createRow(i + 2);
            row.createCell(0).setCellValue(stats[i][0]);
            row.createCell(1).setCellValue(stats[i][1]);
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    public static List<ApiTestCase> readApiTestCases(String filePath)
            throws IOException {
        List<ApiTestCase> cases = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet("API Test Cases");
            if (sheet == null) return cases;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                ApiTestCase tc = new ApiTestCase();
                tc.setTestCaseId(cell(row, 0));
                tc.setTestCaseName(cell(row, 1));
                tc.setEndpoint(cell(row, 2));
                tc.setMethod(cell(row, 3));
                tc.setRequestParams(cell(row, 4));
                tc.setRequestBody(cell(row, 5));
                tc.setExpectedStatusCode(cell(row, 6));
                tc.setExpectedResponseField(cell(row, 7));
                tc.setAssertionType(cell(row, 8));
                tc.setTestType(cell(row, 9));
                tc.setPriority(cell(row, 10));
                tc.setDescription(cell(row, 11));
                tc.setAutomationFeasible(
                        cell(row, 12).equalsIgnoreCase("Yes"));
                cases.add(tc);
            }
        }
        return cases;
    }

    private static String cell(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default      -> "";
        };
    }
}