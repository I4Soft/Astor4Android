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
	private int failingNotExecuted;
	private int passingExecuted;
	private int totalPassing;
	private double suspiciousValue;

	public Line(int number, String className, String test, boolean passing){
		this.number = number;
		this.className = className;
		this.failingExecuted = 0;
		this.failingNotExecuted = 0;
		this.passingExecuted = 0;
		this.totalPassing = 0;
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

	@Override
	public String toString(){
		return "Line = [number: " + number + ", className: " + className + ", hit by: " + String.join(":", tests) + ", fth: " + suspiciousValue + "]";
	}

	public void incrementFailingNotExecuted(){
		failingNotExecuted++;
	}	

	public void setSuspiciousValue(double value){
		suspiciousValue = value;
	}

	public void setTotalPassing(int totalPassing) {
		this.totalPassing = totalPassing;
	}

	public double getSuspiciousValue(){
		return suspiciousValue;
	}

	public int getFailingExecuted() {
		return failingExecuted;
	}

	public int getFailingNotExecuted() {
		return failingNotExecuted;
	}	

	public int getPassingExecuted() {
		return passingExecuted;
	}

	public int getTotalFailing() {
		return failingExecuted + failingNotExecuted;
	}

	public int getTotalPassing() {
		return totalPassing;
	}
}