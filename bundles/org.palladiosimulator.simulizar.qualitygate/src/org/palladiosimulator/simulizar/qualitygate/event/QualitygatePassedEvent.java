package org.palladiosimulator.simulizar.qualitygate.event;

import org.palladiosimulator.failuremodel.qualitygate.QualityGate;

import org.palladiosimulator.metricspec.Identifier;
import de.uka.ipd.sdq.simucomframework.Context;
import de.uka.ipd.sdq.simucomframework.SimuComSimProcess;

public class QualitygatePassedEvent {
    
    private final QualityGate modelElement;
    private final double passageTime;
    private final Context context;
    private final Identifier identifier;
    

    public QualitygatePassedEvent(final QualityGate modelElement, final Context context, final Identifier identifier) {
        
        this.modelElement = modelElement;
        this.context = context;
        this.passageTime = context.getThread().getModel().getSimulationControl().getCurrentSimulationTime();
        this.identifier = identifier;
        
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
    
    public Identifier getIdentifier() {
        return identifier;
    }

}
