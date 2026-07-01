package com.saurav.agentic.tools;

import com.saurav.agentic.models.TestCase;
import com.saurav.agentic.utils.ExcelUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Senior QA Engineer: curates AI-generated test cases for automationexercise.com
 * Removes poor/overly-specific cases, adds missing critical coverage.
 */
public class CurateTestCases {
    public static void main(String[] args) throws Exception {
        String excelPath = "test-output/excel/ui-test-cases.xlsx";

        List<TestCase> curated = buildCuratedTestCases();

        ExcelUtil.writeTestCases(curated, excelPath);

        System.out.println("\n=== CURATION COMPLETE ===");
        System.out.println("Total: " + curated.size() + " test cases");
        System.out.println();
        Map<String, Long> byType = new LinkedHashMap<>();
        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (TestCase tc : curated) {
            byType.merge(tc.getTestType(), 1L, Long::sum);
            byPriority.merge(tc.getPriority(), 1L, Long::sum);
        }
        System.out.println("By Type:");
        byType.forEach((t, c) -> System.out.println("  " + t + ": " + c));
        System.out.println("By Priority:");
        byPriority.forEach((p, c) -> System.out.println("  " + p + ": " + c));
    }

    private static List<TestCase> buildCuratedTestCases() {
        List<TestCase> list = new ArrayList<>();
        int id = 1;

        // ═══════════════════════════════════════════════════════════════════
        // HEADER NAVIGATION (keep from AI generation, improved)
        // ═══════════════════════════════════════════════════════════════════

        list.add(make(id++, "Verify navigation to Home page via header link",
            "Verify clicking 'Home' in header navigates to homepage",
            "Browser is on automationexercise.com homepage",
            "1. Click 'Home' link in header navigation\n2. Wait for page to load\n3. Verify URL contains '/'",
            "url: https://automationexercise.com/",
            "URL is '/' and homepage is displayed with carousel and featured products",
            "Positive", "High", "Header Navigation", true));

        list.add(make(id++, "Verify navigation to Products page via header",
            "Verify clicking 'Products' navigates to /products",
            "Browser is on automationexercise.com homepage",
            "1. Click 'Products' link in header\n2. Wait for page to load\n3. Verify URL contains '/products'",
            "url: https://automationexercise.com/products",
            "URL contains '/products' and all products grid is displayed",
            "Positive", "High", "Header Navigation", true));

        list.add(make(id++, "Verify navigation to Cart page via header",
            "Verify clicking 'Cart' navigates to /view_cart",
            "Browser is on automationexercise.com homepage",
            "1. Click 'Cart' link in header\n2. Wait for page to load\n3. Verify URL contains '/view_cart'",
            "url: https://automationexercise.com/view_cart",
            "URL contains '/view_cart' and empty cart message or items are displayed",
            "Positive", "High", "Header Navigation", true));

        list.add(make(id++, "Verify navigation to Signup/Login page via header",
            "Verify clicking 'Signup / Login' navigates to /login",
            "Browser is on automationexercise.com homepage",
            "1. Click 'Signup / Login' link in header\n2. Wait for page to load\n3. Verify URL contains '/login'",
            "url: https://automationexercise.com/login",
            "URL contains '/login' and login/signup form is displayed",
            "Positive", "High", "Header Navigation", true));

        list.add(make(id++, "Verify navigation to Test Cases page via header",
            "Verify clicking 'Test Cases' navigates to /test_cases",
            "Browser is on automationexercise.com homepage",
            "1. Click 'Test Cases' link in header\n2. Wait for page to load\n3. Verify URL contains '/test_cases'",
            "url: https://automationexercise.com/test_cases",
            "URL contains '/test_cases' and test cases list is displayed",
            "Positive", "High", "Header Navigation", true));

        list.add(make(id++, "Verify navigation to API Testing page via header",
            "Verify clicking 'API Testing' navigates to /api_list",
            "Browser is on automationexercise.com homepage",
            "1. Click 'API Testing' link in header\n2. Wait for page to load\n3. Verify URL contains '/api_list'",
            "url: https://automationexercise.com/api_list",
            "URL contains '/api_list' and API list page is displayed",
            "Positive", "Medium", "Header Navigation", true));

        list.add(make(id++, "Verify navigation to Contact Us page via header",
            "Verify clicking 'Contact Us' navigates to /contact_us",
            "Browser is on automationexercise.com homepage",
            "1. Click 'Contact Us' link in header\n2. Wait for page to load\n3. Verify URL contains '/contact_us'",
            "url: https://automationexercise.com/contact_us",
            "URL contains '/contact_us' and contact form is displayed",
            "Positive", "High", "Header Navigation", true));

        // ═══════════════════════════════════════════════════════════════════
        // SEARCH
        // ═══════════════════════════════════════════════════════════════════

        list.add(make(id++, "Verify product search returns results for valid keyword",
            "Verify searching 'blue top' shows matching products",
            "Browser is on automationexercise.com homepage or products page",
            "1. Locate search bar on products page\n2. Enter 'blue top' as search keyword\n3. Click search button\n4. Verify search results are displayed",
            "search: 'blue top'",
            "Search results show products matching 'blue top' and 'Searched Products' heading is visible",
            "Positive", "High", "Search", true));

        list.add(make(id++, "Verify product search with invalid keyword shows empty results",
            "Verify searching a non-existent product displays 'no results' message",
            "Browser is on products page",
            "1. Locate search bar\n2. Enter 'zzz_nonexistent_999' as search keyword\n3. Click search button\n4. Verify results area shows 0 products or appropriate empty state",
            "search: 'zzz_nonexistent_999'",
            "No products are displayed for the invalid search keyword",
            "Negative", "Medium", "Search", true));

        // ═══════════════════════════════════════════════════════════════════
        // PRODUCT INTERACTION
        // ═══════════════════════════════════════════════════════════════════

        list.add(make(id++, "Verify adding a product to cart shows success confirmation",
            "Verify clicking 'Add to cart' on a featured product shows modal with 'Your product has been added'",
            "Browser is on homepage with featured products visible",
            "1. Hover over first featured product\n2. Click 'Add to cart'\n3. Wait for confirmation modal\n4. Verify modal displays 'Added!' or 'Your product has been added to cart'",
            "product: first featured product on homepage",
            "Success modal appears with confirmation message and 'View Cart' and 'Continue Shopping' buttons",
            "Positive", "High", "Product Interaction", true));

        list.add(make(id++, "Verify cart count badge updates after adding product",
            "Verify the cart count in header increments after adding a product",
            "Browser is on homepage. Cart count starts at 0.",
            "1. Note current cart count in header\n2. Add first featured product to cart\n3. Wait for confirmation modal\n4. Close modal via 'Continue Shopping'\n5. Verify header cart count has incremented by 1",
            "product: first featured product",
            "Cart count badge in header shows incremented value (e.g., 0 -> 1)",
            "Positive", "High", "Product Interaction", true));

        list.add(make(id++, "Verify 'View Cart' link in modal navigates to cart page",
            "Verify clicking 'View Cart' in add-to-cart modal navigates to cart",
            "Browser is on homepage. Add-to-cart confirmation modal is visible.",
            "1. Add a product to cart\n2. Wait for confirmation modal\n3. Click 'View Cart' in modal\n4. Verify URL contains '/view_cart' and the added product row is visible",
            "product: first featured product",
            "Cart page shows the added product with correct name, price, and quantity",
            "Positive", "High", "Product Interaction", true));

        list.add(make(id++, "Verify viewing product details page",
            "Verify clicking 'View Product' on a product opens details page with correct info",
            "Browser is on homepage or products page",
            "1. Locate a featured product\n2. Click 'View Product' link\n3. Wait for product details page\n4. Verify product name, category, price, availability, and description are displayed",
            "product: first featured product on homepage",
            "Product details page shows product name, category, price, availability, brand, and description",
            "Positive", "High", "Product Interaction", true));

        // ═══════════════════════════════════════════════════════════════════
        // CATEGORY / BRAND SIDEBAR
        // ═══════════════════════════════════════════════════════════════════

        list.add(make(id++, "Verify category sidebar is visible on homepage",
            "Verify the product categories section is displayed on the homepage",
            "Browser is on automationexercise.com homepage",
            "1. Scroll to left sidebar area\n2. Verify 'Category' heading is visible\n3. Verify at least one category (Women, Men, Kids) is displayed",
            "",
            "Category sidebar with at least 'Women', 'Men', 'Kids' categories is visible on left side",
            "Positive", "Low", "Sidebar", true));

        list.add(make(id++, "Verify clicking a category filters products",
            "Verify clicking 'Women -> Dress' category navigates to filtered products",
            "Browser is on automationexercise.com homepage",
            "1. Locate category sidebar\n2. Click on 'Women' category to expand\n3. Click on 'Dress' subcategory\n4. Verify URL contains '/category_products/' and filtered products are displayed",
            "category: Women -> Dress",
            "URL contains '/category_products/' and products matching the Dress category are displayed",
            "Positive", "High", "Sidebar", true));

        list.add(make(id++, "Verify brands sidebar is visible and clickable",
            "Verify brands list is displayed and clicking a brand filters products",
            "Browser is on automationexercise.com homepage or products page",
            "1. Scroll to brands section in sidebar\n2. Verify 'Brands' heading is visible\n3. Click on first brand name (e.g., 'Polo')\n4. Verify URL contains '/brand_products/' and filtered products are displayed",
            "brand: first listed brand",
            "URL contains '/brand_products/' and products filtered by selected brand are displayed",
            "Positive", "High", "Sidebar", true));

        // ═══════════════════════════════════════════════════════════════════
        // SUBSCRIPTION
        // ═══════════════════════════════════════════════════════════════════

        list.add(make(id++, "Verify footer subscription with valid email shows success",
            "Verify entering a valid email in footer subscription shows 'You have been successfully subscribed!'",
            "Browser is on automationexercise.com homepage. Footer is visible.",
            "1. Scroll to footer section\n2. Locate subscription email input\n3. Enter 'test@example.com'\n4. Click subscribe/submit arrow\n5. Verify success alert message appears",
            "email: 'test@example.com'",
            "Success message 'You have been successfully subscribed!' is displayed in green alert",
            "Positive", "Medium", "Subscription", true));

        list.add(make(id++, "Verify footer subscription with empty email does not submit",
            "Verify clicking subscribe with empty email does not show success",
            "Browser is on automationexercise.com homepage. Footer is visible.",
            "1. Scroll to footer\n2. Locate subscription email input (leave empty)\n3. Click subscribe button\n4. Verify no success alert appears",
            "email: '' (empty)",
            "No success alert message appears - form validation prevents empty submission",
            "Negative", "Low", "Subscription", true));

        // ═══════════════════════════════════════════════════════════════════
        // HOMEPAGE FEATURES
        // ═══════════════════════════════════════════════════════════════════

        list.add(make(id++, "Verify homepage carousel/slider is displayed",
            "Verify the featured items carousel/slider is present and functioning on homepage",
            "Browser is on automationexercise.com homepage",
            "1. Observe the top banner/carousel area\n2. Verify carousel indicators or navigation arrows are visible\n3. Verify carousel images are loaded",
            "",
            "Carousel with navigation indicators and images is visible at top of homepage",
            "Positive", "Low", "Homepage", true));

        list.add(make(id++, "Verify featured products section is displayed on homepage",
            "Verify the 'Features Items' section shows products on the homepage",
            "Browser is on automationexercise.com homepage",
            "1. Scroll to featured products section\n2. Verify 'Features Items' heading is visible\n3. Verify at least one product card is displayed with name and price",
            "",
            "'Features Items' section is visible with product cards showing thumbnail, name, and price",
            "Positive", "Medium", "Homepage", true));

        // ═══════════════════════════════════════════════════════════════════
        // ACCESSIBILITY
        // ═══════════════════════════════════════════════════════════════════

        list.add(make(id++, "Verify all product images have non-empty alt text",
            "Verify featured product images have alt attributes for accessibility",
            "Browser is on automationexercise.com homepage",
            "1. Locate all product images in featured items section\n2. For each image, check the 'alt' attribute is present and non-empty\n3. Log any images missing alt text",
            "",
            "All product images in the featured products section have alt attributes with descriptive text",
            "Accessibility", "Medium", "Homepage", false));

        System.out.println("Total curated test cases: " + (id - 1));
        return list;
    }

    private static TestCase make(int id, String name, String desc, String preconditions,
                                  String steps, String data, String expected,
                                  String type, String priority, String component,
                                  boolean automatable) {
        TestCase tc = new TestCase();
        tc.setTestCaseId("TC_" + String.format("%03d", id));
        tc.setTestCaseName(name);
        tc.setDescription(desc);
        tc.setPreconditions(preconditions);
        tc.setTestSteps(steps);
        tc.setTestData(data);
        tc.setExpectedResult(expected);
        tc.setTestType(type);
        tc.setPriority(priority);
        tc.setComponent(component);
        tc.setAutomationFeasible(automatable);
        return tc;
    }
}
