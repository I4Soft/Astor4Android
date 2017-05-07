package fr.inria.astor.core.validation.entity;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

/**
 * 
 * @author Matias Martinez,  matias.martinez@inria.fr
 * 
 * ******************************************************************
 * @author Kayque de Sousa Teixeira, kayque23@gmail.com
 * [MODIFIED]
 * Difference from the original TestResult:
 *		- Imported java.io.Serializable
 *		- Added "implements Serializable"
 *		- Added the variable serialVersionUID
 */
public class TestResult implements Serializable {
	
	private static final long serialVersionUID = 23L;
	public int casesExecuted = 0;
	public int failures = 0;
	public List<String> successTest =new ArrayList<String>();
	 
	public  List<String> failTest =new ArrayList<String>();

	 public List<String> getSuccessTest() {
		return successTest;
	}

	public void setSuccessTest(List<String> successTest) {
		this.successTest = successTest;
	}

	public List<String> getFailures() {
		return failTest;
	}

	public void setFailTest(List<String> failTest) {
		this.failTest = failTest;
	}

	public boolean wasSuccessful(){
		return failures == 0;
	}

	@Override
	public String toString() {
		return "TR: Success: "+ (failures == 0) + ", failTest= "
				+ failures + ", was successful: "+this.wasSuccessful()+", cases executed: "+casesExecuted+"] ,"+ this.failTest;
	}

	public int getFailureCount(){
		return failures;
	}

	public int getCasesExecuted() {
		return casesExecuted;
	}
	
	 
	 
}
