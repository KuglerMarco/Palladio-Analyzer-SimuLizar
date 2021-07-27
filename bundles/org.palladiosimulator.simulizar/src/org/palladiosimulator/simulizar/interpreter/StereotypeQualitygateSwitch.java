package org.palladiosimulator.simulizar.interpreter;



import java.util.ArrayList;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.qualitygate.RequestParameterScope;
import org.palladiosimulator.failuremodel.qualitygate.ResultParameterScope;
import org.palladiosimulator.failuremodel.qualitygate.util.QualitygateSwitch;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.ParameterIssue;
import org.palladiosimulator.simulizar.interpreter.result.QualitygateIssue;
import org.palladiosimulator.simulizar.interpreter.result.impl.QualitygateInterpreterResult;

import com.google.common.collect.Lists;

import org.modelversioning.emfprofile.Stereotype;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.uka.ipd.sdq.simucomframework.variables.StackContext;
import de.uka.ipd.sdq.simucomframework.variables.stoexvisitor.VariableMode;

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
        StereotypeQualitygateSwitch createStereotypeSwitch(final InterpreterDefaultContext context, Signature operationSignature,
                RequiredRole requiredRole);
    }
    
    private final String stereotypeName = "QualitygateElement";
    private final String profileName = "QualitygateProfile";
    private final InterpreterDefaultContext context;
    private final Signature operationSignature;
    private final RequiredRole requiredRole;
    
    
    private PCMRandomVariable premise = null;
    private CallScope callScope = CallScope.REQUEST;
    
    
    @AssistedInject
    StereotypeQualitygateSwitch(@Assisted final InterpreterDefaultContext context, @Assisted Signature operationSignature, @Assisted RequiredRole requiredRole){
        
        this.context = context;
        this.operationSignature = operationSignature;
        this.requiredRole = requiredRole;
        
        
    }
    

    /**
     * Extracting the necessary information of the QualityGate.
     */
    @Override
    public InterpreterResult caseQualityGate(QualityGate object) {
        
        //Initializing the Qualitygate-Information
        premise = object.getPremise();
        
        //TODO delete later
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
        
        String[] splittedParameter = premise.getSpecification().split(" ");
        
        
        
        String parameterSpecification = splittedParameter[0];
        String operator = splittedParameter[1];
        int premiseValue = Integer.parseInt(splittedParameter[2]);
        //TODO context.evaluateStatic(operator, null))
        
        
        if(callScope.equals(CallScope.REQUEST) && (signatureOfQualitygate == this.operationSignature)) {
            InterpreterResult result = this.checkParameterOnStack(parameterSpecification, operator, premiseValue);
            boolean premiseComparison = (boolean) context.evaluate(premise.getSpecification(), this.context.getStack().currentStackFrame());
            System.out.println(premiseComparison);
            //TODO delete later
            if(result instanceof QualitygateInterpreterResult) {
                ArrayList<QualitygateIssue> list = Lists.newArrayList(((QualitygateInterpreterResult) result).getQualitygateIssues());
                System.out.println("Liste beinhaltet:" + ((ParameterIssue) list.get(0)).getValueOnStack());
            }
            
            return result;
        }
        return InterpreterResult.OK;
     
    }
    
    
    
    /**
     * Checking the values on the parameter-stack against the premise-specification within the Qualitygate.
     */
    @Override
    public InterpreterResult caseResultParameterScope(ResultParameterScope object) {
        
        String[] splittedParameter = premise.getSpecification().split(" ");
        Signature signatureOfQualitygate = object.getSignature();
        
        String parameterSpecification = splittedParameter[0];
        String operator = splittedParameter[1];
        int premiseValue = Integer.parseInt(splittedParameter[2]);
        
        if(callScope.equals(CallScope.REQUEST) && (signatureOfQualitygate == this.operationSignature)) {
            return this.checkParameterOnStack(parameterSpecification, operator, premiseValue);
        }
        return InterpreterResult.OK;
        
    }
    
    
    //TODO delete
    /**
     * Checks the premise against the values of parameter on stack.
     * 
     * @param parameterSpecification
     *          Parameter which needs to be checked on stack
     * @param operator
     * @param premiseValue
     * @return
     */
    public InterpreterResult checkParameterOnStack(String parameterSpecification, String operator, int premiseValue) {
        
            
            
        //Checking whether Parameter is on Stack
        if(StackContext.evaluateStatic(parameterSpecification,
                this.context.getStack().currentStackFrame(), VariableMode.RETURN_NULL_ON_NOT_FOUND) != null) {
                        
            int valueOnStack = StackContext.evaluateStatic(parameterSpecification, Integer.class,
                    this.context.getStack().currentStackFrame());
                        
            //Distinguishing between Operators
            switch(operator) {
                
            case "<": 
                if(valueOnStack >= premiseValue) {
                    return QualitygateInterpreterResult.of(new ParameterIssue(premise, callScope, valueOnStack));

                }
                break;
                    
            case "<=":
                if(valueOnStack > premiseValue) {
                    return QualitygateInterpreterResult.of(new ParameterIssue(premise, callScope, valueOnStack));

                }
                break;
                    
            case "=":    
                if(valueOnStack != premiseValue) {
                    return QualitygateInterpreterResult.of(new ParameterIssue(premise, callScope, valueOnStack));

                }
                break;
                    
            case ">":    
                if(valueOnStack <= premiseValue) {  
                    return QualitygateInterpreterResult.of(new ParameterIssue(premise, callScope, valueOnStack));

                }
                break;
                    
            case ">=":    
                if(valueOnStack < premiseValue) {
                    return QualitygateInterpreterResult.of(new ParameterIssue(premise, callScope, valueOnStack));

                }
                break;
            }
                
                
                
            
        } else {
            throw new IllegalStateException("Parameter not specified for this signature.");
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
        
        //Qualitygate-Element from the Stereotype
        EList<EObject> taggedValues = StereotypeAPI.getTaggedValue(theEObject, "qualitygate", stereotype.getName());
        
        //Model validation
        if(taggedValues.isEmpty()){
            throw new IllegalArgumentException("Qualitygate-Model not valid: Qualitygate-Stereotype needs to have at least one Qualitygate-element.");
        }
        
        //Calling the Switch with Qualitygate-element
        InterpreterResult result = this.doSwitch(taggedValues.get(0));
        
        
        //TODO delete later
        if(result instanceof QualitygateInterpreterResult) {
            ArrayList<QualitygateIssue> list = Lists.newArrayList(((QualitygateInterpreterResult) result).getQualitygateIssues());
            System.out.println("handleStereotype: " + ((ParameterIssue) list.get(0)).getValueOnStack());
        }
        
        return result;
        

    }
    
    
}
