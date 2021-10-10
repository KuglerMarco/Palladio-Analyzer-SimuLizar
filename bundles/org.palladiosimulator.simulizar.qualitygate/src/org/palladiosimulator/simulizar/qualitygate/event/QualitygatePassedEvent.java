package org.palladiosimulator.simulizar.qualitygate.event;

import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.severityhierarchy.Severity;
import org.palladiosimulator.pcm.core.entity.Entity;

import de.uka.ipd.sdq.simucomframework.Context;
import de.uka.ipd.sdq.simucomframework.SimuComSimProcess;

/**
 * Event for the qualitygate evaluation.
 * 
 * @author Marco Kugler
 *
 */
public class QualitygatePassedEvent {

    private final QualityGate qualitygate;
    private final Context context;
    private final boolean isSuccess;
    private final Severity severity;
    private final Entity stereotypedObject;
    private final boolean isCrash;

    public QualitygatePassedEvent(final QualityGate modelElement, final Context context, final boolean isSuccess,
            final Severity severity, final Entity stereotypedObject, boolean isCrash) {

        this.qualitygate = modelElement;
        this.context = context;
        this.isSuccess = isSuccess;
        this.severity = severity;
        this.stereotypedObject = stereotypedObject;
        this.isCrash = isCrash;

    }

    public QualityGate getModelElement() {
        return this.qualitygate;
    }

    public Context getContext() {
        return this.context;
    }

    public SimuComSimProcess getThread() {
        return this.context.getThread();
    }

    /**
     * @return whether the qualitygate-evaluation was successful
     */
    public boolean isSuccess() {
        return isSuccess;
    }

    public Severity getSeverity() {
        return this.severity;
    }

    public Entity getStereotypedObject() {
        return stereotypedObject;
    }

    /**
     * @return whether the execution crashed at the time of evaluation
     */
    public boolean isCrash() {
        return isCrash;
    }

}
