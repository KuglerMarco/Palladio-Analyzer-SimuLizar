package org.palladiosimulator.simulizar.qualitygate.interpreter.issue;

import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.simulizar.entity.EntityReference;
import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;

/**
 * Allows to record a broken Qualitygate in the QualitygateInterpreterResult. Different kinds of
 * QualitygateIssues can be described with implementing this interface.
 * 
 * @author Marco Kugler
 *
 */
public interface QualitygateIssue extends InterpretationIssue {

    public EntityReference<Entity> getStereotypedObjectRef();

    public EntityReference<QualityGate> getQualitygateRef();

}
