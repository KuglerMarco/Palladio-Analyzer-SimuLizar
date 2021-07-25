package org.palladiosimulator.simulizar.interpreter.impl;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.emf.ecore.util.Switch;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitch;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.StereotypeComposedStructureInnerSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.StereotypeDispatchComposedStructureInnerSwitch;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;


public class ExtensibleStereotypeComposedStructureInnerSwitchFactory
        implements StereotypeComposedStructureInnerSwitchFactory {

    /**
     * Includes the registered ComposedStructureInnerSwitchContributionFactory by Dagger.
     */
    private final Provider<Set<ComposedStructureInnerSwitchStereotypeContributionFactory>> elementFactoriesProvider;
    
    /**
     * Factory for the default ComposedStructureInnerSwitch, which needs be set in the StereotypeDispatchComposedStructureInnerSwitch.
     */
    private final ComposedStructureInnerSwitch.Factory composedStructureInnerSwitchFactory;

    /**
     * Dagger creates the set of ComposedStructureInnerSwitchContributionFactorys and the ComposedStructureInnerSwitch.
     * 
     * @param elementFactoriesProvider
     * @param composedStructureInnerSwitchFactory
     */
    @Inject
    public ExtensibleStereotypeComposedStructureInnerSwitchFactory(Provider<Set<ComposedStructureInnerSwitchStereotypeContributionFactory>> elementFactoriesProvider, ComposedStructureInnerSwitch.Factory composedStructureInnerSwitchFactory) {
        this.composedStructureInnerSwitchFactory = composedStructureInnerSwitchFactory;
        this.elementFactoriesProvider = elementFactoriesProvider;
    }

    
    
    /**
     * Creates the ComposedStructureInnerSwitch.
     */
    @Override
    public Switch<InterpreterResult> create(InterpreterDefaultContext context, final Signature operationSignature,
            final RequiredRole requiredRole) {
        
        //TODO Factory?
        final  StereotypeDispatchComposedStructureInnerSwitch interpreter = new StereotypeDispatchComposedStructureInnerSwitch(context);
        
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
