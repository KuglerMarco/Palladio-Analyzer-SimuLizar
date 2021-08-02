package org.palladiosimulator.simulizar.qualitygate.interpreter.issue;

import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;

/**
 * Allows to record a broken Qualitygate in the QualitygateInterpreterResult.
 * Different kinds of QualitygateIssues can be described with implementing this interface.
 * 
 * @author Marco Kugler
 *
 */
public interface QualitygateIssue extends InterpretationIssue{
}
