package br.ufg.inf.astorworker.faultlocalization.entities;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;


public class Line implements Serializable {
	private static final long serialVersionUID = 15L;
	private int number;
	private String className;
	private List<String> tests; //Tests that executed this line
	private int failingExecuted;
	private int passingExecuted;
	private int failingNotExecuted;

	public Line(int number, String className, String test, boolean passing){
		this.number = number;
		this.className = className;
		this.failingExecuted = 0;
		this.failingNotExecuted = 0;
		this.passingExecuted = 0;
		tests = new ArrayList<String>();
		addTest(test, passing);
	}

	public int getNumber(){
		return number;
	}

	public String getClassName(){
		return className;
	}
	
	public List<String> getTestList(){
		return tests;
	}

	public boolean wasHitBy(String test){
		return tests.contains(test);
	}

	public void addTest(String test, boolean passing){
		if(tests.contains(test)) return;
		
		if(passing)
			passingExecuted++;
		else 
			failingExecuted++;

		tests.add(test);
	}

	public double getSuspiciousValue(){
		return failingExecuted/(Math.sqrt((failingExecuted + failingNotExecuted) * (failingExecuted + passingExecuted)));
	}

	@Override
	public String toString(){
		return "Line = [number: "+number+", className: "+className+", hit by: "+String.join(":",tests)+", fth: "+getSuspiciousValue()+"]";
	}

	public void incrementFailingNotExecuted(){
		failingNotExecuted++;
	}
}