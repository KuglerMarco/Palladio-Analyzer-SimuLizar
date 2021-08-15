package org.palladiosimulator.simulizar.qualitygate.interpreter.issue;

import java.util.ArrayList;
import java.util.Map.Entry;

import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.simulizar.entity.EntityReference;
import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.qualitygate.interpreter.RDSeffSwitchQualitygateContributionSwitch;

public class ResponseTimeProxyIssue implements InterpretationIssue {

    private PCMRandomVariable premise;
    private RDSeffSwitchQualitygateContributionSwitch seffSwitch;
    private QualityGate qualitygate;
    private Entity stereotypedObject;

    public ResponseTimeProxyIssue(PCMRandomVariable premise, RDSeffSwitchQualitygateContributionSwitch seffSwitch,
            QualityGate qualitygate, Entity stereotypedObject) {
        
        this.premise = premise;
        this.seffSwitch = seffSwitch;
        this.qualitygate = qualitygate;
        this.stereotypedObject = stereotypedObject;
        
    }

    public PCMRandomVariable getPremise() {
        return premise;
    }

    public RDSeffSwitchQualitygateContributionSwitch getSeffSwitch() {
        return seffSwitch;
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
