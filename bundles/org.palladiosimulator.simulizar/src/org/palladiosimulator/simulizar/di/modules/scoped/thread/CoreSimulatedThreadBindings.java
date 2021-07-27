package org.palladiosimulator.simulizar.di.modules.scoped.thread;

import java.util.Set;

import org.palladiosimulator.simulizar.interpreter.ComposedRDSeffSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.RDSeffPerformanceSwitch;
import org.palladiosimulator.simulizar.interpreter.RDSeffSwitch;
import org.palladiosimulator.simulizar.interpreter.RDSeffSwitchContributionFactory;
import org.palladiosimulator.simulizar.interpreter.StereotypeComposedStructureInnerSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.StereotypeQualitygateSwitch;
import org.palladiosimulator.simulizar.interpreter.impl.ExtensibleComposedRDSeffSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.impl.ExtensibleStereotypeComposedStructureInnerSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultMerger;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResultMerger;
import org.palladiosimulator.simulizar.interpreter.result.impl.NoIssuesHandler;
import org.palladiosimulator.simulizar.interpreter.result.impl.QualitygateInterpreterResultMerger;
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
    InterpreterResultHandler bindIssuesHandler(NoIssuesHandler impl);
    
    @Binds
    @SimulatedThreadScope
    InterpreterResultMerger bindResultMerger(QualitygateInterpreterResultMerger impl);
    
    @Binds
    @SimulatedThreadScope
    ComposedRDSeffSwitchFactory bindComposedRDSeffSwitchFactory(ExtensibleComposedRDSeffSwitchFactory impl);
    
    @Binds
    @SimulatedThreadScope
    StereotypeComposedStructureInnerSwitchFactory bindStereotypeComposedStructureInnerSwitchFactory(ExtensibleStereotypeComposedStructureInnerSwitchFactory impl);


    @Provides
    @ElementsIntoSet
    static Set<RDSeffSwitchContributionFactory> provideCoreRDSeffSwitchFactories(
            RDSeffPerformanceSwitch.Factory performanceSwitchFactory, RDSeffSwitch.Factory rdseffSwitchFactory) {
        return ImmutableSet.of(rdseffSwitchFactory, performanceSwitchFactory);
    }
    
    @Provides
    @ElementsIntoSet
    static Set<ComposedStructureInnerSwitchStereotypeContributionFactory> provideStereotypeComposedStructureInnerSwitchFactories(
            StereotypeQualitygateSwitch.Factory stereotypeQualityGateSwitchFactory) {
        return ImmutableSet.of(stereotypeQualityGateSwitchFactory);
    }
}
