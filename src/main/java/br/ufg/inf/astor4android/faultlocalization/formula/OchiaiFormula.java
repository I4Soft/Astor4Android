package br.ufg.inf.astor4android.faultlocalization.formula;

import br.ufg.inf.astorworker.faultlocalization.entities.Line;

public class OchiaiFormula implements FaultLocalizationFormula {

	@Override
	public double apply(Line line) {
		return line.getFailingExecuted()/(Math.sqrt(line.getTotalFailing() * (line.getFailingExecuted() + line.getPassingExecuted())));
	}
}