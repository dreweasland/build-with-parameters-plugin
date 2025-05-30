package org.jenkinsci.plugins.buildwithparameters;

import hudson.model.BooleanParameterValue;
import hudson.model.ParameterValue;
import hudson.model.PasswordParameterValue;
import hudson.model.StringParameterValue;
import hudson.model.TextParameterValue;
import java.util.List;

public class BuildParameter {

    static final String JOB_DEFAULT_PASSWORD_PLACEHOLDER = "job_default_password";
    private BuildParameterType type;
    private final String name;
    private final String description;
    private String value;
    private List<String> choices;
    private String sectionHeader;
    private String separatorStyle;
    private String sectionHeaderStyle;

    public BuildParameter(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public static boolean isDefaultPasswordPlaceholder(String candidate) {
        return JOB_DEFAULT_PASSWORD_PLACEHOLDER.equals(candidate);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getValue() {
        return value;
    }

    public void setValue(ParameterValue parameterValue) {
        if (parameterValue instanceof PasswordParameterValue) {
            this.value = JOB_DEFAULT_PASSWORD_PLACEHOLDER;
        } else if (parameterValue instanceof BooleanParameterValue) {
            this.value = String.valueOf(((BooleanParameterValue) parameterValue).getValue());
        } else if (parameterValue instanceof TextParameterValue || parameterValue instanceof StringParameterValue) {
            this.value = parameterValue instanceof TextParameterValue
                    ? ((TextParameterValue) parameterValue).getValue()
                    : ((StringParameterValue) parameterValue).getValue();
        } else {
            this.value = null;
        }
    }

    public BuildParameterType getType() {
        return type;
    }

    public void setType(BuildParameterType type) {
        this.type = type;
    }

    public List<String> getChoices() {
        return choices;
    }

    public void setChoices(List<String> choices) {
        this.choices = choices;
    }

    public String getSectionHeader() {
        return sectionHeader;
    }

    public void setSectionHeader(String sectionHeader) {
        this.sectionHeader = sectionHeader;
    }

    public String getSeparatorStyle() {
        return separatorStyle;
    }

    public void setSeparatorStyle(String separatorStyle) {
        this.separatorStyle = separatorStyle;
    }

    public String getSectionHeaderStyle() {
        return sectionHeaderStyle;
    }

    public void setSectionHeaderStyle(String sectionHeaderStyle) {
        this.sectionHeaderStyle = sectionHeaderStyle;
    }
}
