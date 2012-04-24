package de.upb.pcm.interpreter.interpreter;

import de.uka.ipd.sdq.pcm.core.composition.AssemblyContext;
import de.upb.pcm.interpreter.simulation.InterpreterDefaultContext;

public interface IInterpreterFactory {
	public UsageScenarioInterpreter getUsageModelScenarioInterpreter(
			InterpreterDefaultContext context);

	public RDSeffInterpreter getRDSEFFInterpreter(
			InterpreterDefaultContext context, AssemblyContext assemblyContext);
}
