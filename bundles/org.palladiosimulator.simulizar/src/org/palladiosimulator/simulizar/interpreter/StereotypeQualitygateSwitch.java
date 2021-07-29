package org.palladiosimulator.simulizar.interpreter;



import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.qualitygate.RequestParameterScope;
import org.palladiosimulator.failuremodel.qualitygate.ResultParameterScope;
import org.palladiosimulator.failuremodel.qualitygate.util.QualitygateSwitch;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.composition.AssemblyConnector;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitchStereotypeContributionFactory.ComposedStructureInnerSwitchStereotypeElementDispatcher;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.ParameterIssue;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResult;


import org.modelversioning.emfprofile.Stereotype;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

/**
 * Switch processing the attached Qualitygate-Stereotype.
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
    
    private final String stereotypeName = "QualitygateElement";
    private final String profileName = "QualitygateProfile";
    private final InterpreterDefaultContext context;
    private final Signature operationSignature;
    private final RequiredRole requiredRole;
    private Stereotype stereotype;
    private final ComposedStructureInnerSwitchStereotypeElementDispatcher parentSwitch;
    private static final Logger LOGGER = Logger.getLogger(StereotypeQualitygateSwitch.class);
    Entity object = null;
//    ParameterIssue.Factory parameterIssueFactory;
    
    
    private PCMRandomVariable premise = null;
    private CallScope callScope = CallScope.REQUEST;
    
    
    @AssistedInject
    StereotypeQualitygateSwitch(@Assisted final InterpreterDefaultContext context, @Assisted Signature operationSignature, @Assisted RequiredRole requiredRole,
            @Assisted ComposedStructureInnerSwitchStereotypeElementDispatcher parentSwitch){
        
        this.context = context;
        this.operationSignature = operationSignature;
        this.requiredRole = requiredRole;
        this.parentSwitch = parentSwitch;
//        this.parameterIssueFactory = parameterIssueFactory;
        
        
    }
    

    /**
     * Extracting the necessary information of the QualityGate.
     */
    @Override
    public InterpreterResult caseQualityGate(QualityGate object) {
        
        //Initializing the Qualitygate-Information
        premise = object.getPremise();
        
        //TODO delete later
        LOGGER.info("");

        
        System.out.println("Qualitygate erkannt.");
        System.out.println(premise.getSpecification());
        
        return this.doSwitch(object.getScope());

    }
    
    
    /**
     * Checking the values on the parameter-stack against the premise-specification within the Qualitygate.
     */
    @Override
    public InterpreterResult caseRequestParameterScope(RequestParameterScope object) {
        
        Signature signatureOfQualitygate = object.getSignature();
        
        
        
        if(callScope.equals(CallScope.REQUEST) && (signatureOfQualitygate == this.operationSignature)) {
            
//            try {
                if((boolean) context.evaluate(premise.getSpecification(), this.context.getStack().currentStackFrame())) {
                    

                    
                    System.out.println("Stoex ist wahr: " + stereotype.toString());
                    
                    return BasicInterpreterResult.of(new ParameterIssue(this.object, stereotype, this.context.getStack().currentStackFrame().toString()));

                }
//            } 
//                catch(ClassCastException e) {
//                throw new IllegalArgumentException("Invalid Model: Premise needs to be a boolean specification.");
//            }
            
            
        }
        return InterpreterResult.OK;
     
    }
    
    
    
    /**
     * Checking the values on the parameter-stack against the premise-specification within the Qualitygate.
     */
    @Override
    public InterpreterResult caseResultParameterScope(ResultParameterScope object) {
        
        Signature signatureOfQualitygate = object.getSignature();
        
        
        
        if(callScope.equals(CallScope.RESPONSE) && (signatureOfQualitygate == this.operationSignature)) {
            
            try {
                if((boolean) context.evaluate(premise.getSpecification(), this.context.getCurrentResultFrame())) {

                    System.out.print("Stoex ist wahr");
                    
                    return BasicInterpreterResult.of(new ParameterIssue(this.object, stereotype, this.context.getCurrentResultFrame().toString()));

                }
            } catch(ClassCastException e) {
                throw new IllegalArgumentException("Invalid Model: Premise needs to be a boolean specification.");
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
        
        
        //Call- or Return-Processing
        this.callScope = callScope;
        
        this.stereotype = stereotype;
        this.object = (Entity) theEObject;
        
        //Qualitygate-Element from the Stereotype
        EList<EObject> taggedValues = StereotypeAPI.getTaggedValue(theEObject, "qualitygate", stereotype.getName());
        
        //Model validation
        if(taggedValues.isEmpty()){
            throw new IllegalArgumentException("Qualitygate-Model not valid: Qualitygate-Stereotype needs to have at least one Qualitygate element.");
        }
        
        //Calling the Switch with Qualitygate-element
        InterpreterResult result = this.doSwitch(taggedValues.get(0));
        
        
        
        return result;
        

    }
    
    
}
