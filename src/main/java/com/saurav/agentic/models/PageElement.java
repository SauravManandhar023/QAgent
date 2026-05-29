package com.saurav.agentic.models;

import java.util.HashMap;
import java.util.Map;

/**
 * PageElement - Represents a single DOM element extracted from the page
 * Mirrors what you see when inspecting an element in browser DevTools
 */
public class PageElement {

    // ── Core identity ─────────────────────────────────────────────────────────
    private String tag;           // input, button, select, a, form, div, etc.
    private String id;            // id attribute
    private String name;          // name attribute
    private String type;          // type attribute (text, password, submit, checkbox)
    private String className;     // class attribute

    // ── Content ───────────────────────────────────────────────────────────────
    private String text;          // visible text content
    private String placeholder;   // placeholder attribute
    private String value;         // value attribute
    private String href;          // href for links
    private String src;           // src for images
    private String alt;           // alt for images
    private String ariaLabel;     // aria-label attribute
    private String dataTestId;    // data-testid attribute
    private String forAttr;       // for attribute on labels

    // ── Form context ──────────────────────────────────────────────────────────
    private String formId;        // parent form id if inside a form
    private String action;        // form action URL
    private String method;        // form method (get/post)

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean isChecked;    // for checkboxes/radio buttons
    private boolean isSelected;   // for select options
    private boolean isDisabled;   // disabled attribute
    private boolean isRequired;   // required attribute
    private boolean isVisible;    // visible on screen
    private boolean isInteractable; // can be clicked/typed

    // ── Locator recommendations ───────────────────────────────────────────────
    private String bestLocator;   // best locator strategy
    private String bestLocatorValue; // value for best locator
    private String cssSelector;   // generated CSS selector
    private String xpath;         // generated XPath

    // ── Extra attributes ──────────────────────────────────────────────────────
    private Map<String, String> extraAttributes = new HashMap<>();

    // ── Target behavior ───────────────────────────────────────────────────────
    private String target;        // target attribute (_blank, _self, etc.)

    public PageElement() {}

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getTag()              { return tag; }
    public String getId()               { return id; }
    public String getName()             { return name; }
    public String getType()             { return type; }
    public String getClassName()        { return className; }
    public String getText()             { return text; }
    public String getPlaceholder()      { return placeholder; }
    public String getValue()            { return value; }
    public String getHref()             { return href; }
    public String getSrc()              { return src; }
    public String getAlt()              { return alt; }
    public String getAriaLabel()        { return ariaLabel; }
    public String getDataTestId()       { return dataTestId; }
    public String getForAttr()          { return forAttr; }
    public String getFormId()           { return formId; }
    public String getAction()           { return action; }
    public String getMethod()           { return method; }
    public boolean isChecked()          { return isChecked; }
    public boolean isSelected()         { return isSelected; }
    public boolean isDisabled()         { return isDisabled; }
    public boolean isRequired()         { return isRequired; }
    public boolean isVisible()          { return isVisible; }
    public boolean isInteractable()     { return isInteractable; }
    public String getBestLocator()      { return bestLocator; }
    public String getBestLocatorValue() { return bestLocatorValue; }
    public String getCssSelector()      { return cssSelector; }
    public String getXpath()            { return xpath; }
    public String getTarget()           { return target; }
    public Map<String, String> getExtraAttributes() { return extraAttributes; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setTag(String tag)                        { this.tag = tag; }
    public void setId(String id)                          { this.id = id; }
    public void setName(String name)                      { this.name = name; }
    public void setType(String type)                      { this.type = type; }
    public void setClassName(String className)            { this.className = className; }
    public void setText(String text)                      { this.text = text; }
    public void setPlaceholder(String placeholder)        { this.placeholder = placeholder; }
    public void setValue(String value)                    { this.value = value; }
    public void setHref(String href)                      { this.href = href; }
    public void setSrc(String src)                        { this.src = src; }
    public void setAlt(String alt)                        { this.alt = alt; }
    public void setAriaLabel(String ariaLabel)            { this.ariaLabel = ariaLabel; }
    public void setDataTestId(String dataTestId)          { this.dataTestId = dataTestId; }
    public void setForAttr(String forAttr)                { this.forAttr = forAttr; }
    public void setFormId(String formId)                  { this.formId = formId; }
    public void setAction(String action)                  { this.action = action; }
    public void setMethod(String method)                  { this.method = method; }
    public void setChecked(boolean checked)               { this.isChecked = checked; }
    public void setSelected(boolean selected)             { this.isSelected = selected; }
    public void setDisabled(boolean disabled)             { this.isDisabled = disabled; }
    public void setRequired(boolean required)             { this.isRequired = required; }
    public void setVisible(boolean visible)               { this.isVisible = visible; }
    public void setInteractable(boolean interactable)     { this.isInteractable = interactable; }
    public void setBestLocator(String bestLocator)        { this.bestLocator = bestLocator; }
    public void setBestLocatorValue(String v)             { this.bestLocatorValue = v; }
    public void setCssSelector(String cssSelector)        { this.cssSelector = cssSelector; }
    public void setXpath(String xpath)                    { this.xpath = xpath; }
    public void setTarget(String target)                  { this.target = target; }
    public void addExtraAttribute(String key, String val) { this.extraAttributes.put(key, val); }

    @Override
    public String toString() {
        return "<" + tag + ">"
                + (id != null ? " id=" + id : "")
                + (name != null ? " name=" + name : "")
                + (type != null ? " type=" + type : "")
                + (text != null && !text.isEmpty() ? " text=" + text : "")
                + " [" + bestLocator + "=" + bestLocatorValue + "]";
    }
}