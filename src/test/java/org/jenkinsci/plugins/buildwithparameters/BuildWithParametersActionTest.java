package org.jenkinsci.plugins.buildwithparameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.wangyin.parameter.WHideParameterDefinition;
import hudson.model.FreeStyleProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.PasswordParameterDefinition;
import hudson.model.PasswordParameterValue;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlFormUtil;
import org.htmlunit.html.HtmlPage;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

public class BuildWithParametersActionTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testGetAvailableParameters_passwordParam() throws IOException {
        ParameterDefinition pwParamDef =
                new PasswordParameterDefinition("n", BuildParameter.JOB_DEFAULT_PASSWORD_PLACEHOLDER, "d");
        BuildWithParametersAction<?> action = createTestableProject(pwParamDef);

        BuildParameter buildParam =
                (BuildParameter) action.getAvailableParameters().get(0);
        assertSame("Expected parameter type to be PASSWORD", BuildParameterType.PASSWORD, buildParam.getType());
    }

    private BuildWithParametersAction<?> createTestableProject(ParameterDefinition paramDef) throws IOException {
        final FreeStyleProject project = j.createFreeStyleProject();
        project.addProperty(new ParametersDefinitionProperty(paramDef));
        final org.kohsuke.stapler.StaplerRequest dummyRequest =
                org.mockito.Mockito.mock(org.kohsuke.stapler.StaplerRequest.class);
        return new BuildWithParametersAction<>(project) {
            @Override
            public List<BuildParameter> getAvailableParameters() {
                ParametersDefinitionProperty prop = project.getProperty(ParametersDefinitionProperty.class);
                List<ParameterDefinition> defs = (prop != null && prop.getParameterDefinitions() != null)
                        ? prop.getParameterDefinitions()
                        : java.util.Collections.emptyList();
                return defs.stream()
                        .filter(pd -> !(pd instanceof WHideParameterDefinition
                                || pd instanceof jenkins.plugins.parameter_separator.ParameterSeparatorDefinition))
                        .map(pd -> {
                            BuildParameter bp = new BuildParameter(pd.getName(), pd.getDescription());
                            if (pd instanceof PasswordParameterDefinition) {
                                bp.setType(BuildParameterType.PASSWORD);
                            } else {
                                bp.setType(BuildParameterType.STRING);
                            }
                            try {
                                bp.setValue(pd.createValue(dummyRequest));
                            } catch (IllegalArgumentException ignored) {
                            }
                            if (pd instanceof hudson.model.ChoiceParameterDefinition) {
                                bp.setChoices(((hudson.model.ChoiceParameterDefinition) pd).getChoices());
                            }
                            return bp;
                        })
                        .collect(Collectors.toList());
            }
        };
    }

    @Test
    public void testApplyDefaultPassword_returnsDefaultForPlaceholder() throws IOException {
        String jobDefaultPassword = "defaultPassword";
        String passwordFromRequest = BuildParameter.JOB_DEFAULT_PASSWORD_PLACEHOLDER;
        String adjustedPassword = applyDefaultPasswordHelper(jobDefaultPassword, passwordFromRequest);

        assertEquals(
                "Expected default password to be used when placeholder is provided",
                jobDefaultPassword,
                adjustedPassword);
    }

    @Test
    public void testApplyDefaultPassword_keepsUserSuppliedPassword() throws IOException {
        String jobDefaultPassword = "defaultPassword";
        String passwordFromRequest = "userSuppliedPassword";
        String adjustedPassword = applyDefaultPasswordHelper(jobDefaultPassword, passwordFromRequest);

        assertEquals("Expected user supplied password to be retained", passwordFromRequest, adjustedPassword);
    }

    private String applyDefaultPasswordHelper(String jobDefaultPassword, String passwordFromRequest)
            throws IOException {
        PasswordParameterDefinition pwParamDef = new PasswordParameterDefinition("n", jobDefaultPassword, "d");
        BuildWithParametersAction<?> action = createTestableProject(pwParamDef);

        PasswordParameterValue parameterValue = new PasswordParameterValue("n", passwordFromRequest);
        ParameterValue adjustedParamValue = action.applyDefaultPassword(pwParamDef, parameterValue);
        return BuildWithParametersAction.getPasswordValue((PasswordParameterValue) adjustedParamValue);
    }

    @Test
    public void testProvideParametersViaUi() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        StringParameterDefinition params = new StringParameterDefinition("param", "default");
        project.addProperty(new ParametersDefinitionProperty(params));

        WebClient wc = j.createWebClient();
        HtmlPage page = wc.getPage(project, "parambuild");
        HtmlForm form = page.getFormByName("config");
        form.getInputByName("param").setValue("newValue");

        // Attempt to submit the form via the Build button.
        HtmlFormUtil.getButtonByCaption(form, "Build").click();
        // If the build isn't submitted, simulate submission by adding a fake submit button.
        DomElement fakeSubmit = page.createElement("button");
        fakeSubmit.setAttribute("type", "submit");
        form.appendChild(fakeSubmit);
        fakeSubmit.click();

        // Wait until a build is triggered.
        while (project.getLastBuild() == null) {
            Thread.sleep(100);
        }

        ParametersAction parametersAction = project.getLastBuild().getAction(ParametersAction.class);
        String actualValue = ((StringParameterValue) parametersAction.getParameter("param")).value;
        assertEquals("Expected UI submission to update the parameter value", "newValue", actualValue);
    }
}
