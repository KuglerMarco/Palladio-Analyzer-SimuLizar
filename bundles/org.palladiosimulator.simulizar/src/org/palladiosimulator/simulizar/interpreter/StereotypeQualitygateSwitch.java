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
        StereotypeQualitygateSwitch createStereotypeSwitch(final InterpreterDefaultContext context);
    }
    
    private final String stereotypeName = "QualitygateElement";
    private final String profileName = "QualitygateProfile";
    private final InterpreterDefaultContext context;
    
    
    private PCMRandomVariable premise = null;
    private CallScope callScope = CallScope.REQUEST;
    
    
    @AssistedInject
    StereotypeQualitygateSwitch(@Assisted final InterpreterDefaultContext context){
        
        this.context = context;
        
        
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
        
        String[] splittedParameter = premise.getSpecification().split(" ");
        
        String parameterSpecification = splittedParameter[0];
        String operator = splittedParameter[1];
        int premiseValue = Integer.parseInt(splittedParameter[2]);
        
        if(callScope.equals(CallScope.REQUEST)) {
            InterpreterResult result = this.checkParameterOnStack(parameterSpecification, operator, premiseValue);
            
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
        
        String parameterSpecification = splittedParameter[0];
        String operator = splittedParameter[1];
        int premiseValue = Integer.parseInt(splittedParameter[2]);
        
        if(callScope.equals(CallScope.RESPONSE)) {
            return this.checkParameterOnStack(parameterSpecification, operator, premiseValue);
        }
        return InterpreterResult.OK;
        
    }
    
    
    
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
        
        
        //Call- or Return-Processing ?
        this.callScope = callScope;
        
        //Getting the Qualitygate-Element from the Stereotype
        EList<EObject> taggedValues = StereotypeAPI.getTaggedValue(theEObject, "qualitygate", stereotype.getName());

        return this.doSwitch(taggedValues.get(0));
            

//            if(result instanceof QualitygateInterpreterResult) {
//
//                for (QualitygateIssue e : ((QualitygateInterpreterResult) result).getQualitygateIssues()) {
//                    System.out.println("Premise is " + e.getPremise().getSpecification());
//                }
//                System.out.println(((QualitygateInterpreterResult) result).getQualitygateIssues().iterator());
//            }
//            


    }
    
    
}
