package org.palladiosimulator.simulizar.interpreter;

import javax.inject.Inject;

import org.eclipse.emf.ecore.util.Switch;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.qualitygate.util.QualitygateSwitch;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitchContributionFactory.ComposedStructureInnerSwitchElementDispatcher;
import org.palladiosimulator.simulizar.interpreter.RDSeffSwitchContributionFactory.RDSeffElementDispatcher;
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
    
    public final String stereotypeName = "Qualitygate";
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
        
        
        
        
        return InterpreterResult.OK;
    }

    @Override
    public boolean isSwitchForStereotype(Stereotype stereotype) {
        
        return stereotype.getName() == this.stereotypeName;
    }
    
    
}
