package org.palladiosimulator.simulizar.interpreter.result;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.modelversioning.emfprofile.Stereotype;
import org.palladiosimulator.simulizar.interpreter.CallScope;

public class ParameterIssue implements QualitygateIssue {

    private URI uri;
    
    
    public ParameterIssue(Stereotype stereotype) {
        
        this.uri = EcoreUtil.getURI(stereotype);

    }



    @Override
    public boolean isHandled() {
        // TODO Auto-generated method stub
        return false;
    }



    public URI getUri() {
        return uri;
    }
}
