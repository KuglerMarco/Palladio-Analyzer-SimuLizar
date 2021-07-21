package org.palladiosimulator.simulizar.interpreter;


import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.qualitygate.util.QualitygateSwitch;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitchContributionFactory.ComposedStructureInnerSwitchElementDispatcher;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.modelversioning.emfprofile.Stereotype;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

public class StereotypeQualitygateSwitch extends QualitygateSwitch<InterpreterResult> implements StereotypeSwitch {

    @AssistedFactory
    public interface Factory extends ComposedStructureInnerSwitchContributionFactory {
        @Override
        StereotypeQualitygateSwitch createComposedStructureInnerSwitch(final InterpreterDefaultContext context, final ComposedStructureInnerSwitchElementDispatcher parentSwitch, final Signature operationSignature,
                final RequiredRole requiredRole);
    }
    
    public final String stereotypeName = "QualitygateElement";
    final InterpreterDefaultContext context;
    final ComposedStructureInnerSwitchElementDispatcher parentSwitch;
    final Signature operationSignature;
    final RequiredRole requiredRole;
    
    @AssistedInject
    StereotypeQualitygateSwitch(@Assisted final InterpreterDefaultContext context, @Assisted final ComposedStructureInnerSwitchElementDispatcher parentSwitch, @Assisted final Signature operationSignature,
            @Assisted final RequiredRole requiredRole){
        this.context = context;
        this.parentSwitch = parentSwitch;
        this.operationSignature = operationSignature;
        this.requiredRole = requiredRole;
        
    }
    
    @Override
    public InterpreterResult caseQualityGate(QualityGate object) {
        
        
        System.out.println("Qualitygate erkannt.");
        System.out.println(object.getScope());
   
        
        
        return InterpreterResult.OK;
    }

    @Override
    public boolean isSwitchForStereotype(Stereotype stereotype) {
        
        return stereotype.getName().equals(stereotypeName);
    }

    @Override
    public InterpreterResult handleStereotype(Stereotype stereotype, EObject theEObject) {
        
        EList<EObject> obj1 = StereotypeAPI.getTaggedValue(theEObject, "qualitygate", stereotype.getName());
        
        EObject obj2 = obj1.get(0);
        
        this.doSwitch(obj2);
        
        
        return InterpreterResult.OK;
    }
    
    
}
