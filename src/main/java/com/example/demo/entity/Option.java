package com.example.demo.entity;

public class Option {
    private String value; 
    private String label; 
    private String text; 

    public Option() {
    }

    public Option(String value, String label, String text) {
        this.value = value;
        this.label = label;
        this.text = text;
    }

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

