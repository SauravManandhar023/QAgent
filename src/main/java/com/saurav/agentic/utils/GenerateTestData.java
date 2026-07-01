package com.saurav.agentic.utils;

import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.models.TestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * GenerateTestData - Generates high-quality test cases for the automationexercise.com homepage.
 * Run BEFORE the pipeline to populate the Excel. Used when Claude generates test data
 * instead of relying on a local LLM.
 */
public class GenerateTestData {

    public static void main(String[] args) throws Exception {
        String excelPath = "test-output/excel/ui-test-cases.xlsx";

        // Delete existing file so we write fresh
        File existing = new File(excelPath);
        if (existing.exists()) existing.delete();

        List<TestCase> testCases = buildTestCases();
        ExcelUtil.writeTestCases(testCases, excelPath);

        System.out.println(FrameworkConstants.LOG_SUCCESS + " Generated " +
                testCases.size() + " test cases to " + excelPath);

        // Print summary
        long pos = testCases.stream().filter(t -> t.getTestType().equals("Positive")).count();
        long neg = testCases.stream().filter(t -> t.getTestType().equals("Negative")).count();
        long edge = testCases.stream().filter(t -> t.getTestType().equals("Edge")).count();
        long acc = testCases.stream().filter(t -> t.getTestType().equals("Accessibility")).count();
        long auto = testCases.stream().filter(TestCase::isAutomationFeasible).count();

        System.out.println("  Positive: " + pos + ", Negative: " + neg +
                ", Edge: " + edge + ", Accessibility: " + acc);
        System.out.println("  Automation Feasible: " + auto + "/" + testCases.size());
    }

    private static List<TestCase> buildTestCases() {
        List<TestCase> list = new ArrayList<>();

        // ── TC_001: Home link navigation ──────────────────────────────────────
        list.add(build("TC_001", "Verify Home link navigates to homepage",
                "The Home link in the navigation bar should redirect to the homepage.",
                "Browser is open on the automationexercise.com homepage.",
                "Click on the 'Home' link in the top navigation bar.",
                "N/A",
                "User is redirected to the homepage.",
                "Positive", "High", true));

        // ── TC_002: Products link navigation ──────────────────────────────────
        list.add(build("TC_002", "Verify Products link navigates to products listing",
                "The Products link in the navigation bar should redirect to the products listing page.",
                "Browser is open on the automationexercise.com homepage.",
                "1. Click on the 'Products' link in the navigation bar.\n2. Verify URL contains '/products'.",
                "N/A",
                "User is navigated to the products listing page.",
                "Positive", "High", true));

        // ── TC_003: Cart link navigation ──────────────────────────────────────
        list.add(build("TC_003", "Verify Cart link navigates to cart page",
                "The Cart link should redirect to the shopping cart page.",
                "Browser is open on the homepage.",
                "1. Click on the 'Cart' link in the navigation bar.\n2. Verify URL contains '/view_cart'.",
                "N/A",
                "User is navigated to the shopping cart page.",
                "Positive", "Medium", true));

        // ── TC_004: Signup/Login link navigation ──────────────────────────────
        list.add(build("TC_004", "Verify Signup/Login link navigates to login page",
                "The Signup/Login link should redirect to the login/registration page.",
                "Browser is open on the homepage.",
                "1. Click on the 'Signup / Login' link in the navigation bar.\n2. Verify URL contains '/login'.",
                "N/A",
                "User is navigated to the login/signup page.",
                "Positive", "High", true));

        // ── TC_005: Test Cases link navigation ────────────────────────────────
        list.add(build("TC_005", "Verify Test Cases link navigates to test cases page",
                "The Test Cases link should redirect to the test cases reference page.",
                "Browser is open on the homepage.",
                "1. Click on the 'Test Cases' link in the navigation bar.\n2. Verify URL contains '/test_cases'.",
                "N/A",
                "User is navigated to the test cases page.",
                "Positive", "Medium", true));

        // ── TC_006: Contact Us link navigation ────────────────────────────────
        list.add(build("TC_006", "Verify Contact Us link navigates to contact page",
                "The Contact Us link should redirect to the contact form page.",
                "Browser is open on the homepage.",
                "1. Click on the 'Contact us' link in the navigation bar.\n2. Verify URL contains '/contact_us'.",
                "N/A",
                "User is navigated to the contact page.",
                "Positive", "Medium", true));

        // ── TC_007: Subscribe with valid email ────────────────────────────────
        list.add(build("TC_007", "Verify subscription with valid email shows success",
                "Entering a valid email in the subscription field and clicking Subscribe should show a success message.",
                "Browser is open on the homepage. Subscription form in footer is visible.",
                "1. Scroll to footer section.\n2. Enter 'testuser@example.com' in the email input.\n3. Click the 'Subscribe' button.\n4. Verify success message is displayed.",
                "email: testuser@example.com",
                "Subscription is successful with a confirmation message.",
                "Positive", "High", true));

        // ── TC_008: Subscribe with empty email ────────────────────────────────
        list.add(build("TC_008", "Verify subscription with empty email does not submit",
                "Submitting the subscription form with an empty email field should not submit.",
                "Browser is open on the homepage. Subscription form is visible.",
                "1. Scroll to subscription section.\n2. Leave email field empty.\n3. Click the 'Subscribe' button.\n4. Verify no success message appears.",
                "email: ''",
                "Empty email subscription does not proceed.",
                "Edge", "Medium", true));

        // ── TC_009: Subscribe with invalid email format ───────────────────────
        list.add(build("TC_009", "Verify subscription with invalid email format shows error",
                "Entering a malformed email address in the subscription field should show a validation error.",
                "Browser is open on the homepage. Subscription form is visible.",
                "1. Scroll to subscription section.\n2. Enter 'not-an-email' in the email field.\n3. Click the 'Subscribe' button.\n4. Verify browser validation prevents submission.",
                "email: not-an-email",
                "Invalid email format is rejected.",
                "Negative", "Low", true));

        // ── TC_010: WOMEN category link ───────────────────────────────────────
        list.add(build("TC_010", "Verify WOMEN category link expands subcategories",
                "Clicking on WOMEN category should expand its subcategories.",
                "Browser is open on the homepage. Left sidebar with categories is visible.",
                "1. Locate the 'WOMEN' category link.\n2. Click on 'WOMEN'.\n3. Verify subcategory links are displayed.",
                "N/A",
                "WOMEN category expands with subcategories.",
                "Positive", "Medium", true));

        // ── TC_011: MEN category link ─────────────────────────────────────────
        list.add(build("TC_011", "Verify MEN category link expands subcategories",
                "Clicking on MEN category should expand its subcategories.",
                "Browser is open on the homepage. Category sidebar is visible.",
                "1. Locate the 'MEN' category link.\n2. Click on 'MEN'.\n3. Verify subcategory links are displayed.",
                "N/A",
                "MEN category expands with subcategories.",
                "Positive", "Medium", true));

        // ── TC_012: KIDS category link ────────────────────────────────────────
        list.add(build("TC_012", "Verify KIDS category link expands subcategories",
                "Clicking on KIDS category should expand its subcategories.",
                "Browser is open on the homepage. Category sidebar is visible.",
                "1. Locate the 'KIDS' category link.\n2. Click on 'KIDS'.\n3. Verify subcategory links are displayed.",
                "N/A",
                "KIDS category expands with subcategories.",
                "Positive", "Medium", true));

        // ── TC_013: Brand link navigation ─────────────────────────────────────
        list.add(build("TC_013", "Verify brand link navigates to brand products page",
                "Clicking on a brand link should navigate to the brand's product listing.",
                "Browser is open on the homepage. Brands section is visible.",
                "1. Scroll to Brands section.\n2. Click on 'POLO' brand link.\n3. Verify URL contains '/brand_products/'.",
                "N/A",
                "Brand link navigates to correct brand products page.",
                "Positive", "Medium", true));

        // ── TC_014: View Product navigation ──────────────────────────────────
        list.add(build("TC_014", "Verify View Product link opens product details page",
                "Clicking 'View Product' should navigate to the product details page.",
                "Browser is open on the homepage. Featured products section is visible.",
                "1. Locate the featured products section.\n2. Click the first 'View Product' link.\n3. Verify URL contains '/product_details/'.",
                "N/A",
                "Product details page opens correctly.",
                "Positive", "High", true));

        // ── TC_015: Scroll Up button ──────────────────────────────────────────
        list.add(build("TC_015", "Verify Scroll Up button scrolls to top of page",
                "The 'Scroll Up' button should scroll the page back to the top.",
                "Browser is open on the homepage. Page is scrolled to the bottom.",
                "1. Scroll to bottom using JavaScript.\n2. Click the 'Scroll Up' button.\n3. Verify page scrolls back to top.",
                "N/A",
                "Scroll Up button scrolls page to top.",
                "Positive", "Low", true));

        // ── TC_016: Verify all navigation links are visible ──────────────────
        list.add(build("TC_016", "Verify all main navigation links are visible on homepage",
                "All navigation bar links should be visible on the homepage.",
                "Browser is open on the homepage.",
                "1. Check visibility of 'Home' link.\n2. Check visibility of 'Products' link.\n3. Check visibility of 'Cart' link.\n4. Check visibility of 'Signup / Login' link.\n5. Check visibility of 'Test Cases' link.\n6. Check visibility of 'Contact us' link.",
                "N/A",
                "All navigation links are visible.",
                "Positive", "High", true));

        // ── TC_017: Featured products section is visible ──────────────────────
        list.add(build("TC_017", "Verify featured products section displays products",
                "The featured products section should display product cards.",
                "Browser is open on the homepage.",
                "1. Scroll to featured products section.\n2. Verify at least one product card is visible.\n3. Verify each card has a 'View Product' link.",
                "N/A",
                "Featured products are displayed correctly.",
                "Positive", "High", true));

        // ── TC_018: Subscription section is visible ───────────────────────────
        list.add(build("TC_018", "Verify subscription section is visible in footer",
                "The email subscription section should be visible in the footer.",
                "Browser is open on the homepage.",
                "1. Scroll to footer section.\n2. Verify subscription heading is displayed.\n3. Verify email input field is displayed.\n4. Verify subscribe button is displayed.",
                "N/A",
                "Subscription section is present in the footer.",
                "Positive", "Medium", true));

        // ── TC_019: Subscription input accessibility ─────────────────────────
        list.add(build("TC_019", "Verify email subscription input has placeholder text",
                "The email input should have placeholder text to guide users.",
                "Browser is open on the homepage. Subscription section is visible.",
                "1. Scroll to subscription section.\n2. Get placeholder attribute of email input.\n3. Verify placeholder is not empty.",
                "N/A",
                "Email input displays placeholder text for accessibility.",
                "Accessibility", "Low", true));

        // ── TC_020: Category links visible on sidebar ─────────────────────────
        list.add(build("TC_020", "Verify WOMEN, MEN, KIDS category links are present",
                "The left sidebar should display WOMEN, MEN, and KIDS category links.",
                "Browser is open on the homepage.",
                "1. Locate the left sidebar.\n2. Verify 'WOMEN' link is present.\n3. Verify 'MEN' link is present.\n4. Verify 'KIDS' link is present.",
                "N/A",
                "All three category links are present.",
                "Positive", "Medium", true));

        return list;
    }

    private static TestCase build(String id, String name, String desc, String precond,
                                   String steps, String testData, String expected, String type,
                                   String priority, boolean autoFeasible) {
        TestCase tc = new TestCase();
        tc.setTestCaseId(id);
        tc.setTestCaseName(name);
        tc.setDescription(desc);
        tc.setPreconditions(precond);
        tc.setTestSteps(steps);
        tc.setTestData(testData);
        tc.setExpectedResult(expected);
        tc.setTestType(type);
        tc.setPriority(priority);
        tc.setComponent("Home Page");
        tc.setAutomationFeasible(autoFeasible);
        return tc;
    }
}
