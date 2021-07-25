package org.palladiosimulator.simulizar.interpreter;



import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.qualitygate.RequestParameterScope;
import org.palladiosimulator.failuremodel.qualitygate.Scope;
import org.palladiosimulator.failuremodel.qualitygate.util.QualitygateSwitch;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitchStereotypeContributionFactory.ComposedStructureInnerSwitchStereotypeElementDispatcher;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.ParameterIssue;
import org.palladiosimulator.simulizar.interpreter.result.impl.QualitygateInterpreterResult;
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
    
    //TODO Deklarationen richtig
    private final String stereotypeName = "QualitygateElement";
    private final String profileName = "QualitygateProfile";
    final InterpreterDefaultContext context;
    
    /*
     * Qualitygate-Information
     */
    Scope qualitygateScope = null;
    PCMRandomVariable premise = null;
    //Request or Response-Processing
    CallScope callScope = CallScope.REQUEST;
    
    
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
        qualitygateScope = object.getScope();
        premise = object.getPremise();
        
        //TODO delete later
        System.out.println("Qualitygate erkannt.");
        System.out.println(premise.getSpecification());
        
        return this.doSwitch(object.getScope());

    }
    
    
    @Override
    public InterpreterResult caseRequestParameterScope(RequestParameterScope object) {
        
        String[] splittedParameter = premise.getSpecification().split(" ");
        
        String parameterSpecification = splittedParameter[0];
        String operator = splittedParameter[1];
        int premiseValue = Integer.parseInt(splittedParameter[2]);
        
        //Checking the processing time of the Qualitygate: Request or Response?
        if(callScope.equals(CallScope.REQUEST)) {
            
            
            //Checking whether Parameter is on Stack
            //TODO .getValue benutzen?
            if(StackContext.evaluateStatic(parameterSpecification,
                this.context.getStack().currentStackFrame(), VariableMode.RETURN_NULL_ON_NOT_FOUND) != null) {
                
                int valueOnStack = StackContext.evaluateStatic(parameterSpecification, Integer.class,
                    this.context.getStack().currentStackFrame());
            
            
                //TODO delete later
                System.out.println(valueOnStack);
                System.out.println(premiseValue);
                
                //Distinguishing between Operators
                switch(operator) {
                
                case "<": 
                    if(premiseValue < valueOnStack) {
                        
                        //TODO wenn kleiner, dann Fehlerhistorie anlegen (welche Infos sind nötig?)
                        
                        //TODO delete later
                        System.out.println("Breaking Qualitygate");
                        
                        return QualitygateInterpreterResult.of(new ParameterIssue(premise, callScope, valueOnStack));
                        
                        
                        
                    }
                    break;
                }
                
                
                
            
            }
        }
        
        
        //TODO delete later
//        if(scope.equals(CallScope.REQUEST)) {
//            
//            for (Entry<String, Object> e : this.context.getStack().currentStackFrame().getContents()) {
//            
////            System.out.println(numberOfLoops);
//            }
//        }
        

        
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
        
        if(taggedValues.get(0) != null) {
            InterpreterResult result = this.doSwitch(taggedValues.get(0));
//            if(result instanceof QualitygateInterpreterResult) {
//
//                for (QualitygateIssue e : ((QualitygateInterpreterResult) result).getQualitygateIssues()) {
//                    System.out.println("Premise is " + e.getPremise().getSpecification());
//                }
//                System.out.println(((QualitygateInterpreterResult) result).getQualitygateIssues().iterator());
//            }
//            
            return result;
        }

        return InterpreterResult.OK;
    }
    
    
}
