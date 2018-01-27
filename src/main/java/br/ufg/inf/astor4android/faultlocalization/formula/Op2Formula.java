package br.ufg.inf.astor4android.faultlocalization.formula;

import br.ufg.inf.astorworker.faultlocalization.entities.Line;

public class Op2Formula implements FaultLocalizationFormula {

	@Override
	public double apply(Line line) {
		return (double)(line.getFailingExecuted()) - (double)(line.getPassingExecuted())/(double)(line.getTotalPassing() + 1);
	}
}