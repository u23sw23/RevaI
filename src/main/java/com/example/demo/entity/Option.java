package com.example.demo.entity;

/**
 * 选项实体类
 */
public class Option {
    private String value; // 选项值，如 "A", "B", "C"
    private String label; // 选项标签，如 "A", "B", "C"
    private String text; // 选项文本内容

    public Option() {
    }

    public Option(String value, String label, String text) {
        this.value = value;
        this.label = label;
        this.text = text;
    }

    // Getters and Setters
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

