package com.saurav.agentic.models;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for PageElement model — DOM element representation.
 */
public class PageElementTest {

    private PageElement el;

    @BeforeMethod
    public void setUp() {
        el = new PageElement();
    }

    @Test
    public void testDefaultValues() {
        assertNull(el.getTag());
        assertNull(el.getId());
        assertNull(el.getName());
        assertNull(el.getType());
        assertNull(el.getText());
        assertNull(el.getBestLocator());
        assertNull(el.getBestLocatorValue());
        assertFalse(el.isChecked());
        assertFalse(el.isDisabled());
        assertFalse(el.isRequired());
        assertFalse(el.isSelected());
        assertTrue(el.getExtraAttributes().isEmpty());
    }

    @Test
    public void testCoreIdentity() {
        el.setTag("input");
        el.setId("username");
        el.setName("username");
        el.setType("text");
        el.setClassName("form-control");

        assertEquals(el.getTag(), "input");
        assertEquals(el.getId(), "username");
        assertEquals(el.getName(), "username");
        assertEquals(el.getType(), "text");
        assertEquals(el.getClassName(), "form-control");
    }

    @Test
    public void testContentFields() {
        el.setText("Login");
        el.setPlaceholder("Enter username");
        el.setValue("admin");
        el.setHref("/dashboard");
        el.setSrc("/img/logo.png");
        el.setAlt("Company Logo");
        el.setAriaLabel("Close dialog");
        el.setDataTestId("btn-submit");
        el.setForAttr("email-input");

        assertEquals(el.getText(), "Login");
        assertEquals(el.getPlaceholder(), "Enter username");
        assertEquals(el.getValue(), "admin");
        assertEquals(el.getHref(), "/dashboard");
        assertEquals(el.getSrc(), "/img/logo.png");
        assertEquals(el.getAlt(), "Company Logo");
        assertEquals(el.getAriaLabel(), "Close dialog");
        assertEquals(el.getDataTestId(), "btn-submit");
        assertEquals(el.getForAttr(), "email-input");
    }

    @Test
    public void testFormContext() {
        el.setFormId("login-form");
        el.setAction("/api/login");
        el.setMethod("post");

        assertEquals(el.getFormId(), "login-form");
        assertEquals(el.getAction(), "/api/login");
        assertEquals(el.getMethod(), "post");
    }

    @Test
    public void testBooleanStates() {
        el.setChecked(true);
        el.setSelected(true);
        el.setDisabled(true);
        el.setRequired(true);
        el.setVisible(true);
        el.setInteractable(true);

        assertTrue(el.isChecked());
        assertTrue(el.isSelected());
        assertTrue(el.isDisabled());
        assertTrue(el.isRequired());
        assertTrue(el.isVisible());
        assertTrue(el.isInteractable());
    }

    @Test
    public void testBooleanStatesResetToFalse() {
        el.setChecked(true);
        el.setChecked(false);
        assertFalse(el.isChecked());
    }

    @Test
    public void testLocatorRecommendations() {
        el.setBestLocator("id");
        el.setBestLocatorValue("username");
        el.setCssSelector("#username");
        el.setXpath("//input[@id='username']");

        assertEquals(el.getBestLocator(), "id");
        assertEquals(el.getBestLocatorValue(), "username");
        assertEquals(el.getCssSelector(), "#username");
        assertEquals(el.getXpath(), "//input[@id='username']");
    }

    @Test
    public void testExtraAttributes() {
        el.addExtraAttribute("data-cy", "user-input");
        el.addExtraAttribute("role", "textbox");

        assertEquals(el.getExtraAttributes().size(), 2);
        assertEquals(el.getExtraAttributes().get("data-cy"), "user-input");
        assertEquals(el.getExtraAttributes().get("role"), "textbox");
    }

    @Test
    public void testTargetAttribute() {
        el.setTarget("_blank");
        assertEquals(el.getTarget(), "_blank");

        el.setTarget("_self");
        assertEquals(el.getTarget(), "_self");
    }

    @Test
    public void testToStringInput() {
        el.setTag("input");
        el.setId("email");
        el.setName("email");
        el.setType("email");
        el.setBestLocator("id");
        el.setBestLocatorValue("email");

        String str = el.toString();
        assertTrue(str.contains("<input>"));
        assertTrue(str.contains("id=email"));
        assertTrue(str.contains("name=email"));
        assertTrue(str.contains("type=email"));
        assertTrue(str.contains("id=email"));
    }

    @Test
    public void testToStringLink() {
        el.setTag("a");
        el.setText("Click Here");
        el.setBestLocator("linkText");
        el.setBestLocatorValue("Click Here");

        String str = el.toString();
        assertTrue(str.contains("<a>"));
        assertTrue(str.contains("text=Click Here"));
        assertTrue(str.contains("linkText=Click Here"));
    }

    @Test
    public void testToStringWithNullBestLocator() {
        el.setTag("div");
        el.setText("Content");
        // Don't set bestLocator — should not NPE
        String str = el.toString();
        assertTrue(str.contains("<div>"));
        assertTrue(str.contains("text=Content"));
    }

    @Test
    public void testMultipleExtraAttributes() {
        el.addExtraAttribute("a", "1");
        el.addExtraAttribute("b", "2");
        el.addExtraAttribute("c", "3");
        assertEquals(el.getExtraAttributes().size(), 3);
    }

    @Test
    public void testOverrideExtraAttribute() {
        el.addExtraAttribute("key", "value1");
        el.addExtraAttribute("key", "value2");
        assertEquals(el.getExtraAttributes().get("key"), "value2");
    }
}
