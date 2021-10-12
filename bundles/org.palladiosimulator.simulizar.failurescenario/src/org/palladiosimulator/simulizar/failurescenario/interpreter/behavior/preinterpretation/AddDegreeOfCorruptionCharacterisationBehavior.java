package org.palladiosimulator.simulizar.failurescenario.interpreter.behavior.preinterpretation;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map.Entry;
import org.palladiosimulator.probeframework.measurement.RequestContext;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.RDSeffSwitch;
import org.palladiosimulator.simulizar.interpreter.preinterpretation.PreInterpretationBehavior;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;

import de.uka.ipd.sdq.simucomframework.variables.exceptions.ValueNotInFrameException;
import de.uka.ipd.sdq.simucomframework.variables.stackframe.SimulatedStackframe;

public class AddDegreeOfCorruptionCharacterisationBehavior extends PreInterpretationBehavior {

    public AddDegreeOfCorruptionCharacterisationBehavior() {
        super(InterpreterResult.OK);
    }

    /**
     * Manipulates the degreeOfCorruption of the simulations stackframe variables.
     * 
     * @return InterpreterResult.OK
     */
    @Override
    public InterpreterResult execute(InterpreterDefaultContext context) {
        if (context != null) {
            addDegreeOfCorruptionCharacterisation(context);
        }
        return super.execute(context);
    }

    /**
     * Adds DegreeOfCorruption to every parameter and variable of the stackFrame
     */
    private void addDegreeOfCorruptionCharacterisation(InterpreterDefaultContext context) {

        final SimulatedStackframe<Object> currentFrame = context.getStack()
            .currentStackFrame();
        final SimulatedStackframe<Object> resultFrame = context.getCurrentResultFrame();
        List<Entry<String, Object>> entries = currentFrame.getContents();

        List<String> list = new ArrayList<String>();

        context.getStack()
            .removeStackFrame();

        if (context.getStack()
            .size() != 0) {

            entries = context.getStack()
                .currentStackFrame()
                .getContents();

            for (Entry<String, Object> e : entries) {
                String id = e.getKey();
                if (id.endsWith("." + RDSeffSwitch.DEGREE_OF_CORRUPTION)) {
                    list.add(id.split("\\.")[0]);
                }

            }

        }
        context.getStack()
            .pushStackFrame(currentFrame);

        entries = context.getStack()
            .currentStackFrame()
            .getContents();

        for (Entry<String, Object> e : entries) {
            String id = e.getKey();
            if (id.endsWith("." + RDSeffSwitch.DEGREE_OF_CORRUPTION)) {
                list.add(id.split("\\.")[0]);
            }

        }

        for (Entry<String, Object> e : entries) {
            String id = e.getKey();
            Double newValue = 0.0;
            if (!id.endsWith("." + RDSeffSwitch.DEGREE_OF_CORRUPTION) && !list.contains(id.split("\\.")[0])) {
                id = id.split("\\.")[0] + "." + RDSeffSwitch.DEGREE_OF_CORRUPTION;
                currentFrame.addValue(id, newValue);
                if (resultFrame != null) {
                    resultFrame.addValue(id, newValue);
                }
            }

        }

    }
}
