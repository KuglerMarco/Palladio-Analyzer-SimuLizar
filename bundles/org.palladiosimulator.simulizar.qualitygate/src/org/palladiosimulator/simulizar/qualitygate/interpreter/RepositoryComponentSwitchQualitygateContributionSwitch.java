package org.palladiosimulator.simulizar.qualitygate.interpreter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.modelversioning.emfprofile.Stereotype;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.probeframework.ProbeFrameworkContext;
import org.palladiosimulator.simulizar.interpreter.CallScope;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.RepositoryComponentSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.RepositoryComponentSwitchStereotypeContributionFactory.RepositoryComponentSwitchStereotypeElementDispatcher;
import org.palladiosimulator.simulizar.interpreter.StereotypeSwitch;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResultMerger;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

public class RepositoryComponentSwitchQualitygateContributionSwitch implements StereotypeSwitch {

    @AssistedFactory
    public interface Factory extends RepositoryComponentSwitchStereotypeContributionFactory {

        @Override
        RepositoryComponentSwitchQualitygateContributionSwitch create(final InterpreterDefaultContext context,
                final AssemblyContext assemblyContext, final Signature signature, final ProvidedRole providedRole,
                RepositoryComponentSwitchStereotypeElementDispatcher parentSwitch);

    }

    // Information about the stereotype it is processing
    private final String stereotypeName = "QualitygateElement";
    private final String profileName = "QualitygateProfile";

    // Information about the simulation-context
    private final InterpreterDefaultContext context;
    private final Signature operationSignature;

    private final StereotypeQualitygateSwitch.Factory stereotypeQualitygateSwitchFactory;

    private ProbeFrameworkContext frameworkContext;

    private static final Logger LOGGER = Logger.getLogger(StereotypeQualitygateSwitch.class);
    private final BasicInterpreterResultMerger merger;

    @AssistedInject
    public RepositoryComponentSwitchQualitygateContributionSwitch(@Assisted final InterpreterDefaultContext context,
            @Assisted final AssemblyContext assemblyContext, @Assisted final Signature signature,
            @Assisted final ProvidedRole providedRole,
            @Assisted RepositoryComponentSwitchStereotypeElementDispatcher parentSwitch,
            BasicInterpreterResultMerger merger, ProbeFrameworkContext frameworkContext,
            StereotypeQualitygateSwitch.Factory stereotypeQualitygateSwitchFactory) {

        this.merger = merger;
        this.context = context;
        this.operationSignature = signature;
        this.frameworkContext = frameworkContext;

        this.stereotypeQualitygateSwitchFactory = stereotypeQualitygateSwitchFactory;

        LOGGER.setLevel(Level.DEBUG);
    }

    @Override
    public boolean isSwitchForStereotype(Stereotype stereotype) {

        boolean result = stereotype.getProfile()
            .getName()
            .equals(profileName);
        if (result) {
            return stereotype.getName()
                .equals(stereotypeName);
        }
        return result;
    }

    @Override
    public InterpreterResult handleStereotype(Stereotype stereotype, EObject theEObject, CallScope callScope) {
        InterpreterResult result = InterpreterResult.OK;


        EList<QualityGate> taggedValues = StereotypeAPI.getTaggedValue(theEObject, "qualitygate", stereotype.getName());

        // Model validation
        if (taggedValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "Qualitygate-Model not valid: Qualitygate-Stereotype needs to have at least one Qualitygate element.");
        }

        // Processing all the attached Qualitygates
        for (QualityGate e : taggedValues) {


            result = merger.merge(result,
                    this.stereotypeQualitygateSwitchFactory
                        .createStereotypeSwitch(context, operationSignature, callScope, theEObject)
                        .doSwitch(e));

        }
        return result;
    }

    @Override
    public String getStereotypeName() {
        return this.stereotypeName;
    }

    @Override
    public String getProfileName() {
        return this.profileName;
    }

}
