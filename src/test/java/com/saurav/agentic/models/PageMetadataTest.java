package com.saurav.agentic.models;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for PageMetadata model — structured page representation.
 */
public class PageMetadataTest {

    private PageMetadata meta;
    private PageElement input;
    private PageElement button;
    private PageElement link;
    private PageElement checkbox;
    private PageElement dropdown;
    private PageElement form;
    private PageElement image;

    @BeforeMethod
    public void setUp() {
        meta = new PageMetadata();

        input = new PageElement();
        input.setTag("input");
        input.setId("username");
        input.setBestLocator("id");
        input.setBestLocatorValue("username");

        button = new PageElement();
        button.setTag("button");
        button.setId("submit");
        button.setText("Login");

        link = new PageElement();
        link.setTag("a");
        link.setText("Forgot password");
        link.setHref("/forgot-password");

        checkbox = new PageElement();
        checkbox.setTag("input");
        checkbox.setType("checkbox");
        checkbox.setName("remember");
        checkbox.setChecked(false);

        dropdown = new PageElement();
        dropdown.setTag("select");
        dropdown.setId("country");
        dropdown.setBestLocator("id");
        dropdown.setBestLocatorValue("country");

        form = new PageElement();
        form.setTag("form");
        form.setId("login-form");
        form.setAction("/login");
        form.setMethod("post");

        image = new PageElement();
        image.setTag("img");
        image.setSrc("/logo.png");
        image.setAlt("Logo");
    }

    @Test
    public void testDefaultState() {
        assertEquals(meta.getUrl(), null);
        assertEquals(meta.getPageTitle(), null);
        assertEquals(meta.getPageType(), null);
        assertTrue(meta.getAllElements().isEmpty());
        assertTrue(meta.getInputs().isEmpty());
        assertTrue(meta.getButtons().isEmpty());
        assertFalse(meta.isHasForm());
        assertFalse(meta.isRequiresAuth());
    }

    @Test
    public void testBasicProperties() {
        meta.setUrl("https://example.com/login");
        meta.setPageTitle("Login Page");
        meta.setPageType("form");

        assertEquals(meta.getUrl(), "https://example.com/login");
        assertEquals(meta.getPageTitle(), "Login Page");
        assertEquals(meta.getPageType(), "form");
    }

    @Test
    public void testAddInput() {
        meta.addInput(input);
        assertEquals(meta.getInputs().size(), 1);
        assertEquals(meta.getAllElements().size(), 1);
        assertEquals(meta.getInputs().get(0).getId(), "username");
    }

    @Test
    public void testAddButton() {
        meta.addButton(button);
        assertEquals(meta.getButtons().size(), 1);
        assertEquals(meta.getButtons().get(0).getText(), "Login");
    }

    @Test
    public void testAddLink() {
        meta.addLink(link);
        assertEquals(meta.getLinks().size(), 1);
        assertEquals(meta.getLinks().get(0).getHref(), "/forgot-password");
    }

    @Test
    public void testAddCheckbox() {
        meta.addCheckbox(checkbox);
        assertEquals(meta.getCheckboxes().size(), 1);
        assertFalse(meta.getCheckboxes().get(0).isChecked());
    }

    @Test
    public void testAddDropdown() {
        meta.addDropdown(dropdown);
        assertEquals(meta.getDropdowns().size(), 1);
        assertEquals(meta.getDropdowns().get(0).getId(), "country");
    }

    @Test
    public void testAddImage() {
        meta.addImage(image);
        assertEquals(meta.getImages().size(), 1);
        assertEquals(meta.getImages().get(0).getAlt(), "Logo");
    }

    @Test
    public void testAddForm() {
        meta.addForm(form);
        assertEquals(meta.getForms().size(), 1);
        assertEquals(meta.getForms().get(0).getAction(), "/login");
    }

    @Test
    public void testAllElementsTracksAllAdds() {
        meta.addInput(input);
        meta.addButton(button);
        meta.addLink(link);
        meta.addCheckbox(checkbox);
        meta.addDropdown(dropdown);
        meta.addImage(image);

        assertEquals(meta.getAllElements().size(), 6);
        assertEquals(meta.getInputs().size(), 1);
        assertEquals(meta.getButtons().size(), 1);
        assertEquals(meta.getLinks().size(), 1);
        assertEquals(meta.getCheckboxes().size(), 1);
        assertEquals(meta.getDropdowns().size(), 1);
        assertEquals(meta.getImages().size(), 1);
    }

    @Test
    public void testPageBehaviorHints() {
        meta.setSuccessCondition("URL changes to /dashboard");
        meta.setFailureCondition("Error message displayed");
        meta.setPrimaryAction("Submit login form");
        meta.setHasForm(true);
        meta.setHasNavigation(false);
        meta.setHasLinks(true);
        meta.setRequiresAuth(false);

        assertEquals(meta.getSuccessCondition(), "URL changes to /dashboard");
        assertEquals(meta.getFailureCondition(), "Error message displayed");
        assertEquals(meta.getPrimaryAction(), "Submit login form");
        assertTrue(meta.isHasForm());
        assertFalse(meta.isHasNavigation());
        assertTrue(meta.isHasLinks());
        assertFalse(meta.isRequiresAuth());
    }

    @Test
    public void testRawData() {
        meta.setRawHtml("<html><body>test</body></html>");
        meta.setPlainTextSummary("A login page with username and password fields");

        assertEquals(meta.getRawHtml(), "<html><body>test</body></html>");
        assertEquals(meta.getPlainTextSummary(), "A login page with username and password fields");
    }

    @Test
    public void testToPromptStringWithForm() {
        meta.setUrl("https://example.com/login");
        meta.setPageTitle("Login Page");
        meta.setPageType("form");

        meta.addInput(input);
        meta.addButton(button);
        meta.addForm(form);

        meta.setSuccessCondition("Redirect to dashboard");
        meta.setFailureCondition("Show error message");
        meta.setPrimaryAction("Click Login button");

        String prompt = meta.toPromptString();

        assertTrue(prompt.contains("Login Page"));
        assertTrue(prompt.contains("https://example.com/login"));
        assertTrue(prompt.contains("form"));
        assertTrue(prompt.contains("FORMS:"));
        assertTrue(prompt.contains("INPUTS:"));
        assertTrue(prompt.contains("BUTTONS:"));
        assertTrue(prompt.contains("SUCCESS CONDITION: Redirect to dashboard"));
        assertTrue(prompt.contains("FAILURE CONDITION: Show error message"));
        assertTrue(prompt.contains("PRIMARY ACTION: Click Login button"));
    }

    @Test
    public void testToPromptStringWithCheckbox() {
        meta.addCheckbox(checkbox);

        String prompt = meta.toPromptString();
        assertTrue(prompt.contains("CHECKBOXES:"));
        assertTrue(prompt.contains("UNCHECKED"));
    }

    @Test
    public void testToPromptStringWithDropdown() {
        dropdown.setBestLocator("id");
        dropdown.setBestLocatorValue("country");
        meta.addDropdown(dropdown);

        String prompt = meta.toPromptString();
        assertTrue(prompt.contains("DROPDOWNS:"));
        assertTrue(prompt.contains("country"));
    }

    @Test
    public void testToPromptStringWithLink() {
        link.setTarget("_blank");
        meta.addLink(link);

        String prompt = meta.toPromptString();
        assertTrue(prompt.contains("LINKS:"));
        assertTrue(prompt.contains("NEW TAB"));
    }

    @Test
    public void testToPromptStringEmpty() {
        // No elements added — should still return non-empty with just page info
        String prompt = meta.toPromptString();
        assertNotNull(prompt);
        assertTrue(prompt.contains("PAGE:"));
    }

    @Test
    public void testToPromptStringWithImage() {
        meta.addImage(image);

        String prompt = meta.toPromptString();
        assertNotNull(prompt);
        // Note: toPromptString does not include an IMAGES section;
        // images are captured in getAllElements() but not rendered in the prompt text
        assertFalse(prompt.contains("IMAGES:"));
    }

    @Test
    public void testNoSideEffectsBetweenAdds() {
        meta.addInput(input);
        meta.addButton(button);

        assertEquals(meta.getInputs().size(), 1);
        assertEquals(meta.getButtons().size(), 1);
        assertEquals(meta.getLinks().size(), 0);
        assertEquals(meta.getCheckboxes().size(), 0);
        assertEquals(meta.getDropdowns().size(), 0);
        assertEquals(meta.getImages().size(), 0);
        assertEquals(meta.getForms().size(), 0);
    }
}
