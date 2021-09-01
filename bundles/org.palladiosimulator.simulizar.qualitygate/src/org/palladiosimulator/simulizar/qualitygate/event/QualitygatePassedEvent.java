package org.palladiosimulator.simulizar.qualitygate.event;

import org.palladiosimulator.failuremodel.qualitygate.Catastrophic;
import org.palladiosimulator.failuremodel.qualitygate.Hazardous;
import org.palladiosimulator.failuremodel.qualitygate.Major;
import org.palladiosimulator.failuremodel.qualitygate.Minor;
import org.palladiosimulator.failuremodel.qualitygate.NoSafetyEffect;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.qualitygate.Severity;
import org.palladiosimulator.metricspec.Identifier;
import org.palladiosimulator.simulizar.qualitygate.metric.QualitygateMetricDescriptionConstants;

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
    private Identifier severity;
    

    public QualitygatePassedEvent(final QualityGate modelElement, final Context context, final boolean success, final Severity severity) {
        
        this.modelElement = modelElement;
        this.context = context;
        this.passageTime = context.getThread().getModel().getSimulationControl().getCurrentSimulationTime();
        this.success = success;
        
        this.severity = null;
        
        if(severity instanceof NoSafetyEffect) { 
            this.severity = QualitygateMetricDescriptionConstants.NO_SAFETY_EFFECT;
        } else if(severity instanceof Minor) {
            this.severity = QualitygateMetricDescriptionConstants.MINOR;
        } else if(severity instanceof Major) {
            this.severity = QualitygateMetricDescriptionConstants.MAJOR;
        } else if(severity instanceof Hazardous) {
            this.severity = QualitygateMetricDescriptionConstants.HAZARDOUS;
        } else if(severity instanceof Catastrophic) { 
            this.severity = QualitygateMetricDescriptionConstants.CATASTROPHIC;
        } 
        
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
    
    
    public Identifier getSeverity() {
        return severity;
    }

}
