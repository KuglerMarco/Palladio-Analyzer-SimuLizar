package org.palladiosimulator.simulizar.qualitygate.event;

import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.severityhierarchy.Severity;

import de.uka.ipd.sdq.simucomframework.Context;
import de.uka.ipd.sdq.simucomframework.SimuComSimProcess;

/**
 * Event in case of a broken qualitygate.
 * 
 * @author Marco Kugler
 *
 */
public class QualitygatePassedEvent {
    
    private final QualityGate modelElement;
    private final double passageTime;
    private final Context context;
    private final boolean success;
    private final Severity severity;
    

    public QualitygatePassedEvent(final QualityGate modelElement, final Context context, final boolean success, final Severity severity) {
        
        this.modelElement = modelElement;
        this.context = context;
        this.passageTime = context.getThread().getModel().getSimulationControl().getCurrentSimulationTime();
        this.success = success;
        
        this.severity = severity;
        
    }
    /**
     * @return the modelElement
     */
    public QualityGate getModelElement() {
        return this.modelElement;
    }

    /**
     * @return the passageTime
     */
    public double getPassageTime() {
        return this.passageTime;
    }

    /**
     *  @return the context
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

}
