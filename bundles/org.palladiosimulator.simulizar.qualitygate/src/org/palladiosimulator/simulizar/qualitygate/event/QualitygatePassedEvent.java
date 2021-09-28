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

    private final QualityGate modelElement;
    private final Context context;
    private final boolean success;
    private final Severity severity;
    private final Entity stereotypedObject;

    public QualitygatePassedEvent(final QualityGate modelElement, final Context context, final boolean success,
            final Severity severity, final Entity stereotypedObject) {

        this.modelElement = modelElement;
        this.context = context;
        this.success = success;
        this.severity = severity;
        this.stereotypedObject = stereotypedObject;

    }

    /**
     * @return the modelElement
     */
    public QualityGate getModelElement() {
        return this.modelElement;
    }

    /**
     * @return the context
     */
    public Context getContext() {
        return this.context;
    }

    /**
     * @return the thread
     */
    public SimuComSimProcess getThread() {
        return this.context.getThread();
    }

    /**
     * @return whether the qualitygate-evaluation was successful
     */
    public boolean isSuccess() {
        return success;
    }

    public Severity getSeverity() {
        return this.severity;
    }

    public Entity getStereotypedObject() {
        return stereotypedObject;
    }

}
