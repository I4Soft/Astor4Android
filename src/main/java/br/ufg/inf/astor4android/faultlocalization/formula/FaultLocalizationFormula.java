package br.ufg.inf.astor4android.faultlocalization.formula;

import br.ufg.inf.astorworker.faultlocalization.entities.Line;

public interface FaultLocalizationFormula {
	public double apply(Line line);
}