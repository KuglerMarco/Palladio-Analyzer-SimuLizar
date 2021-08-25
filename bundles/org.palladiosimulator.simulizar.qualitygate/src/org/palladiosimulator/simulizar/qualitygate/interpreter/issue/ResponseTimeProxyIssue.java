package org.palladiosimulator.simulizar.qualitygate.interpreter.issue;


import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.qualitygate.interpreter.RDSeffSwitchQualitygateContributionSwitch;

public class ResponseTimeProxyIssue implements InterpretationIssue {

    private PCMRandomVariable premise;
    private RDSeffSwitchQualitygateContributionSwitch responseTimeQualitygateSwitch;
    private QualityGate qualitygate;
    private Entity stereotypedObject;
    private InterpreterDefaultContext context;



    public ResponseTimeProxyIssue(PCMRandomVariable premise, RDSeffSwitchQualitygateContributionSwitch responseTimeQualitygateSwitch,
            QualityGate qualitygate, Entity stereotypedObject, InterpreterDefaultContext context) {
        
        this.premise = premise;
        this.responseTimeQualitygateSwitch = responseTimeQualitygateSwitch;
        this.qualitygate = qualitygate;
        this.stereotypedObject = stereotypedObject;
        this.context = context;
        
    }
    
    public InterpreterDefaultContext getContext() {
        return context;
    }
    
    public PCMRandomVariable getPremise() {
        return premise;
    }

    public RDSeffSwitchQualitygateContributionSwitch getResponseTimeQualitygateSwitch() {
        return responseTimeQualitygateSwitch;
    }

    @Override
    public boolean isHandled() {
        // TODO Auto-generated method stub
        return false;
    }

    public QualityGate getQualitygate() {
        return qualitygate;
    }

    public Entity getStereotypedObject() {
        return stereotypedObject;
    }

}
