package org.palladiosimulator.simulizar.qualitygate.interpreter.issue;


import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.qualitygate.interpreter.ResponseTimeQualitygateSwitch;

public class ResponseTimeProxyIssue implements InterpretationIssue {

    private PCMRandomVariable premise;
    private ResponseTimeQualitygateSwitch responseTimeQualitygateSwitch;
    private QualityGate qualitygate;
    private Entity stereotypedObject;

    public ResponseTimeProxyIssue(PCMRandomVariable premise, ResponseTimeQualitygateSwitch responseTimeQualitygateSwitch,
            QualityGate qualitygate, Entity stereotypedObject) {
        
        this.premise = premise;
        this.responseTimeQualitygateSwitch = responseTimeQualitygateSwitch;
        this.qualitygate = qualitygate;
        this.stereotypedObject = stereotypedObject;
        
    }

    public PCMRandomVariable getPremise() {
        return premise;
    }

    public ResponseTimeQualitygateSwitch getResponseTimeQualitygateSwitch() {
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
