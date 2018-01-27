package br.ufg.inf.astor4android.faultlocalization.formula;

import br.ufg.inf.astorworker.faultlocalization.entities.Line;

public class BarinelFormula implements FaultLocalizationFormula {

	@Override
	public double apply(Line line) {
		return 1 - ((double)(line.getPassingExecuted())/(line.getPassingExecuted() + line.getFailingExecuted()));
	}
}