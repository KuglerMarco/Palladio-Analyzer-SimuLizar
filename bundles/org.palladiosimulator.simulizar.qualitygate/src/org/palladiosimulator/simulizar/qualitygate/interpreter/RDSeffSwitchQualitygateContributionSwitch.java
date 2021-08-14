package org.palladiosimulator.simulizar.qualitygate.interpreter;

import java.util.Collection;
import java.util.Stack;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.modelversioning.emfprofile.Stereotype;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.qualitygate.RequestMetricScope;
import org.palladiosimulator.failuremodel.qualitygate.RequestParameterScope;
import org.palladiosimulator.failuremodel.qualitygate.ResultParameterScope;
import org.palladiosimulator.failuremodel.qualitygate.util.QualitygateSwitch;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.measurementframework.MeasuringValue;
import org.palladiosimulator.measurementframework.listener.IMeasurementSourceListener;
import org.palladiosimulator.metricspec.MetricDescription;
import org.palladiosimulator.metricspec.MetricDescriptionRepository;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.pcm.seff.ExternalCallAction;
import org.palladiosimulator.pcmmeasuringpoint.ExternalCallActionMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.SystemOperationMeasuringPoint;
import org.palladiosimulator.probeframework.ProbeFrameworkContext;
import org.palladiosimulator.probeframework.calculator.Calculator;
import org.palladiosimulator.simulizar.interpreter.CallScope;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.RDSeffSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.StereotypeSwitch;
import org.palladiosimulator.simulizar.interpreter.RDSeffSwitchStereotypeContributionFactory.RDSeffSwitchElementDispatcher;
import org.palladiosimulator.simulizar.interpreter.RepositoryComponentSwitchStereotypeContributionFactory.RepositoryComponentSwitchStereotypeElementDispatcher;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResultMerger;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.ParameterIssue;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.ResponseTimeProxyIssue;
import org.palladiosimulator.simulizar.utils.PCMPartitionManager;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

public class RDSeffSwitchQualitygateContributionSwitch extends QualitygateSwitch<InterpreterResult> implements StereotypeSwitch, IMeasurementSourceListener  {

    @AssistedFactory
    public interface Factory extends RDSeffSwitchStereotypeContributionFactory {

        @Override
        RDSeffSwitchQualitygateContributionSwitch create(final InterpreterDefaultContext context,
                RDSeffSwitchElementDispatcher parentSwitch);

    }

    // Information about the stereotype it is processing
    private final String stereotypeName = "QualitygateElement";
    private final String profileName = "QualitygateProfile";

    
    // Information about the simulation-context
    private final InterpreterDefaultContext context;

    private final StereotypeQualitygateSwitch.Factory stereotypeQualitygateSwitchFactory;

    private ProbeFrameworkContext frameworkContext;

    private static final Logger LOGGER = Logger.getLogger(StereotypeQualitygateSwitch.class);

    // TODO wirklich BasicInterpreter?
    private final BasicInterpreterResultMerger merger;
    
    private Stack<MeasuringValue> responseTime;
    private QualityGate qualitygate;
    private PCMRandomVariable premise;
    private EObject stereotypedObject;
    private Signature operationSignature;
    
    private final PCMPartitionManager partManager;
    
    private CallScope callScope = CallScope.REQUEST;

    @AssistedInject
    public RDSeffSwitchQualitygateContributionSwitch(@Assisted final InterpreterDefaultContext context,
            @Assisted RDSeffSwitchElementDispatcher parentSwitch, BasicInterpreterResultMerger merger,
            ProbeFrameworkContext frameworkContext,
            StereotypeQualitygateSwitch.Factory stereotypeQualitygateSwitchFactory, PCMPartitionManager partManager) {

        this.merger = merger;
        this.context = context;
        this.frameworkContext = frameworkContext;

        this.stereotypeQualitygateSwitchFactory = stereotypeQualitygateSwitchFactory;
        this.partManager = partManager;

        LOGGER.setLevel(Level.DEBUG);
        responseTime = new Stack<MeasuringValue>();
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
        
        this.operationSignature = ((ExternalCallAction) theEObject).getCalledService_ExternalService();
        this.stereotypedObject = theEObject;
        this.callScope = callScope;

        EList<QualityGate> taggedValues = StereotypeAPI.getTaggedValue(theEObject, "qualitygate", stereotype.getName());

        // Model validation
        if (taggedValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "Qualitygate-Model not valid: Qualitygate-Stereotype needs to have at least one Qualitygate element.");
        }

        // Processing all the attached Qualitygates
        for (QualityGate e : taggedValues) {

            LOGGER.debug("RepositoryCompoonent: " + e.getPremise()
                .getSpecification());

            if (theEObject instanceof ExternalCallAction) {
                result = merger.merge(result, this
                    .doSwitch(e));
            } else {
                throw new IllegalStateException("You might wanted to attach the Qualitygate to the ExternalCallAction.");
            }

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

    @Override
    public void newMeasurementAvailable(MeasuringValue newMeasurement) {
        responseTime.add(newMeasurement.getMeasuringValueForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC));
        LOGGER.debug(responseTime.size());
        
    }

    @Override
    public void preUnregister() {
        // TODO Auto-generated method stub
        
    }
    
    /**
     * Processing the attached Qualitygate, Premise and Scope
     */
    @Override
    public InterpreterResult caseQualityGate(QualityGate qualitygate) {
        this.qualitygate = qualitygate;
        premise = qualitygate.getPremise();
        return this.doSwitch(qualitygate.getScope());

    }
    
    
    @Override
    public InterpreterResult caseRequestMetricScope(RequestMetricScope object) {

        // Registering at the Calculator in Request-Scope
        if (callScope.equals(CallScope.REQUEST)) {

            // TODO MetricDescription laden (REsponseTime)
            // Loading CommonMetrics-model
            URI uri = URI.createURI("pathmap://METRIC_SPEC_MODELS/models/commonMetrics.metricspec");
            MetricDescriptionRepository res = (MetricDescriptionRepository) partManager.getBlackboard()
                .getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
                .getResourceSet()
                .getResource(uri, false)
                .getContents()
                .get(0);

            // Searching for the Measuring-Point
            MeasuringPointRepository measuringPointRepo = (MeasuringPointRepository) partManager
                .findModel(MeasuringpointPackage.Literals.MEASURING_POINT_REPOSITORY);
//            MeasuringPointRepository measuringPointRepo = (MeasuringPointRepository) partManager.getBlackboard()
//                .getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
//                .getElement(MeasuringpointPackage.Literals.MEASURING_POINT_REPOSITORY)
//                .get(0);

            MeasuringPoint measPoint = null;

            for (MeasuringPoint e : measuringPointRepo.getMeasuringPoints()) {
//                if (e instanceof SystemOperationMeasuringPoint) {
//                    if (((SystemOperationMeasuringPoint) e).getOperationSignature()
//                        .equals(object.getSignature())
//                            && ((SystemOperationMeasuringPoint) e).getRole()
//                                .equals(stereotypedObject)) {
//
//                        measPoint = (SystemOperationMeasuringPoint) e;
//
//                    }
//                }
                if (e instanceof ExternalCallActionMeasuringPoint) {
                    if (((ExternalCallActionMeasuringPoint) e).getExternalCall().equals(stereotypedObject)) {

                        measPoint = (ExternalCallActionMeasuringPoint) e;

                    }
                }
            }

            if (measPoint == null) {
                throw new IllegalStateException(
                        "No MeasuringPoint found in MeasuringPointRepository for this Qualitygate.");
            }

            MetricDescription respTimeMetricDesc = res.getMetricDescriptions()
                .stream()
                .filter(e -> e.getName()
                    .equals("Response Time"))
                .findFirst()
                .orElse(null);

            if (respTimeMetricDesc == null) {
                throw new IllegalStateException("MEtricDescription not loadable.");
            }

            // Calculator for this Qualitygate TODO Noch MetricDescription laden
            Collection<Calculator> calculator = frameworkContext.getCalculatorRegistry()
                .getCalculatorsForMeasuringPoint(measPoint);

            LOGGER.debug("MeasuringPoint is: " + measPoint.getStringRepresentation());

            Calculator calc = calculator.stream()
                .findFirst()
                .orElse(null);
            
            // TODO schöner
            try {
                calc.addObserver(this);
            } catch (IllegalArgumentException e) {
                
            }
            
            

            LOGGER.debug(calc.toString());

            return InterpreterResult.OK;

        } else {
            // TODO Checking the Value on the Stack in Response-Scope
            LOGGER.debug("Stack size: " + responseTime.size());
            return InterpreterResult.of(new ResponseTimeProxyIssue(premise, this));
            
//            LOGGER.debug(responseTime.firstElement()
//                .asArray());
        }

//        return InterpreterResult.OK;
    }
    
    
    /**
     * Checking the values on the parameter-stack against the premise-specification within the
     * Qualitygate.
     */
    @Override
    public InterpreterResult caseRequestParameterScope(RequestParameterScope object) {

        Signature signatureOfQualitygate = object.getSignature();

        if (stereotypedObject instanceof ExternalCallAction) {

            // TODO: Build Stack to evaluate against which will be evaluated
            
            
            LOGGER.debug("!!!ExternalCall");

        } else if (callScope.equals(CallScope.REQUEST) && (signatureOfQualitygate == (this.operationSignature))) {

            if (!((boolean) context.evaluate(premise.getSpecification(), this.context.getStack()
                .currentStackFrame()))) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StoEx is broken: " + premise.getSpecification() + " because stackframe is: "
                            + this.context.getStack()
                                .currentStackFrame()
                                .toString());
                }

                return BasicInterpreterResult.of(new ParameterIssue((Entity) this.stereotypedObject, this.qualitygate,
                        this.context.getStack()
                            .currentStackFrame()
                            .getContents()));

            }

        }
        return InterpreterResult.OK;

    }
    
    /**
     * Checking the values on the parameter-stack against the premise-specification within the
     * Qualitygate.
     */
    @Override
    public InterpreterResult caseResultParameterScope(ResultParameterScope object) {

        Signature signatureOfQualitygate = object.getSignature();

        if (callScope.equals(CallScope.RESPONSE) && (signatureOfQualitygate == (this.operationSignature))) {
            if (!((boolean) context.evaluate(premise.getSpecification(), this.context.getCurrentResultFrame()))) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StoEx is broken: " + premise.getSpecification()
                            + " because resultframe is: " + this.context.getCurrentResultFrame()
                                .toString());
                }

                return BasicInterpreterResult.of(new ParameterIssue((Entity) this.stereotypedObject, this.qualitygate,
                        this.context.getCurrentResultFrame()
                            .getContents()));
            }
        }
        return InterpreterResult.OK;
    }
    
    
    public MeasuringValue getLastMeasure() {
        return this.responseTime.firstElement();
    }
    
    

}
