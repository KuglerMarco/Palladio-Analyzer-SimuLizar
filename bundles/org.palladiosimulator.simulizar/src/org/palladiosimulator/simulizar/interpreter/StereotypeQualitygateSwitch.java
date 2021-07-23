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
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitchContributionFactory.ComposedStructureInnerSwitchElementDispatcher;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
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
    public interface Factory extends ComposedStructureInnerSwitchContributionFactory {
        @Override
        StereotypeQualitygateSwitch createComposedStructureInnerSwitch(final InterpreterDefaultContext context, final ComposedStructureInnerSwitchElementDispatcher parentSwitch, final Signature operationSignature,
                final RequiredRole requiredRole);
    }
    
    //TODO Deklarationen richtig
    public final String stereotypeName = "QualitygateElement";
    final InterpreterDefaultContext context;
    final ComposedStructureInnerSwitchElementDispatcher parentSwitch;
    final Signature operationSignature;
    final RequiredRole requiredRole;
    
    InterpreterResult interpreterResult = InterpreterResult.OK;
    
    /*
     * Qualitygate-Information
     */
    Scope qualitygateScope = null;
    PCMRandomVariable premise = null;
    //Request or Response-Processing?
    CallScope callScope = CallScope.REQUEST;
    
    
    @AssistedInject
    StereotypeQualitygateSwitch(@Assisted final InterpreterDefaultContext context, @Assisted final ComposedStructureInnerSwitchElementDispatcher parentSwitch, @Assisted final Signature operationSignature,
            @Assisted final RequiredRole requiredRole){
        
        this.context = context;
        
        //TODO Nötig?
        this.parentSwitch = parentSwitch;
        this.operationSignature = operationSignature;
        this.requiredRole = requiredRole;
        
    }
    

    /**
     * Extracting the necessary information of the QualityGate.
     */
    @Override
    public InterpreterResult caseQualityGate(QualityGate object) {
        
        interpreterResult = InterpreterResult.OK;
        
        //Initializing the Qualitygate-Information
        qualitygateScope = object.getScope();
        premise = object.getPremise();
        
        //TODO delete later
        System.out.println("Qualitygate erkannt.");
        System.out.println(premise.getSpecification());
        
        interpreterResult = this.doSwitch(object.getScope());

        return interpreterResult;
    }
    
    
    @Override
    public InterpreterResult caseRequestParameterScope(RequestParameterScope object) {
        
        interpreterResult = InterpreterResult.OK;
        
        String[] splittedParameter = premise.getSpecification().split(" ");
        
        String parameterSpecification = splittedParameter[0];
        String operator = splittedParameter[1];
        int premiseValue = Integer.parseInt(splittedParameter[2]);
        
        //Checking the processing time of the Qualitygate: Request or Response?
        if(callScope.equals(CallScope.REQUEST)) {
            
            
            //Checking whether Parameter is on Stack 
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
                        
                        //TODO wenn kleiner, dann Fehlerhistorie anlegen (Identifikation des Qualitygates?)
                        
                        //TODO als Methode auslagern
                        
                        //TODO delete later
                        System.out.println("Breaking Qualitygate");
                        
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
        

        
        return interpreterResult;
        
    }
    


    /**
     * Returns whether Switch is for Stereotype
     */
    @Override
    public boolean isSwitchForStereotype(Stereotype stereotype) {
        
        return stereotype.getName().equals(stereotypeName);
        
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
        
        if(!taggedValues.get(0).equals(null)) {
            return this.doSwitch(taggedValues.get(0));
        }

        return interpreterResult;
    }
    
    
}
