package org.palladiosimulator.simulizar.qualitygate.eventbasedcommunication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.qualitygatemeasuringpoint.QualitygateMeasuringPoint;
import org.palladiosimulator.measurementframework.MeasuringValue;
import org.palladiosimulator.measurementframework.listener.IMeasurementSourceListener;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.probeframework.calculator.Calculator;
import org.palladiosimulator.probeframework.calculator.DefaultCalculatorProbeSets;
import org.palladiosimulator.probeframework.calculator.IGenericCalculatorFactory;
import org.palladiosimulator.probeframework.measurement.RequestContext;
import org.palladiosimulator.probeframework.probes.Probe;
import org.palladiosimulator.probeframework.probes.TriggeredProbe;
import org.palladiosimulator.simulizar.interpreter.listener.ModelElementPassedEvent;
import org.palladiosimulator.simulizar.qualitygate.metric.QualitygateMetricDescriptionConstants;
import org.palladiosimulator.simulizar.runtimestate.RuntimeStateEntityManager;
import org.palladiosimulator.simulizar.scopes.RuntimeExtensionScope;
import org.palladiosimulator.simulizar.utils.PCMPartitionManager;

import de.uka.ipd.sdq.simucomframework.model.SimuComModel;
import de.uka.ipd.sdq.simucomframework.probes.TakeCurrentSimulationTimeProbe;

@RuntimeExtensionScope
public class EventBasedCommunicationProbeRegistry implements RuntimeStateEntityManager, IMeasurementSourceListener {

    private static final int START_PROBE_INDEX = 0;
    private static final int STOP_PROBE_INDEX = 1;

    protected final SimuComModel simuComModel;
    protected final IGenericCalculatorFactory calculatorFactory;
    private final PCMPartitionManager pcmPartitionManager;
    
    private static MeasuringValue responseTime;

    private final Map<String, List<TriggeredProbe>> currentTimeProbes = new HashMap<String, List<TriggeredProbe>>();

    /**
     * @param modelAccessFactory
     *            Provides access to simulated models
     * @param simuComModel
     *            Provides access to the central simulation
     */
    @Inject
    public EventBasedCommunicationProbeRegistry(final PCMPartitionManager pcmPartitionManager,
            final SimuComModel simuComModel) {

        this.pcmPartitionManager = Objects.requireNonNull(pcmPartitionManager);
        this.calculatorFactory = Objects.requireNonNull(simuComModel)
            .getProbeFrameworkContext()
            .getGenericCalculatorFactory();
        this.simuComModel = simuComModel;

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected List<Probe> createStartAndStopProbe(final QualitygateMeasuringPoint measuringPoint,
            final SimuComModel simuComModel) {
        final List probeList = new ArrayList<TriggeredProbe>(2);
        probeList.add(new TakeCurrentSimulationTimeProbe(simuComModel.getSimulationControl()));
        probeList.add(new TakeCurrentSimulationTimeProbe(simuComModel.getSimulationControl()));
        this.currentTimeProbes.put
        (measuringPoint.getQualitygate()
            .getId(), Collections.unmodifiableList(probeList));
        return probeList;
    }

    /**
     * @param <T>
     * @param event
     */
    public void startMeasurement(final ModelElementPassedEvent<QualityGate> event) {

        
        if (!this.currentTimeProbes.containsKey(event.getModelElement()
            .getId())) {

            var probes = this.createStartAndStopProbe(this.findQualitygateMeasuringPoint(event.getModelElement()),
                    this.simuComModel);

            Calculator calc = this.calculatorFactory.buildCalculator(QualitygateMetricDescriptionConstants.PROCESSING_TIME_TUPLE,
                    this.findQualitygateMeasuringPoint(event.getModelElement()),
                    DefaultCalculatorProbeSets.createStartStopProbeConfiguration(probes.get(START_PROBE_INDEX),
                            probes.get(STOP_PROBE_INDEX)));
            calc.addObserver(this);

        }

        if (this.currentTimeProbes.containsKey(((Entity) event.getModelElement()).getId())
                && this.simulationIsRunning()) {
            this.currentTimeProbes.get(((Entity) event.getModelElement()).getId())
                .get(START_PROBE_INDEX)
                .takeMeasurement(this.calcMostParentContext(event.getThread()
                        .getRequestContext()));
            
            
        }
        
    }

    public MeasuringValue endMeasurement(final ModelElementPassedEvent<QualityGate> event) {
        if (this.currentTimeProbes.containsKey(((Entity) event.getModelElement()).getId())
                && this.simulationIsRunning()) {
            this.currentTimeProbes.get(((Entity) event.getModelElement()).getId())
                .get(STOP_PROBE_INDEX)
                .takeMeasurement(this.calcMostParentContext(event.getThread()
                    .getRequestContext()));
            
        }
        
        return responseTime;
        
    }

    private boolean simulationIsRunning() {
        return this.simuComModel.getSimulationControl()
            .isRunning();
    }

    private QualitygateMeasuringPoint findQualitygateMeasuringPoint(QualityGate qualitygate) {

        MonitorRepository monitorRepositoryModel = this.pcmPartitionManager
            .findModel(MonitorRepositoryPackage.eINSTANCE.getMonitorRepository());

        for (Monitor monitor : monitorRepositoryModel.getMonitors()) {

            if (monitor.getMeasuringPoint() instanceof QualitygateMeasuringPoint) {
                if (((QualitygateMeasuringPoint) monitor.getMeasuringPoint()).getQualitygate()
                    .getId()
                    .equals(qualitygate.getId())) {
                    return ((QualitygateMeasuringPoint) monitor.getMeasuringPoint());

                }
            }

        }

        return null;

    }
    
    @Override
    public void newMeasurementAvailable(MeasuringValue newMeasurement) {

        responseTime = (newMeasurement.getMeasuringValueForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC));

    }

    @Override
    public void preUnregister() {
        // TODO Auto-generated method stub
        
    }
    
    /**
     * To handle fork actions.
     * 
     * @param context
     * @return
     */
    private RequestContext calcMostParentContext(RequestContext context) {
        
        RequestContext result = context;
        
        while(result.getParentContext() != null) {
            result = result.getParentContext();
        }
        
        return result;
        
    }


}
