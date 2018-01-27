package br.ufg.inf.astor4android.faultlocalization;

import br.ufg.inf.astor4android.faultlocalization.formula.FaultLocalizationFormula;
import br.ufg.inf.astor4android.faultlocalization.formula.OchiaiFormula;
import br.ufg.inf.astor4android.faultlocalization.formula.Op2Formula;
import br.ufg.inf.astor4android.faultlocalization.formula.DStarFormula;
import br.ufg.inf.astor4android.faultlocalization.formula.BarinelFormula;
import br.ufg.inf.astor4android.faultlocalization.formula.TarantulaFormula;
import br.ufg.inf.astorworker.faultlocalization.entities.Line;

public class FaultLocalizationFormulaDelegate {
	private static FaultLocalizationFormula formula;
	private static String formulaType;
	private FaultLocalizationFormulaDelegate () {}

	public static boolean setFormulaType(String type) {
		formulaType = type.toUpperCase();
		switch (formulaType) {
			case "OCHIAI":
				formula = new OchiaiFormula();
				break;
			case "TARANTULA":
				formula = new TarantulaFormula();
				break;
			case "OP2":
				formula = new Op2Formula();
				break;
			case "BARINEL":
				formula = new BarinelFormula();
				break;
			case "DSTAR":
				formula = new DStarFormula();
				break;
			default:
				return false;
		}

		return true;
	}

	public static double applyFormula(Line line) {
		return formula.apply(line);
	}

	public static String getFormulaType() {
		return formulaType;
	}
}