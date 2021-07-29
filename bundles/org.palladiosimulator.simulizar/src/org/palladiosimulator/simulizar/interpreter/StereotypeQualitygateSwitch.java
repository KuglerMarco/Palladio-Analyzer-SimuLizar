package org.palladiosimulator.simulizar.interpreter;



import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.qualitygate.RequestParameterScope;
import org.palladiosimulator.failuremodel.qualitygate.ResultParameterScope;
import org.palladiosimulator.failuremodel.qualitygate.util.QualitygateSwitch;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitchStereotypeContributionFactory.ComposedStructureInnerSwitchStereotypeElementDispatcher;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.ParameterIssue;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResultMerger;
import org.modelversioning.emfprofile.Stereotype;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

/**
 * Switch processing the Qualitygate-Stereotype.
 * 
 * @author Marco Kugler
 *
 */
public class StereotypeQualitygateSwitch extends QualitygateSwitch<InterpreterResult> implements StereotypeSwitch {

    @AssistedFactory
    public interface Factory extends ComposedStructureInnerSwitchStereotypeContributionFactory {
        @Override
        StereotypeQualitygateSwitch createStereotypeSwitch(final InterpreterDefaultContext context, final Signature operationSignature,
                final RequiredRole requiredRole, ComposedStructureInnerSwitchStereotypeElementDispatcher parentSwitch);
    }
    
    //Information about the stereotype it is processing
    private final String stereotypeName = "QualitygateElement";
    private final String profileName = "QualitygateProfile";
    
    //Information about the simulation-context
    private final InterpreterDefaultContext context;
    private final Signature operationSignature;
    private final RequiredRole requiredRole;
    private final ComposedStructureInnerSwitchStereotypeElementDispatcher parentSwitch;

    //Information about the stereotype attachment and processing time
    private Stereotype stereotype;
    private Entity object;
    private PCMRandomVariable premise;
    private CallScope callScope = CallScope.REQUEST;
    
    
    private static final Logger LOGGER = Logger.getLogger(StereotypeQualitygateSwitch.class);
    private final BasicInterpreterResultMerger merger;
    
    
    @AssistedInject
    StereotypeQualitygateSwitch(@Assisted final InterpreterDefaultContext context, @Assisted Signature operationSignature, @Assisted RequiredRole requiredRole,
            @Assisted ComposedStructureInnerSwitchStereotypeElementDispatcher parentSwitch, BasicInterpreterResultMerger merger){
        
        this.merger = merger;
        this.context = context;
        this.operationSignature = operationSignature;
        this.requiredRole = requiredRole;
        this.parentSwitch = parentSwitch;
        
        LOGGER.setLevel(Level.DEBUG);
        
    }
    

    
    /**
     * Processing the attached Qualitygat, Premise and Scope
     */
    @Override
    public InterpreterResult caseQualityGate(QualityGate object) {
        
        premise = object.getPremise();
        return this.doSwitch(object.getScope());

    }
    
    
    /**
     * Checking the values on the parameter-stack against the premise-specification within the Qualitygate.
     */
    @Override
    public InterpreterResult caseRequestParameterScope(RequestParameterScope object) {
        
        Signature signatureOfQualitygate = object.getSignature();

        if(callScope.equals(CallScope.REQUEST) && (signatureOfQualitygate == (this.operationSignature))) {
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("stackframe is: " + this.context.getStack().currentStackFrame().toString());
            }
            if(!((boolean) context.evaluate(premise.getSpecification(), this.context.getStack().currentStackFrame()))) {

                if(LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StoEx is broken: " + premise.getSpecification() + " because stackframe is: " + this.context.getStack().currentStackFrame().toString());
                }
                    
                return BasicInterpreterResult.of(new ParameterIssue(this.object, stereotype, this.context.getStack().currentStackFrame().toString()));

            }
            
            
        }
        return InterpreterResult.OK;
     
    }
    
    
    
    /**
     * Checking the values on the parameter-stack against the premise-specification within the Qualitygate.
     */
    @Override
    public InterpreterResult caseResultParameterScope(ResultParameterScope object) {
        
        Signature signatureOfQualitygate = object.getSignature();

        if(callScope.equals(CallScope.RESPONSE) && (signatureOfQualitygate == (this.operationSignature))) {
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("stackframe is: " + this.context.getStack().currentStackFrame().toString());
            }
            if(!((boolean) context.evaluate(premise.getSpecification(), this.context.getStack().currentStackFrame()))) {

                if(LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StoEx is broken: " + premise.getSpecification() + " because stackframe is: " + this.context.getStack().currentStackFrame().toString());
                }
                    
                return BasicInterpreterResult.of(new ParameterIssue(this.object, stereotype, this.context.getStack().currentStackFrame().toString()));

            }
            
            
        }
        return InterpreterResult.OK;
        
    }
    
    


    /**
     * Returns whether Switch is for Stereotype
     */
    @Override
    public boolean isSwitchForStereotype(Stereotype stereotype) {
        
        boolean result = stereotype.getProfile().getName().equals(profileName);
        if(result) {
            return stereotype.getName().equals(stereotypeName);
        }
        return result;
        
    }
    
    
    /**
     * Handles the Stereotype attached to the element
     */
    @Override
    public InterpreterResult handleStereotype(Stereotype stereotype, EObject theEObject, CallScope callScope) {
        
        InterpreterResult result = InterpreterResult.OK;
        
        this.callScope = callScope;
        this.stereotype = stereotype;
        this.object = (Entity) theEObject;
        
        EList<QualityGate> taggedValues = StereotypeAPI.getTaggedValue(theEObject, "qualitygate", stereotype.getName());
        
        //Model validation
        if(taggedValues.isEmpty()){
            throw new IllegalArgumentException("Qualitygate-Model not valid: Qualitygate-Stereotype needs to have at least one Qualitygate element.");
        }
        
        //Processing all the attached Qualitygates
        for(QualityGate e : taggedValues) {
            
            result = merger.merge(result, this.doSwitch(e));
            
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("Qualitygate " + e.getEntityName() + " was processed with premise " + this.premise.getSpecification());
            }
        }

        return result;
        
    }


    @Override
    public String getStereotypeName() {
        return this.stereotypeName;
    }


    @Override
    public String getProfileName() {
        return this.profileName;
    }
    
    
}
