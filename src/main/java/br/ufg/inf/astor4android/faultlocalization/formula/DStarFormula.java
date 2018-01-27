package br.ufg.inf.astor4android.faultlocalization.formula;

import br.ufg.inf.astorworker.faultlocalization.entities.Line;

public class DStarFormula implements FaultLocalizationFormula {
	private final int STAR_VALUE = 2;

	@Override
	public double apply(Line line) {
		return Math.pow(line.getFailingExecuted(), STAR_VALUE)/(line.getPassingExecuted() + (line.getTotalFailing() - line.getFailingExecuted()));
	}
}