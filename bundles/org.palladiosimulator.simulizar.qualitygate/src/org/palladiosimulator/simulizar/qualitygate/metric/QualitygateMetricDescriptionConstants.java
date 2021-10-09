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
import org.palladiosimulator.metricspec.NumericalBaseMetricDescription;
import org.palladiosimulator.metricspec.TextualBaseMetricDescription;

import org.palladiosimulator.metricspec.Identifier;


/**
 * MetricDescription for Qualitygate violations.
 * 
 * @author Marco Kugler
 *
 */
public final class QualitygateMetricDescriptionConstants {
    
    public static final String PATHMAP_METRIC_SPEC_QUALITYGATE_METRICS_METRICSPEC = "platform:/plugin/org.palladiosimulator.simulizar.qualitygate/model/qualitygate.metricspec";
    public static final String CLASSPATH_RELATIVE_QUALITYGATE_METRICSPEC = "qualitygate.metricspec";
    
    private static final Map<?, ?> OPTIONS = Collections.emptyMap();

    public final static TextualBaseMetricDescription QUALITYGATE_VIOLATION_METRIC;
    
    public final static MetricSetDescription QUALITYGATE_VIOLATION_METRIC_OVER_TIME;
    
    public final static Identifier SUCCESS;
    
    public final static Identifier VIOLATION;
    
    public final static Identifier CRASH;
    
    public final static NumericalBaseMetricDescription PROCESSING_TIME;
    
    public final static MetricSetDescription PROCESSING_TIME_TUPLE;
    
    
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
                .getResource(CLASSPATH_RELATIVE_QUALITYGATE_METRICSPEC))
                .orElseThrow(() -> new IllegalStateException(
                        "You are running in standalone mode. Make sure the bundle \"org.palladiosimulator.metricspec.resource\" is on the class path."));
            resourceSet.getURIConverter().getURIMap().putIfAbsent(
                    URI.createURI(PATHMAP_METRIC_SPEC_QUALITYGATE_METRICS_METRICSPEC),
                    URI.createURI(res.toString()));
        }
        
        final Resource resource = resourceSet
                .createResource(URI.createURI(PATHMAP_METRIC_SPEC_QUALITYGATE_METRICS_METRICSPEC, true));
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
        
        CRASH = (Identifier) resource.getEObject("_yXCp8CE1EeyB6fGWm4pqeA");
        
        PROCESSING_TIME = (NumericalBaseMetricDescription) resource.getEObject("_x4BCEih8EeyofPOU1vRt9w");
        
        PROCESSING_TIME_TUPLE = (MetricSetDescription) resource.getEObject("_CPj4QSh9EeyofPOU1vRt9w");
        
    }
    
    /**
     * Private constructor to forbid instantiation.
     */
    private QualitygateMetricDescriptionConstants() {
    }

}
