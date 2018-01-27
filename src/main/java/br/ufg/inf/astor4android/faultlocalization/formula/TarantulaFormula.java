package br.ufg.inf.astor4android.faultlocalization.formula;

import br.ufg.inf.astorworker.faultlocalization.entities.Line;

public class TarantulaFormula implements FaultLocalizationFormula {

	@Override
	public double apply(Line line) {
		return ((double)(line.getFailingExecuted())/line.getTotalFailing())/((double)(line.getFailingExecuted())/line.getTotalFailing() + (double)(line.getPassingExecuted())/line.getTotalPassing());
	}
}