package fr.inria.astor.core.validation.validators;

import java.io.Serializable;

import fr.inria.astor.core.validation.entity.TestResult;
import fr.inria.astor.core.entities.TestCaseVariantValidationResult;

/**
 * 
 * @author Matias Martinez
 *
 * ****************************************************
 * @author Kayque de Sousa Teixeira, kayque23@gmail.com
 * [MODIFIED]
 * Difference from the original TestCasesProgramValidationResult:
 *		- Imported java.io.Serializable
 *		- Added "implements Serializable"
 *		- Added the variable serialVersionUID
 *		- Added the variable compilationSucess
 *		- Added function isCompilationSuccess
 */
public class TestCasesProgramValidationResult  implements Serializable, TestCaseVariantValidationResult {

	private static final long serialVersionUID = 2L;
	int numberFailingTestCases = 0;
	int numberPassingTestCases = 0;

	boolean regressionExecuted = false;
	boolean resultSucess = false;
	boolean compilationSuccess = false;

	/**
	 * Indicates whether where were a problem during the execution that stop
	 * finishing the complete execution , example Infinite loop
	 **/
	boolean executionError = false;

	TestResult testResult;

	public TestCasesProgramValidationResult(TestResult result) {
		super();
		setTestResult(result);
	}

	public TestCasesProgramValidationResult(boolean errorExecution) {
		this.executionError = errorExecution;
		this.testResult = null;
		this.regressionExecuted = false;
		this.resultSucess = false;
		this.numberFailingTestCases = 0;
		this.numberPassingTestCases = 0;
	}

	public TestCasesProgramValidationResult(TestResult result, boolean resultSucess, boolean regressionExecuted) {
		this(result);
		this.regressionExecuted = regressionExecuted;
		this.resultSucess = resultSucess;
	}

	public boolean isSuccessful() {

		return numberFailingTestCases == 0 && this.resultSucess;
	}

	public int getFailureCount() {

		return numberFailingTestCases;
	}

	public boolean isRegressionExecuted() {
		return regressionExecuted;
	}

	public void setRegressionExecuted(boolean regressionExecuted) {
		this.regressionExecuted = regressionExecuted;
	}

	public int getPassingTestCases() {
		return numberPassingTestCases;
	}

	public String toString() {
		return printTestResult(this.getTestResult());
	}

	public TestResult getTestResult() {
		return testResult;
	}

	public void setTestResult(TestResult result) {
		this.testResult = result;
		if (result != null) {
			numberPassingTestCases = result.casesExecuted - result.failures;
			numberFailingTestCases = result.failures;
			resultSucess = (result.casesExecuted == result.failures);
		}
	}

	protected String printTestResult(TestResult result) {
		if (this.executionError || (result == null)) {
			return "|" + false + "|" + 0 + "|" + 0 + "|" + "[]" + "|";
		}
	
		return "|" + result.wasSuccessful() + "|" + result.failures + "|" + result.casesExecuted + "|" + result.failTest
				+ "|";
	}

	@Override
	public int getCasesExecuted() {
		return getPassingTestCases() + getFailureCount();
	}

	public boolean isExecutionError() {
		return executionError;
	}

	public boolean isCompilationSuccess(){
		return compilationSuccess;
	}	

	public void setCompilationSuccess(boolean compilationSuccess){
		this.compilationSuccess = compilationSuccess;
	}


}
