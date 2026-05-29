package com.saurav.agentic.models;

import java.util.ArrayList;
import java.util.List;

/**
 * PageMetadata - Full structured representation of a scraped page
 * Contains all elements, forms, links, and page behavior hints
 * Replaces plain text page analysis with structured data
 */
public class PageMetadata {

    private String url;
    private String pageTitle;
    private String pageType;        // form, list, dashboard, navigation, mixed

    // ── Element collections ───────────────────────────────────────────────────
    private List<PageElement> inputs      = new ArrayList<>();
    private List<PageElement> buttons     = new ArrayList<>();
    private List<PageElement> links       = new ArrayList<>();
    private List<PageElement> checkboxes  = new ArrayList<>();
    private List<PageElement> dropdowns   = new ArrayList<>();
    private List<PageElement> images      = new ArrayList<>();
    private List<PageElement> forms       = new ArrayList<>();
    private List<PageElement> allElements = new ArrayList<>();

    // ── Page behavior hints ───────────────────────────────────────────────────
    private String successCondition;    // what happens on success
    private String failureCondition;    // what happens on failure
    private String primaryAction;       // main action on this page
    private boolean hasForm;
    private boolean hasNavigation;
    private boolean hasLinks;
    private boolean requiresAuth;

    // ── Raw data ──────────────────────────────────────────────────────────────
    private String rawHtml;             // trimmed HTML for reference
    private String plainTextSummary;    // human readable summary

    public PageMetadata() {}

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getUrl()                      { return url; }
    public String getPageTitle()                { return pageTitle; }
    public String getPageType()                 { return pageType; }
    public List<PageElement> getInputs()        { return inputs; }
    public List<PageElement> getButtons()       { return buttons; }
    public List<PageElement> getLinks()         { return links; }
    public List<PageElement> getCheckboxes()    { return checkboxes; }
    public List<PageElement> getDropdowns()     { return dropdowns; }
    public List<PageElement> getImages()        { return images; }
    public List<PageElement> getForms()         { return forms; }
    public List<PageElement> getAllElements()   { return allElements; }
    public String getSuccessCondition()         { return successCondition; }
    public String getFailureCondition()         { return failureCondition; }
    public String getPrimaryAction()            { return primaryAction; }
    public boolean isHasForm()                  { return hasForm; }
    public boolean isHasNavigation()            { return hasNavigation; }
    public boolean isHasLinks()                 { return hasLinks; }
    public boolean isRequiresAuth()             { return requiresAuth; }
    public String getRawHtml()                  { return rawHtml; }
    public String getPlainTextSummary()         { return plainTextSummary; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setUrl(String url)                          { this.url = url; }
    public void setPageTitle(String pageTitle)              { this.pageTitle = pageTitle; }
    public void setPageType(String pageType)                { this.pageType = pageType; }
    public void setSuccessCondition(String s)               { this.successCondition = s; }
    public void setFailureCondition(String f)               { this.failureCondition = f; }
    public void setPrimaryAction(String primaryAction)      { this.primaryAction = primaryAction; }
    public void setHasForm(boolean hasForm)                 { this.hasForm = hasForm; }
    public void setHasNavigation(boolean hasNavigation)     { this.hasNavigation = hasNavigation; }
    public void setHasLinks(boolean hasLinks)               { this.hasLinks = hasLinks; }
    public void setRequiresAuth(boolean requiresAuth)       { this.requiresAuth = requiresAuth; }
    public void setRawHtml(String rawHtml)                  { this.rawHtml = rawHtml; }
    public void setPlainTextSummary(String s)               { this.plainTextSummary = s; }

    // ── Add elements ──────────────────────────────────────────────────────────
    public void addInput(PageElement e)     { inputs.add(e);      allElements.add(e); }
    public void addButton(PageElement e)    { buttons.add(e);     allElements.add(e); }
    public void addLink(PageElement e)      { links.add(e);       allElements.add(e); }
    public void addCheckbox(PageElement e)  { checkboxes.add(e);  allElements.add(e); }
    public void addDropdown(PageElement e)  { dropdowns.add(e);   allElements.add(e); }
    public void addImage(PageElement e)     { images.add(e);      allElements.add(e); }
    public void addForm(PageElement e)      { forms.add(e);       allElements.add(e); }

    /**
     * Generates a structured text summary for AI prompts
     * Much richer than the old plain text page analysis
     */
    public String toPromptString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PAGE: ").append(pageTitle).append(" (").append(url).append(")\n");
        sb.append("TYPE: ").append(pageType).append("\n\n");

        if (!forms.isEmpty()) {
            sb.append("FORMS:\n");
            for (PageElement f : forms) {
                sb.append("  form id=").append(f.getId())
                  .append(" action=").append(f.getAction())
                  .append(" method=").append(f.getMethod()).append("\n");
            }
            sb.append("\n");
        }

        if (!inputs.isEmpty()) {
            sb.append("INPUTS:\n");
            for (PageElement e : inputs) {
                sb.append("  ").append(e).append("\n");
                sb.append("    best locator: ")
                  .append(e.getBestLocator()).append("=\"")
                  .append(e.getBestLocatorValue()).append("\"\n");
                if (e.getPlaceholder() != null)
                    sb.append("    placeholder: ").append(e.getPlaceholder()).append("\n");
                if (e.isRequired())
                    sb.append("    required: true\n");
            }
            sb.append("\n");
        }

        if (!buttons.isEmpty()) {
            sb.append("BUTTONS:\n");
            for (PageElement e : buttons) {
                sb.append("  ").append(e).append("\n");
                sb.append("    best locator: ")
                  .append(e.getBestLocator()).append("=\"")
                  .append(e.getBestLocatorValue()).append("\"\n");
            }
            sb.append("\n");
        }

        if (!checkboxes.isEmpty()) {
            sb.append("CHECKBOXES:\n");
            for (PageElement e : checkboxes) {
                sb.append("  ").append(e).append("\n");
                sb.append("    best locator: ")
                  .append(e.getBestLocator()).append("=\"")
                  .append(e.getBestLocatorValue()).append("\"\n");
                sb.append("    initial state: ")
                  .append(e.isChecked() ? "CHECKED" : "UNCHECKED").append("\n");
            }
            sb.append("\n");
        }

        if (!dropdowns.isEmpty()) {
            sb.append("DROPDOWNS:\n");
            for (PageElement e : dropdowns) {
                sb.append("  ").append(e).append("\n");
                sb.append("    best locator: ")
                  .append(e.getBestLocator()).append("=\"")
                  .append(e.getBestLocatorValue()).append("\"\n");
            }
            sb.append("\n");
        }

        if (!links.isEmpty()) {
            sb.append("LINKS:\n");
            for (PageElement e : links) {
                sb.append("  ").append(e).append("\n");
                sb.append("    href: ").append(e.getHref()).append("\n");
                if ("_blank".equals(e.getTarget()))
                    sb.append("    opens in: NEW TAB (target=_blank)\n");
            }
            sb.append("\n");
        }

        if (successCondition != null)
            sb.append("SUCCESS CONDITION: ").append(successCondition).append("\n");
        if (failureCondition != null)
            sb.append("FAILURE CONDITION: ").append(failureCondition).append("\n");
        if (primaryAction != null)
            sb.append("PRIMARY ACTION: ").append(primaryAction).append("\n");

        return sb.toString();
    }
}