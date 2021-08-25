package org.palladiosimulator.simulizar.qualitygate.event;

import org.palladiosimulator.failuremodel.qualitygate.QualityGate;

import de.uka.ipd.sdq.simucomframework.Context;
import de.uka.ipd.sdq.simucomframework.SimuComSimProcess;

public class QualitygatePassedEvent {
    
    private final QualityGate modelElement;
    private final double passageTime;
    private final Context context;
    private final boolean success;
    

    public QualitygatePassedEvent(final QualityGate modelElement, final Context context, final boolean success) {
        
        this.modelElement = modelElement;
        this.context = context;
        this.passageTime = context.getThread().getModel().getSimulationControl().getCurrentSimulationTime();
        this.success = success;
        
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
    
    public boolean isSuccess() {
        return success;
    }

}
