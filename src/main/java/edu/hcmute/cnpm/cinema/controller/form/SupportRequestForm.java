package edu.hcmute.cnpm.cinema.controller.form;

import edu.hcmute.cnpm.cinema.entity.SupportCategory;

public class SupportRequestForm {
    private String subject;
    private SupportCategory category;
    private String content;

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public SupportCategory getCategory() { return category; }
    public void setCategory(SupportCategory category) { this.category = category; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
