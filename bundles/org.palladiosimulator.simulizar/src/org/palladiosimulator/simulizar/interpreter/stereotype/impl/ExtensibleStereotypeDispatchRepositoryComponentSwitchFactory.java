package org.palladiosimulator.simulizar.interpreter.stereotype.impl;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.emf.ecore.util.Switch;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.di.modules.scoped.thread.StandardSwitch;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.RepositoryComponentSwitch;
import org.palladiosimulator.simulizar.interpreter.RepositoryComponentSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandlerDispatchFactory;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultMerger;
import org.palladiosimulator.simulizar.interpreter.stereotype.RepositoryComponentSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.stereotype.StereotypeDispatchRepositoryComponentSwitch;


/**
 * Factory for the StereotypeDispatchRepositoryComponentSwitch.
 * 
 * @author Marco Kugler
 *
 */
public class ExtensibleStereotypeDispatchRepositoryComponentSwitchFactory implements RepositoryComponentSwitchFactory {

    private final Provider<Set<RepositoryComponentSwitchStereotypeContributionFactory>> elementFactoriesProvider;

    private final RepositoryComponentSwitchFactory repositoryComponentSwitchFactory;

    private final InterpreterResultMerger merger;

    private final InterpreterResultHandler handler;

    @Inject
    public ExtensibleStereotypeDispatchRepositoryComponentSwitchFactory(
            Provider<Set<RepositoryComponentSwitchStereotypeContributionFactory>> elementFactoriesProvider,
            @StandardSwitch RepositoryComponentSwitchFactory repositoryComponentSwitchFactory, InterpreterResultMerger merger,
            InterpreterResultHandlerDispatchFactory handler) {
        
        this.repositoryComponentSwitchFactory = repositoryComponentSwitchFactory;
        this.elementFactoriesProvider = elementFactoriesProvider;
        this.merger = merger;
        this.handler = handler.create();
        
    }

    /**
     * Creates the StereotypeDispatchRepositoryComponentSwitch.
     */
    @Override
    public Switch<InterpreterResult> create(InterpreterDefaultContext context, AssemblyContext assemblyContext,
            Signature signature, ProvidedRole providedRole) {

        StereotypeDispatchRepositoryComponentSwitch interpreter = new StereotypeDispatchRepositoryComponentSwitch(
                merger, handler, (RepositoryComponentSwitch) repositoryComponentSwitchFactory.create(context,
                        assemblyContext, signature, providedRole));

        var elementFactories = elementFactoriesProvider.get();
        elementFactories.stream()
            .forEach(s -> interpreter
                .addSwitch(s.create(context, assemblyContext, signature, providedRole, interpreter)));

        return interpreter;
    }
    
    
    
}
