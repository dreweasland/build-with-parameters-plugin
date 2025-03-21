package org.jenkinsci.plugins.buildwithparameters;

/**
 * Type of a build parameter.
 *
 * @author Miroslav Cupak (mirocupak@gmail.com)
 * @version 1.0
 */
public enum BuildParameterType {
    STRING,
    BOOLEAN,
    PASSWORD,
    CHOICE,
    TEXT,
    SEPARATOR
}
