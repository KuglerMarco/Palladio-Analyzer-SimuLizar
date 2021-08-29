package org.palladiosimulator.simulizar.qualitygate.metric;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.palladiosimulator.metricspec.MetricSetDescription;
import org.palladiosimulator.metricspec.MetricSpecPackage;
import org.palladiosimulator.metricspec.TextualBaseMetricDescription;

import org.palladiosimulator.metricspec.Identifier;


public final class QualitygateMetricDescriptionConstants {
    
    // TODO Namen ändern
    public static final String PATHMAP_METRIC_SPEC_MODELS_COMMON_METRICS_METRICSPEC = "file:/C:/Users/Public/WorkspaceFailureScenario/Palladio-Analyzer-SimuLizar/bundles/org.palladiosimulator.simulizar.qualitygate/src/org/palladiosimulator/simulizar/qualitygate/metric/qualitygate.metricspec";
    public static final String CLASSPATH_RELATIVE_COMMON_METRICS_METRICSPEC = "qualitygate.metricspec";
    
    private static final Map<?, ?> OPTIONS = Collections.emptyMap();

    public final static TextualBaseMetricDescription QUALITYGATE_VIOLATION_METRIC;
    
    public final static MetricSetDescription QUALITYGATE_VIOLATION_METRIC_OVER_TIME;
    
    public final static Identifier SUCCESS;
    
    public final static Identifier VIOLATION;
    
    public final static TextualBaseMetricDescription SEVERITY_METRIC;
    
    public final static MetricSetDescription SEVERITY_METRIC_OVER_TIME;
    
    public final static Identifier NO_SAFETY_EFFECT;
    
    public final static Identifier MINOR;
    
    public final static Identifier MAJOR;
    
    public final static Identifier HAZARDOUS;
    
    public final static Identifier CATASTROPHIC ;
    
    static {
        
        final ResourceSet resourceSet = new ResourceSetImpl();
        
        if (!Platform.isRunning()) {
            //Trigger package initialization
            MetricSpecPackage.eINSTANCE.getEFactoryInstance();
            
            //Register the ResourceFactory
            resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .putIfAbsent("qualitygate", new XMIResourceFactoryImpl());
            
            //If we are running in standalone mode, the dependencies, and consequently the
            // common metrics model, are available directly on the classpath.
            URL res = Optional.ofNullable(Thread.currentThread()
                .getContextClassLoader()
                .getResource(CLASSPATH_RELATIVE_COMMON_METRICS_METRICSPEC))
                .orElseThrow(() -> new IllegalStateException(
                        "You are running in standalone mode. Make sure the bundle \"org.palladiosimulator.metricspec.resource\" is on the class path."));
            resourceSet.getURIConverter().getURIMap().putIfAbsent(
                    URI.createURI(PATHMAP_METRIC_SPEC_MODELS_COMMON_METRICS_METRICSPEC),
                    URI.createURI(res.toString()));
        }
        
        final Resource resource = resourceSet
                .createResource(URI.createURI(PATHMAP_METRIC_SPEC_MODELS_COMMON_METRICS_METRICSPEC, true));
        try {
            resource.load(OPTIONS);
        } catch (final IOException e) {
            // TODO Auto-generated catch block. Use eclipse error log instead?
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        
        QUALITYGATE_VIOLATION_METRIC = (TextualBaseMetricDescription) resource.getEObject("_biPckAJhEeyupI8CZJs1wg");
        
        QUALITYGATE_VIOLATION_METRIC_OVER_TIME = (MetricSetDescription) resource.getEObject("_CBWPIQIHEeyBwtsHp4B1og");
        
        SUCCESS = (Identifier) resource.getEObject("_inOiQAJhEeyupI8CZJs1wg");
        
        VIOLATION = (Identifier) resource.getEObject("_k0dO0AJhEeyupI8CZJs1wg");
        
        
        
        SEVERITY_METRIC = (TextualBaseMetricDescription) resource.getEObject("_bnYmwAf9Eey9TNWaDr7jMQ");
        
        SEVERITY_METRIC_OVER_TIME = (MetricSetDescription) resource.getEObject("_rU4aYQf9Eey9TNWaDr7jMQ");
        
        NO_SAFETY_EFFECT = (Identifier) resource.getEObject("_uPp3gAf_Eey9TNWaDr7jMQ");
        
        MINOR = (Identifier) resource.getEObject("_ul3YEAf_Eey9TNWaDr7jMQ");
        
        MAJOR = (Identifier) resource.getEObject("_u5amMAf_Eey9TNWaDr7jMQ");
        
        HAZARDOUS = (Identifier) resource.getEObject("_vLQkIAf_Eey9TNWaDr7jMQ");
        
        CATASTROPHIC = (Identifier) resource.getEObject("_vfOB8Af_Eey9TNWaDr7jMQ");
        
        
    }
    
    /**
     * Private constructor to forbid instantiation.
     */
    private QualitygateMetricDescriptionConstants() {
    }

}
