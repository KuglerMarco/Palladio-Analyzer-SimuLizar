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
    private org.palladiosimulator.failuremodel.severityhierarchy.Severity severityNew;
    

    public QualitygatePassedEvent(final QualityGate modelElement, final Context context, final boolean success, final org.palladiosimulator.failuremodel.severityhierarchy.Severity severity2) {
        
        this.modelElement = modelElement;
        this.context = context;
        this.passageTime = context.getThread().getModel().getSimulationControl().getCurrentSimulationTime();
        this.success = success;
        
        this.severity = null;
        
        // TODO hier (im Registry dann abgelegt) müsste Identifier gemäß severity erstellet werden, bei selbst konfigurierbarer Hierarchie
        
        if(severity2 instanceof NoSafetyEffect) { 
            this.severity = QualitygateMetricDescriptionConstants.NO_SAFETY_EFFECT;
        } else if(severity2 instanceof Minor) {
            this.severity = QualitygateMetricDescriptionConstants.MINOR;
        } else if(severity2 instanceof Major) {
            this.severity = QualitygateMetricDescriptionConstants.MAJOR;
        } else if(severity2 instanceof Hazardous) {
            this.severity = QualitygateMetricDescriptionConstants.HAZARDOUS;
        } else if(severity2 instanceof Catastrophic) { 
            this.severity = QualitygateMetricDescriptionConstants.CATASTROPHIC;
        } 
        
        this.severityNew = severity2;
        
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
    
    
//    public Identifier getSeverity() {
//        return severity;
//    }
    
    public org.palladiosimulator.failuremodel.severityhierarchy.Severity getSeverityNew() {
        return this.severityNew;
    }

}
