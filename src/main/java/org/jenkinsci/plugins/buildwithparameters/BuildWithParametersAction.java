package org.jenkinsci.plugins.buildwithparameters;

import com.wangyin.parameter.WHideParameterDefinition;
import hudson.model.*;
import hudson.util.Secret;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;
import jenkins.plugins.parameter_separator.ParameterSeparatorDefinition;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

public class BuildWithParametersAction<T extends Job<?, ?> & ParameterizedJob> implements Action {
    private static final String URL_NAME = "parambuild";
    private final T project;

    public BuildWithParametersAction(T project) {
        this.project = project;
    }

    //////////////////
    //     VIEW     //
    //////////////////

    public String getProjectName() {
        return project.getName();
    }

    public String getDisplayName() {
        return project.getDisplayName();
    }

    /**
     * Returns the list of parameters to be shown in the build UI.
     * Hidden and separator parameters are omitted.
     */
    public List<BuildParameter> getAvailableParameters() {
        return getParameterDefinitions().stream()
                .filter(pd -> !(pd instanceof WHideParameterDefinition))
                .map(pd -> {
                    BuildParameter bp = new BuildParameter(pd.getName(), pd.getDescription());
                    bp.setType(mapParameterType(pd));
                    try {
                        bp.setValue(pd.createValue(Stapler.getCurrentRequest()));
                    } catch (IllegalArgumentException ignored) {
                        // If a provided value does not match available options, leave the value blank.
                    }
                    if (pd instanceof ChoiceParameterDefinition) {
                        bp.setChoices(((ChoiceParameterDefinition) pd).getChoices());
                    }
                    if (pd instanceof jenkins.plugins.parameter_separator.ParameterSeparatorDefinition) {
                        jenkins.plugins.parameter_separator.ParameterSeparatorDefinition sepDef =
                                (jenkins.plugins.parameter_separator.ParameterSeparatorDefinition) pd;
                        bp.setSectionHeader(sepDef.getSectionHeader());
                        bp.setSeparatorStyle(sepDef.getSeparatorStyle());
                        bp.setsectionHeaderStyle(sepDef.getSectionHeaderStyle());
                    }
                    return bp;
                })
                .collect(Collectors.toList());
    }

    private BuildParameterType mapParameterType(ParameterDefinition pd) {
        if (pd instanceof PasswordParameterDefinition) {
            return BuildParameterType.PASSWORD;
        } else if (pd instanceof BooleanParameterDefinition) {
            return BuildParameterType.BOOLEAN;
        } else if (pd instanceof ChoiceParameterDefinition) {
            return BuildParameterType.CHOICE;
        } else if (pd instanceof StringParameterDefinition) {
            return BuildParameterType.STRING;
        } else if (pd instanceof TextParameterDefinition) {
            return BuildParameterType.TEXT;
        } else if (pd instanceof ParameterSeparatorDefinition) {
            return BuildParameterType.SEPARATOR;
        }
        // Default to STRING if type is not recognized.
        return BuildParameterType.STRING;
    }

    public String getIconFileName() {
        return null; // Invisible
    }

    public String getUrlName() {
        project.checkPermission(BuildableItem.BUILD);
        return URL_NAME;
    }

    //////////////////
    //  SUBMISSION  //
    //////////////////

    /**
     * Processes the submitted form.
     * Hidden parameters use their default value when not provided; separator parameters are skipped.
     */
    @RequirePOST
    public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        project.checkPermission(BuildableItem.BUILD);

        List<ParameterValue> values = new ArrayList<>();
        JSONObject formData = req.getSubmittedForm();

        if (!formData.isEmpty()) {
            for (ParameterDefinition pd : getParameterDefinitions()) {
                if (pd instanceof ParameterSeparatorDefinition) {
                    continue;
                }
                ParameterValue pv = pd.createValue(req);
                // For hidden parameters, if no value is supplied, use the default.
                if (pv == null && pd instanceof WHideParameterDefinition) {
                    pv = pd.getDefaultParameterValue();
                }
                if (pv != null) {
                    pv = processSpecialParameterTypes(req, pd, pv);
                    values.add(pv);
                }
            }
        }

        Jenkins.get()
                .getQueue()
                .schedule(project, 0, new ParametersAction(values), new CauseAction(new Cause.UserIdCause()));
        rsp.sendRedirect("../");
    }

    private ParameterValue processSpecialParameterTypes(StaplerRequest req, ParameterDefinition pd, ParameterValue pv) {
        if (pv instanceof BooleanParameterValue) {
            boolean value = req.getParameter(pd.getName()) != null;
            pv = ((BooleanParameterDefinition) pd).createValue(String.valueOf(value));
        } else if (pv instanceof PasswordParameterValue) {
            pv = applyDefaultPassword((PasswordParameterDefinition) pd, (PasswordParameterValue) pv);
        }
        return pv;
    }

    protected ParameterValue applyDefaultPassword(PasswordParameterDefinition pd, PasswordParameterValue pv) {
        String jobPassword = getPasswordValue(pv);
        if (!BuildParameter.isDefaultPasswordPlaceholder(jobPassword)) {
            return pv;
        }
        PasswordParameterValue defaultPv = (PasswordParameterValue) pd.getDefaultParameterValue();
        String defaultPassword = defaultPv != null ? getPasswordValue(defaultPv) : "";
        return new PasswordParameterValue(pv.getName(), defaultPassword);
    }

    public static String getPasswordValue(PasswordParameterValue pv) {
        return Secret.toString(pv.getValue());
    }

    //////////////////
    //   HELPERS    //
    //////////////////

    private List<ParameterDefinition> getParameterDefinitions() {
        ParametersDefinitionProperty prop = project.getProperty(ParametersDefinitionProperty.class);
        return (prop != null && prop.getParameterDefinitions() != null)
                ? prop.getParameterDefinitions()
                : Collections.emptyList();
    }
}
