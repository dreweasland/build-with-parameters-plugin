package org.jenkinsci.plugins.buildwithparameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import hudson.model.PasswordParameterValue;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class BuildParameterTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testSetValue_PasswordParameterUsesPlaceholder() {
        BuildParameter bp = new BuildParameter("n", "v");
        PasswordParameterValue passwordValue = new PasswordParameterValue("asdf", "fdfd");
        bp.setValue(passwordValue);
        assertEquals(
                "Password parameters should always return the default password placeholder",
                BuildParameter.JOB_DEFAULT_PASSWORD_PLACEHOLDER,
                bp.getValue());
    }

    @Test
    public void testIsDefaultPasswordPlaceholder() {
        String placeholder = BuildParameter.JOB_DEFAULT_PASSWORD_PLACEHOLDER;
        assertFalse(
                "Null should not be recognized as a default password placeholder",
                BuildParameter.isDefaultPasswordPlaceholder(null));
        assertFalse(
                "Empty string should not be recognized as a default password placeholder",
                BuildParameter.isDefaultPasswordPlaceholder(""));
        assertFalse(
                "Modified placeholder should not be recognized as a default password placeholder",
                BuildParameter.isDefaultPasswordPlaceholder(placeholder + "-"));
        assertTrue(
                "Exact placeholder should be recognized as a default password placeholder",
                BuildParameter.isDefaultPasswordPlaceholder(placeholder));
    }
}
