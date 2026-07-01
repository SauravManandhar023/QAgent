package com.saurav.agentic.tools;

import com.saurav.agentic.models.TestCase;
import com.saurav.agentic.utils.ExcelUtil;
import java.util.List;

public class DumpTC {
    public static void main(String[] args) throws Exception {
        List<TestCase> tcs = ExcelUtil.readTestCases("test-output/excel/ui-test-cases.xlsx");
        int i = 1;
        for (TestCase tc : tcs) {
            System.out.println("=== TC #" + (i++) + " ===");
            System.out.println("ID: " + tc.getTestCaseId());
            System.out.println("Name: " + tc.getTestCaseName());
            System.out.println("Component: " + tc.getComponent());
            System.out.println("Type: " + tc.getTestType() + " | Priority: " + tc.getPriority() + " | Auto: " + tc.isAutomationFeasible());
            System.out.println("Data: " + tc.getTestData());
            System.out.println("Expected: " + tc.getExpectedResult());
            System.out.println();
        }
    }
}
