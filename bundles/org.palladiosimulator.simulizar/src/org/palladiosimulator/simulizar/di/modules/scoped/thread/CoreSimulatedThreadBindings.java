package org.palladiosimulator.simulizar.di.modules.scoped.thread;

import java.util.Set;

import org.palladiosimulator.simulizar.interpreter.ComposedRDSeffSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitch;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.RDSeffPerformanceSwitch;
import org.palladiosimulator.simulizar.interpreter.RDSeffSwitch;
import org.palladiosimulator.simulizar.interpreter.RDSeffSwitchContributionFactory;
import org.palladiosimulator.simulizar.interpreter.RepositoryComponentSwitch;
import org.palladiosimulator.simulizar.interpreter.RepositoryComponentSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.impl.ExtensibleComposedRDSeffSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultMerger;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResultMerger;
import org.palladiosimulator.simulizar.interpreter.stereotype.StereotypeDispatchRepositoryComponentSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.stereotype.impl.ExtensibleStereotypeComposedStructureInnerSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.stereotype.impl.ExtensibleStereotypeDispatchRDSeffSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.stereotype.impl.ExtensibleStereotypeDispatchRepositoryComponentSwitchFactory;
import org.palladiosimulator.simulizar.scopes.SimulatedThreadScope;

import com.google.common.collect.ImmutableSet;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;

@Module
public interface CoreSimulatedThreadBindings {

    @Binds
    @SimulatedThreadScope
    InterpreterResultMerger bindResultMerger(BasicInterpreterResultMerger impl);

    @Binds
    @SimulatedThreadScope
    @StandardSwitch
    ComposedRDSeffSwitchFactory bindComposedRDSeffSwitchFactory(ExtensibleComposedRDSeffSwitchFactory impl);
    
    @Binds
    @SimulatedThreadScope
    ComposedRDSeffSwitchFactory bindExtensibleStereotypeDispatchRDSeffSwitchFactory(ExtensibleStereotypeDispatchRDSeffSwitchFactory impl);

    @Binds
    @SimulatedThreadScope
    @StandardSwitch
    ComposedStructureInnerSwitchFactory bindComposedStructureInnerSwitchFactory(
            ComposedStructureInnerSwitch.Factory impl);

    @Binds
    @SimulatedThreadScope
    ComposedStructureInnerSwitchFactory bindStereotypeDispatchComposedStructureInnerSwitchFactory(
            ExtensibleStereotypeComposedStructureInnerSwitchFactory impl);

    @Binds
    @SimulatedThreadScope
    @StandardSwitch
    RepositoryComponentSwitchFactory bindRepositoryComponentSwitchFactory(RepositoryComponentSwitch.Factory impl);

    @Binds
    @SimulatedThreadScope
    RepositoryComponentSwitchFactory bindExtensibleStereotypeDispatchRepositoryComponentSwitchFactory(
            ExtensibleStereotypeDispatchRepositoryComponentSwitchFactory impl);

    @Provides
    @ElementsIntoSet
    static Set<RDSeffSwitchContributionFactory> provideCoreRDSeffSwitchFactories(
            RDSeffPerformanceSwitch.Factory performanceSwitchFactory, RDSeffSwitch.Factory rdseffSwitchFactory) {
        return ImmutableSet.of(rdseffSwitchFactory, performanceSwitchFactory);
    }


}
