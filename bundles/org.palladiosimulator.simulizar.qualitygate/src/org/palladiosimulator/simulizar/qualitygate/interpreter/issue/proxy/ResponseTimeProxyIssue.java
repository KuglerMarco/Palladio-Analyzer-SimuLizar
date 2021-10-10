package org.palladiosimulator.simulizar.qualitygate.interpreter.issue.proxy;

import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.qualitygate.interpreter.RDSeffSwitchQualitygateContributionSwitch;

/**
 * Proxy for Response Time
 * 
 * @author Marco Kugler
 *
 */
public class ResponseTimeProxyIssue implements InterpretationIssue {

    private PCMRandomVariable predicate;
    private RDSeffSwitchQualitygateContributionSwitch responseTimeQualitygateSwitch;
    private QualityGate qualitygate;
    private Entity stereotypedObject;
    private InterpreterDefaultContext context;

    // because of handler in Dispatch after response scope evaluation (one time pass for ProxyIssue)
    private boolean isHandledOnce = false;
    private RequiredRole requiredRole;

    public ResponseTimeProxyIssue(PCMRandomVariable predicate,
            RDSeffSwitchQualitygateContributionSwitch responseTimeQualitygateSwitch, QualityGate qualitygate,
            Entity stereotypedObject, InterpreterDefaultContext context, RequiredRole role) {

        this.predicate = predicate;
        this.responseTimeQualitygateSwitch = responseTimeQualitygateSwitch;
        this.qualitygate = qualitygate;
        this.stereotypedObject = stereotypedObject;
        this.context = context;
        this.requiredRole = role;

    }

    public InterpreterDefaultContext getContext() {
        return context;
    }

    public PCMRandomVariable getPremise() {
        return predicate;
    }

    public RDSeffSwitchQualitygateContributionSwitch getResponseTimeQualitygateSwitch() {
        return responseTimeQualitygateSwitch;
    }

    @Override
    public boolean isHandled() {
        /*
         * is not intended to be handled, but just removed from the InterpreterResult after
         * evaluation
         */
        return false;
    }

    public boolean isHandledOnce() {
        return isHandledOnce;
    }

    public void setHandledOnce(boolean handledOnce) {
        this.isHandledOnce = handledOnce;
    }

    public QualityGate getQualitygate() {
        return qualitygate;
    }

    public Entity getStereotypedObject() {
        return stereotypedObject;
    }

    public RequiredRole getRequiredRole() {
        return requiredRole;
    }

}
