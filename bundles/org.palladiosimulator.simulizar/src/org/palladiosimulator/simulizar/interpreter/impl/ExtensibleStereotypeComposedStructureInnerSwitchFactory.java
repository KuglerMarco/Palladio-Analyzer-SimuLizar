package org.palladiosimulator.simulizar.interpreter.impl;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.emf.ecore.util.Switch;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitch;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitchContributionFactory;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.StereotypeComposedStructureInnerSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.StereotypeDispatchComposedStructureInnerSwitch;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitch.Factory;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;

import dagger.assisted.AssistedFactory;


public class ExtensibleStereotypeComposedStructureInnerSwitchFactory
        implements StereotypeComposedStructureInnerSwitchFactory {

    private final Provider<Set<ComposedStructureInnerSwitchContributionFactory>> elementFactoriesProvider;
    private final ComposedStructureInnerSwitch.Factory composedStructureInnerSwitchFactory;

    @Inject
    public ExtensibleStereotypeComposedStructureInnerSwitchFactory(Provider<Set<ComposedStructureInnerSwitchContributionFactory>> elementFactoriesProvider, ComposedStructureInnerSwitch.Factory composedStructureInnerSwitchFactory) {
        this.composedStructureInnerSwitchFactory = composedStructureInnerSwitchFactory;
        this.elementFactoriesProvider = elementFactoriesProvider;
    }

    @Override
    public Switch<InterpreterResult> create(InterpreterDefaultContext context, final Signature operationSignature,
            final RequiredRole requiredRole) {
        final  StereotypeDispatchComposedStructureInnerSwitch interpreter = new StereotypeDispatchComposedStructureInnerSwitch();
        interpreter.setDefaultSwitch(composedStructureInnerSwitchFactory.create(context, operationSignature, requiredRole, interpreter));
        
        var elementFactories = elementFactoriesProvider.get();
        if (elementFactories.isEmpty()) {
            throw new IllegalStateException("No ComposedStructureInnerSwitches are registered.");
        }
        elementFactories.stream().forEach(s -> interpreter.addSwitch(
                s.createComposedStructureInnerSwitch(context, interpreter, operationSignature, requiredRole)));
        
        return interpreter;
    }

}
