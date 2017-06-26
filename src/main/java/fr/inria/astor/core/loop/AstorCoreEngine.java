package fr.inria.astor.core.loop;

import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.io.File;

import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;

import com.martiansoftware.jsap.JSAPException;

import br.ufg.inf.handlers.entities.Worker;
import br.ufg.inf.handlers.WorkerHandler;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.VariantValidationResult;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.entities.WeightCtElement;
import fr.inria.astor.core.faultlocalization.FaultLocalizationStrategy;
import fr.inria.astor.core.loop.extension.AstorExtensionPoint;
import fr.inria.astor.core.loop.extension.SolutionVariantSortCriterion;
import fr.inria.astor.core.loop.extension.VariantCompiler;
import fr.inria.astor.core.loop.population.FitnessFunction;
import fr.inria.astor.core.loop.population.PopulationController;
import fr.inria.astor.core.loop.population.ProgramVariantFactory;
import fr.inria.astor.core.loop.spaces.operators.OperatorSelectionStrategy;
import fr.inria.astor.core.loop.spaces.operators.OperatorSpace;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.manipulation.bytecode.entities.CompilationResult;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.ProjectRepairFacade;
import fr.inria.astor.core.stats.StatPatch;
import fr.inria.astor.core.stats.Stats;
import fr.inria.astor.core.validation.validators.ProcessEvoSuiteValidator;
import fr.inria.astor.core.validation.validators.ProcessValidator;
import fr.inria.astor.core.validation.validators.ProgramValidator;
import fr.inria.astor.core.validation.validators.TestCasesProgramValidationResult;
import fr.inria.astor.util.PatchDiffCalculator;
import fr.inria.astor.util.StringUtil;
import fr.inria.astor.util.TimeUtil;
import fr.inria.main.evolution.ExtensionPoints;
import fr.inria.main.evolution.PlugInLoader;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtType;

/**
 * Evolutionary program transformation Loop
 * 
 * @author Matias Martinez, matias.martinez@inria.fr
 * 
 * ******************************************************************
 * @author Kayque de Sousa Teixeira, kayque23@gmail.com
 * [MODIFIED]
 * Difference from the original AstorCoreEngine:
 *		- Modified function processGenerations
 *		- Modified function atEnd
 *		- Added innerclass VariantHandler
 *		- Imported br.ufg.inf.handlers.entities.Worker
 *		- Imported br.ufg.inf.handlers.WorkerHandler
 *		- Imported fr.inria.astor.core.validation.validators.TestCasesProgramValidationResult
 *		- Imported java.io.File
 */
public abstract class AstorCoreEngine implements AstorExtensionPoint {

	/**
	 * Initial identifier.
	 */
	public static int firstgenerationIndex = 1;

	/**
	 * Statistic
	 */
	protected Stats currentStat = null;

	protected Logger log = Logger.getLogger(Thread.currentThread().getName());

	protected ProgramVariantFactory variantFactory;

	protected ProgramValidator programValidator;

	// INTERNAL
	protected List<ProgramVariant> variants = new ArrayList<ProgramVariant>();
	protected List<ProgramVariant> solutions = new ArrayList<ProgramVariant>();

	protected ProgramVariant originalVariant = null;

	// SPACES

	protected OperatorSelectionStrategy operatorSelectionStrategy = null;

	protected OperatorSpace operatorSpace = null;

	protected PopulationController populationControler = null;

	// CODE MANAGMENT
	protected MutationSupporter mutatorSupporter = null;

	protected ProjectRepairFacade projectFacade = null;

	protected Date dateInitEvolution = new Date();

	//
	protected FaultLocalizationStrategy faultLocalization = null;

	protected int generationsExecuted = 0;

	protected SolutionVariantSortCriterion patchSortCriterion = null;

	protected FitnessFunction fitnessFunction = null;

	protected VariantCompiler compiler = null;

	/**
	 * 
	 * @param mutatorExecutor
	 * @throws JSAPException
	 */
	public AstorCoreEngine(MutationSupporter mutatorExecutor, ProjectRepairFacade projFacade) throws JSAPException {
		this.mutatorSupporter = mutatorExecutor;
		this.projectFacade = projFacade;

		this.currentStat = Stats.getCurrentStats();
	}

	public void startEvolution() throws Exception {

		log.info("\n----Starting Solution Search");

		generationsExecuted = 0;
		boolean stop = false;

		currentStat.passFailingval1 = 0;
		currentStat.passFailingval2 = 0;

		dateInitEvolution = new Date();

		int maxMinutes = ConfigurationProperties.getPropertyInt("maxtime");

		while (!this.variants.isEmpty() && (!stop || !ConfigurationProperties.getPropertyBool("stopfirst"))
				&& (generationsExecuted < ConfigurationProperties.getPropertyInt("maxGeneration")
						&& belowMaxTime(dateInitEvolution, maxMinutes))
				&& limitDate()) {
			generationsExecuted++;
			log.debug("\n----------Running generation/iteraction " + generationsExecuted + ", population size: "
					+ this.variants.size());
			stop = processGenerations(generationsExecuted);
		}
		// At the end
		long startT = dateInitEvolution.getTime();
		long endT = System.currentTimeMillis();
		log.info("Time Repair Loop (s): " + (endT - startT) / 1000d);
		currentStat.timeIteraction = ((endT - startT));

	}

	public void atEnd() throws Exception {

		this.sortPatches();
		this.showResults();

		WorkerHandler.finishAllWorkers();
	}

	/**
	 * Sorts patches
	 */
	public void sortPatches() {
		if (!getSolutions().isEmpty() && this.getPatchSortCriterion() != null) {
			this.solutions = this.getPatchSortCriterion().priorize(getSolutions());

		}
	}

	public void showResults() {
		log.info("\n----SUMMARY_EXECUTION---");
		if (!this.solutions.isEmpty()) {
			log.debug("End Repair Loops: Found solution");
			log.debug("Solution stored at: " + projectFacade.getProperties().getWorkingDirForSource());

		} else {
			log.debug("End Repair Loops: NOT Found solution");
		}
		log.debug("\nNumber solutions:" + this.solutions.size());
		for (ProgramVariant variant : solutions) {
			log.debug("f (sol): " + variant.getFitness() + ", " + variant);
		}
		log.debug("\nAll variants:");
		for (ProgramVariant variant : variants) {
			log.debug("f " + variant.getFitness() + ", " + variant);
		}

		if (!solutions.isEmpty()) {
			log.info("\nSolution details");
			log.info(getSolutionData(solutions, generationsExecuted));

		}

	}

	/**
	 * Check whether the program has passed the maximum time for operating
	 * 
	 * @param dateInit
	 *            start date of execution
	 * @param maxMinutes
	 *            max minutes for operating
	 * @return
	 */
	protected boolean belowMaxTime(Date dateInit, int maxMinutes) {
		if (TimeUtil.deltaInMinutes(dateInit) <= maxMinutes) {
			return true;
		} else {
			log.info("\n No more time for operating");
			return false;
		}
	}

	public boolean limitDate() {
		String limit = ConfigurationProperties.properties.getProperty("maxdate");
		if (limit == null) {
			return true;
		}

		try {
			Date d = TimeUtil.tranformHours(limit);

			Date dc = new Date();
			Date tr = TimeUtil.tranformHours(dc.getHours() + ":" + dc.getMinutes());
			if (tr.getHours() >= 12) {
				return true;
			}
			boolean continueProc = tr.before(d);
			if (!continueProc) {
				log.info("Astor reaches the hour limit, we stop here");
			}
			return continueProc;
		} catch (ParseException e) {
			log.error("Parsing time", e);
			return false;
		}
	}

	/**
	 * Process a generation i: loops over all instances
	 * 
	 * @param generation
	 * @return
	 * @throws Exception
	 */
	private boolean processGenerations(int generation) throws Exception {

		log.debug("\n***** Generation " + generation);
		boolean foundSolution = false;

		List<ProgramVariant> temporalInstances = new ArrayList<ProgramVariant>();

		currentStat.numberGenerations++;

		List<VariantHandler> variantHandlers = new ArrayList<VariantHandler>();

		for (ProgramVariant parentVariant : variants) {

			log.debug("\n**\n*-Parent Variant: " + parentVariant);
			
			VariantHandler vh = new VariantHandler(parentVariant, fitnessFunction, generation);
			vh.start();
			variantHandlers.add(vh);

		}

		for(VariantHandler vh : variantHandlers){
			vh.join();
			ProgramVariant tmp = vh.getTemportalInstance();
			if(tmp != null)
				temporalInstances.add(tmp);
			foundSolution = vh.getFoundSolution();

			if (foundSolution && ConfigurationProperties.getPropertyBool("stopfirst")) {
				break;
			}
		}

		prepareNextGeneration(temporalInstances, generation);

		return foundSolution;
	}


	private class VariantHandler extends Thread {
		private ProgramVariant variant;
		private FitnessFunction fitnessFunction;
		private int generation;
		private ProgramVariant temporalInstance;
		private boolean foundSolution;
		private Logger log = Logger.getLogger(Thread.currentThread().getName());

		public VariantHandler(ProgramVariant variant, FitnessFunction fitnessFunction, int generation){
			this.variant = variant;
			this.fitnessFunction = fitnessFunction;
			this.generation = generation;
		}

		@Override
		public void run() {
			ProgramVariant newVariant = null;
			String srcOutput = null;
			TestCasesProgramValidationResult validationResult = null;
			boolean childCompiles = false;

			synchronized(VariantHandler.class){
				try{
					saveOriginalVariant(variant);
					newVariant = createNewProgramVariant(variant, generation);
					saveModifVariant(variant);

					if (newVariant == null) {
						return;
					}

					srcOutput = projectFacade.getInDirWithPrefix(newVariant.currentMutatorIdentifier());
					mutatorSupporter.saveSourceCodeOnDiskProgramVariant(newVariant, srcOutput);
					// Finally, reverse the changes done by the child
					reverseOperationInModel(newVariant, generation);
					boolean validation = validateReversedOriginalVariant(newVariant);
					
				} catch(Exception e){
					e.printStackTrace();
				}

			}

			try {
				Worker worker = WorkerHandler.getWorker();
				log.info(Thread.currentThread().getName()+" got the worker "+worker.toString());

				File variantFolder = new File(srcOutput);
				validationResult = worker.processVariant(variantFolder);
				childCompiles = validationResult.isCompilationSuccess();

				WorkerHandler.putWorker(worker);
				log.info(Thread.currentThread().getName()+" returned the worker "+worker.toString()+" to the queue");

				temporalInstance = newVariant;

				boolean solution = processValidationResult(newVariant, generation, childCompiles, validationResult);

				if (solution) {
					foundSolution = true;
					newVariant.setBornDate(new Date());
				}
			} catch(Exception e){
					e.printStackTrace();
			}
		}

		private boolean processValidationResult(ProgramVariant programVariant, int generation, boolean childCompiles, TestCasesProgramValidationResult validationResult) throws Exception {
			if (childCompiles) {

				log.debug(Thread.currentThread().getName()+": the child compiles: id " + programVariant.getId());
				currentStat.numberOfRightCompilation++;
				currentStat.setCompiles(programVariant.getId());
				boolean validInstance = validateInstance(programVariant, validationResult);

				double fitness = this.fitnessFunction.calculateFitnessValue(programVariant);
				programVariant.setFitness(fitness);

				log.debug(Thread.currentThread().getName()+": valid?: " + validInstance + ", fitness " + programVariant.getFitness());
				if (validInstance) {
					log.info(Thread.currentThread().getName()+": found Solution, child variant #" + programVariant.getId());
					saveStaticSucessful(programVariant.getId(), generation);
					return true;
				}
			} else {
				log.debug(Thread.currentThread().getName()+": the child does NOT compile: " + programVariant.getId());
				currentStat.numberOfFailingCompilation++;
				currentStat.setNotCompiles(programVariant.getId());
				programVariant.setFitness(this.fitnessFunction.getWorstMaxFitnessValue());

			}
			return false;
		}

		private boolean validateInstance(ProgramVariant variant, TestCasesProgramValidationResult validationResult) {
			if (validationResult.getTestResult() != null) {
				boolean wasSuc = validationResult.isSuccessful();
				variant.setIsSolution(wasSuc);
				variant.setValidationResult(validationResult);
				return wasSuc;
			}
			return false;
		}

		public boolean getFoundSolution(){
			return foundSolution;
		}

		public ProgramVariant getTemportalInstance(){
			return temporalInstance;
		}
	}

	/**
	 * Store in the program variant passed as parameter a clone of each ctclass
	 * involved in the variant.
	 * 
	 * @param variant
	 */
	private void storeModifiedModel(ProgramVariant variant) {
		variant.getModifiedClasses().clear();
		for (CtClass modifiedClass : variant.getBuiltClasses().values()) {
			CtClass cloneModifClass = (CtClass) MutationSupporter.clone(modifiedClass);
			cloneModifClass.setParent(modifiedClass.getParent());
			variant.getModifiedClasses().add(cloneModifClass);
		}

	}

	public void prepareNextGeneration(List<ProgramVariant> temporalInstances, int generation) {
		// After analyze all variant
		// New population creation:
		// show all and search solutions:

		// We filter the solution from the rest
		String solutionId = "";
		for (ProgramVariant programVariant : temporalInstances) {
			if (programVariant.isSolution()) {
				this.solutions.add(programVariant);
				solutionId += programVariant.getId() + "(SOLUTION)(f=" + programVariant.getFitness() + ")" + ", ";
			}
		}
		log.debug("\nEnd analysis generation - \nSolutions found:" + "--> (" + solutionId + ")");

		variants = populationControler.selectProgramVariantsForNextGeneration(variants, temporalInstances,
				ConfigurationProperties.getPropertyInt("population"), variantFactory, originalVariant, generation);

	}

	Map<String, String> originalModel = new HashedMap();
	Map<String, String> modifModel = new HashedMap();

	private void saveOriginalVariant(ProgramVariant variant) {
		originalModel.clear();
		for (CtType st : variant.getAffectedClasses()) {
			try {
				originalModel.put(st.getQualifiedName(), st.toString());
			} catch (Exception e) {
				log.error("Problems saving cttype: " + st.getQualifiedName());
			}
		}

	}

	private void saveModifVariant(ProgramVariant variant) {
		modifModel.clear();
		for (CtType st : variant.getAffectedClasses()) {
			try {
				modifModel.put(st.getQualifiedName(), st.toString());
			} catch (Exception e) {
				log.error("Problems saving cttype: " + st.getQualifiedName());
			}

		}

	}

	private boolean validateReversedOriginalVariant(ProgramVariant variant) {

		for (CtType st : variant.getAffectedClasses()) {
			String original = originalModel.get(st.getQualifiedName());
			if (original != null) {
				boolean idem = original.equals(st.toString());
				if (!idem) {
					log.error("Error: the model was not the same from the original after this generation");
					log.error("Undo Error: original: \n" + original);
					log.error("Undo Error: modified: \n" + st.toString());
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 
	 * Compiles and validates a created variant.
	 * 
	 * @param parentVariant
	 * @param generation
	 * @return true if the variant is a solution. False otherwise.
	 * @throws Exception
	 */
	public boolean processCreatedVariant(ProgramVariant programVariant, int generation) throws Exception {

		URL[] originalURL = projectFacade.getClassPathURLforProgramVariant(ProgramVariant.DEFAULT_ORIGINAL_VARIANT);

		CompilationResult compilation = compiler.compile(programVariant, originalURL);

		boolean childCompiles = compilation.compiles();
		programVariant.setCompilation(compilation);

		storeModifiedModel(programVariant);

		String srcOutput = projectFacade.getInDirWithPrefix(programVariant.currentMutatorIdentifier());

		if (ConfigurationProperties.getPropertyBool("saveall")) {
			log.debug("\n-Saving child on disk variant #" + programVariant.getId() + " at " + srcOutput);
			// This method should be refactored, and replace by the
			// output from memory compilation
			mutatorSupporter.saveSourceCodeOnDiskProgramVariant(programVariant, srcOutput);
		}

		if (childCompiles) {

			log.debug("-The child compiles: id " + programVariant.getId());
			currentStat.numberOfRightCompilation++;
			currentStat.setCompiles(programVariant.getId());

			boolean validInstance = validateInstance(programVariant);
			double fitness = this.fitnessFunction.calculateFitnessValue(programVariant);
			programVariant.setFitness(fitness);

			log.debug("-Valid?: " + validInstance + ", fitness " + programVariant.getFitness());
			if (validInstance) {
				log.info("-Found Solution, child variant #" + programVariant.getId());
				saveStaticSucessful(programVariant.getId(), generation);
				if (ConfigurationProperties.getPropertyBool("savesolution")) {
					mutatorSupporter.saveSourceCodeOnDiskProgramVariant(programVariant, srcOutput);
					mutatorSupporter.saveSolutionData(programVariant, srcOutput, generation);
				}
				return true;
			}
		} else {
			log.debug("-The child does NOT compile: " + programVariant.getId() + ", errors: "
					+ compilation.getErrorList());
			currentStat.numberOfFailingCompilation++;
			currentStat.setNotCompiles(programVariant.getId());
			programVariant.setFitness(this.fitnessFunction.getWorstMaxFitnessValue());
		}
		// In case that the variant a) does not compile; b) compiles but it's
		// not adequate
		Stats.currentStat.storeIngCounterFromFailingPatch(programVariant.getId());
		return false;

	}

	protected void saveStaticSucessful(int variant_id, int generation) {
		currentStat.patches++;
		currentStat.genPatches.add(new StatPatch(generation, currentStat.passFailingval1, currentStat.passFailingval2));
		currentStat.passFailingval1 = 0;
		currentStat.passFailingval2 = 0;
		Stats.currentStat.storeIngCounterFromSuccessPatch(variant_id);
		Stats.currentStat.storePatchAttempts(variant_id);
	}

	/**
	 * Create a child mutated. Return null if not mutation is produced by the
	 * engine (i.e. the child is equal to the parent)
	 * 
	 * @param parentVariant
	 * @param generation
	 * @param idsChild
	 * @return
	 * @throws Exception
	 */
	protected ProgramVariant createNewProgramVariant(ProgramVariant parentVariant, int generation) throws Exception {
		// This is the copy of the original program
		ProgramVariant childVariant = variantFactory.createProgramVariantFromAnother(parentVariant, generation);
		log.debug("\n--Child created id: " + childVariant.getId());

		// Apply previous operations (i.e., from previous operators)
		applyPreviousOperationsToVariantModel(childVariant, generation);

		boolean isChildMutatedInThisGeneration = modifyProgramVariant(childVariant, generation);

		if (!isChildMutatedInThisGeneration) {
			log.debug("--Not Operation generated in child variant: " + childVariant);
			reverseOperationInModel(childVariant, generation);
			return null;
		}

		boolean appliedOperations = applyNewOperationsToVariantModel(childVariant, generation);

		if (!appliedOperations) {
			log.debug("---Not Operation applied in child variant:" + childVariant);
			reverseOperationInModel(childVariant, generation);
			return null;
		}

		return childVariant;
	}

	/**
	 * Undo in reverse order that the mutation were applied.
	 * 
	 * @param variant
	 * @param generation
	 */
	protected void reverseOperationInModel(ProgramVariant variant, int generation) {

		if (variant.getOperations() == null || variant.getOperations().isEmpty()) {
			return;
		}
		// For each generation, in reverse order they are generated.

		for (int genI = generation; genI >= 1; genI--) {

			undoSingleGeneration(variant, genI);
		}
	}

	protected void undoSingleGeneration(ProgramVariant instance, int genI) {
		List<OperatorInstance> operations = instance.getOperations().get(genI);
		if (operations == null || operations.isEmpty()) {
			return;
		}

		for (int i = operations.size() - 1; i >= 0; i--) {
			OperatorInstance genOperation = operations.get(i);
			log.debug("---Undoing: gnrtn(" + genI + "): " + genOperation);
			undoOperationToSpoonElement(genOperation);
		}
	}

	/**
	 * Given a program variant, the method generates operations for modifying
	 * that variants. Each operation is related to one gen of the program
	 * variant.
	 * 
	 * @param variant
	 * @param generation
	 * @return
	 * @throws Exception
	 */
	private boolean modifyProgramVariant(ProgramVariant variant, int generation) throws Exception {

		log.debug("--Creating new operations for variant " + variant);
		boolean oneOperationCreated = false;
		int genMutated = 0, notmut = 0, notapplied = 0;
		int nroGen = 0;

		this.currentStat.sizeSpaceOfVariant.clear();

		// For each gen of the program instance
		List<ModificationPoint> modificationPointsToProcess = getGenList(variant);
		// log.debug("modifPointsToProcess " + modificationPointsToProcess);
		for (ModificationPoint modificationPoint : modificationPointsToProcess) {
			// tp refactor
			modificationPoint.identified = variant.getModificationPoints().indexOf(modificationPoint);
			log.debug("---analyzing modificationPoint position: " + modificationPoint.identified);

			// A point can be modified several time in the evolution
			boolean multiPointMutation = ConfigurationProperties.getPropertyBool("multipointmodification");
			if (!multiPointMutation && alreadyModified(modificationPoint, variant.getOperations(), generation))
				continue;

			this.currentStat.typeOfElementsSelectedForModifying
					.add(modificationPoint.getCodeElement().getClass().getSimpleName());

			modificationPoint.setProgramVariant(variant);
			OperatorInstance modificationInstance = createOperatorInstanceForPoint(modificationPoint);

			if (modificationInstance != null) {

				modificationInstance.setModificationPoint(modificationPoint);

				if (ConfigurationProperties.getPropertyBool("uniqueoptogen") && alreadyApplied(modificationInstance)) {
					log.debug("---Operation already applied to the gen " + modificationInstance);
					currentStat.setAlreadyApplied(variant.getId());
					continue;
				}

				log.debug("operation " + modificationInstance);
				currentStat.numberOfAppliedOp++;
				variant.putModificationInstance(generation, modificationInstance);

				oneOperationCreated = true;
				genMutated++;
				// We analyze all gens
				if (!ConfigurationProperties.getPropertyBool("allpoints")) {
					break;
				}

			} else {// Not gen created
				currentStat.numberOfGenInmutated++;
				log.debug("---modifPoint " + (nroGen++) + " not mutation generated in  "
						+ StringUtil.trunc(modificationPoint.getCodeElement().toString()));
				notmut++;
			}
		}

		if (oneOperationCreated && !ConfigurationProperties.getPropertyBool("resetoperations")) {
			updateVariantGenList(variant, generation);
		}
		log.debug("\n--Summary Creation: for variant " + variant + " gen mutated: " + genMutated + " , gen not mut: "
				+ notmut + ", gen not applied  " + notapplied);

		currentStat.saveStats();

		return oneOperationCreated;
	}

	Map<ModificationPoint, List<OperatorInstance>> operationGenerated = new HashedMap();

	private boolean alreadyApplied(OperatorInstance operationNew) {

		List<OperatorInstance> ops = operationGenerated.get(operationNew.getModificationPoint());
		if (ops == null) {
			ops = new ArrayList<>();
			operationGenerated.put(operationNew.getModificationPoint(), ops);
			ops.add(operationNew);
			return false;
		}
		return ops.contains(operationNew);
	}

	/**
	 * Return true if the gen passed as parameter was already affected by a
	 * previous operator.
	 * 
	 * @param genProgInstance
	 * @param map
	 * @param generation
	 * @return
	 */
	private boolean alreadyModified(ModificationPoint genProgInstance, Map<Integer, List<OperatorInstance>> map,
			int generation) {

		for (int i = 1; i < generation; i++) {
			List<OperatorInstance> ops = map.get(i);
			if (ops == null)
				continue;
			for (OperatorInstance genOperationInstance : ops) {
				if (genOperationInstance.getModificationPoint() == genProgInstance) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 
	 * @param variant
	 * @param modificationPoints
	 * @return
	 */
	protected List<ModificationPoint> getGenList(ProgramVariant variant) {
		List<ModificationPoint> genList = variant.getModificationPoints();
		String mode = ConfigurationProperties.getProperty("modificationpointnavigation");

		if ("inorder".equals(mode))
			return genList;

		if ("weight".equals(mode))
			return getWeightGenList(genList);

		if ("random".equals(mode)) {
			List<ModificationPoint> shuffList = new ArrayList<ModificationPoint>(genList);
			Collections.shuffle(shuffList);
			return shuffList;
		}

		if ("sequence".equals(mode)) {
			int i = variant.getLastGenAnalyzed();
			if (i < genList.size()) {
				variant.setLastGenAnalyzed(i + 1);
				return genList.subList(i, i + 1);
			}
			return Collections.EMPTY_LIST;
		}

		return genList;

	}

	protected List<ModificationPoint> getWeightGenList(List<ModificationPoint> genList) {
		List<ModificationPoint> remaining = new ArrayList<ModificationPoint>(genList);
		List<ModificationPoint> solution = new ArrayList<ModificationPoint>();

		for (int i = 0; i < genList.size(); i++) {
			List<WeightCtElement> we = new ArrayList<WeightCtElement>();
			double sum = 0;
			for (ModificationPoint gen : remaining) {
				double susp = ((SuspiciousModificationPoint) gen).getSuspicious().getSuspiciousValue();
				sum += susp;
				WeightCtElement w = new WeightCtElement(gen, 0);
				w.weight = susp;
				we.add(w);
			}

			if (sum != 0) {

				for (WeightCtElement weightCtElement : we) {
					weightCtElement.weight = weightCtElement.weight / sum;
				}

				WeightCtElement.feedAccumulative(we);
				WeightCtElement selected = WeightCtElement.selectElementWeightBalanced(we);

				ModificationPoint selectedg = (ModificationPoint) selected.element;
				remaining.remove(selectedg);
				solution.add(selectedg);
			} else {
				solution.addAll(remaining);
				break;
			}
		}
		return solution;

	}

	private void updateVariantGenList(ProgramVariant variant, int generation) {
		List<OperatorInstance> operations = variant.getOperations().get(generation);

		for (OperatorInstance genOperationInstance : operations) {
			updateVariantGenList(variant, genOperationInstance);
		}
	}

	public abstract void createInitialPopulation() throws Exception;

	/**
	 * This method updates gens of a variant according to a created
	 * GenOperationInstance
	 * 
	 * @param variant
	 * @param operationofGen
	 */
	protected abstract void updateVariantGenList(ProgramVariant variant, OperatorInstance operation);

	/**
	 * Create a Gen Mutation for a given CtElement
	 * 
	 * @param ctElementPointed
	 * @param className
	 * @param suspValue
	 * @return
	 * @throws IllegalAccessException
	 */
	protected abstract OperatorInstance createOperatorInstanceForPoint(ModificationPoint genProgInstance)
			throws IllegalAccessException;

	protected abstract void undoOperationToSpoonElement(OperatorInstance operation);

	/**
	 * Apply a mutation generated in previous generation to a model
	 * 
	 * @param variant
	 * @param currentGeneration
	 * @throws IllegalAccessException
	 */
	protected void applyPreviousOperationsToVariantModel(ProgramVariant variant, int currentGeneration)
			throws IllegalAccessException {

		// We do not include the current generation (should be empty)
		for (int generation_i = firstgenerationIndex; generation_i < currentGeneration; generation_i++) {

			List<OperatorInstance> operations = variant.getOperations().get(generation_i);
			if (operations == null || operations.isEmpty()) {
				continue;
			}
			for (OperatorInstance genOperation : operations) {
				applyPreviousMutationOperationToSpoonElement(genOperation);
				log.debug("----gener( " + generation_i + ") `" + genOperation.isSuccessfulyApplied() + "`, "
						+ genOperation.toString());

			}

		}
	}

	/**
	 * Apply the mutation generated in the current Generation
	 * 
	 * @param variant
	 * @param currentGeneration
	 * @throws IllegalAccessException
	 */
	public boolean applyNewOperationsToVariantModel(ProgramVariant variant, int currentGeneration)
			throws IllegalAccessException {

		List<OperatorInstance> operations = variant.getOperations().get(currentGeneration);
		if (operations == null || operations.isEmpty()) {
			return false;
		}

		for (OperatorInstance genOperation : operations) {

			applyNewMutationOperationToSpoonElement(genOperation);

		}

		// For the last generation,remove operation with exceptions
		// Clean Operations not applied:
		int size = operations.size();
		for (int i = 0; i < size; i++) {
			OperatorInstance genOperationInstance = operations.get(i);
			if (genOperationInstance.getExceptionAtApplied() != null || !genOperationInstance.isSuccessfulyApplied()) {
				log.debug("---Error! Deleting " + genOperationInstance + " failed by a "
						+ genOperationInstance.getExceptionAtApplied());
				operations.remove(i);
				i--;
				size--;
			}
		}
		return !(operations.isEmpty());
	}

	protected abstract void applyPreviousMutationOperationToSpoonElement(OperatorInstance operation)
			throws IllegalAccessException;

	/**
	 * Apply a given Mutation to the node referenced by the operation
	 * 
	 * @param operation
	 * @throws IllegalAccessException
	 */
	protected abstract void applyNewMutationOperationToSpoonElement(OperatorInstance operation)
			throws IllegalAccessException;

	protected boolean validateInstance(ProgramVariant variant) {

		VariantValidationResult validationResult;

		if ((validationResult = programValidator.validate(variant, projectFacade)) != null) {
			boolean wasSuc = validationResult.isSuccessful();
			variant.setIsSolution(wasSuc);
			variant.setValidationResult(validationResult);
			return wasSuc;
		}
		return false;
	}

	public OperatorSelectionStrategy getOperatorSelectionStrategy() {
		return operatorSelectionStrategy;
	}

	public void setOperatorSelectionStrategy(OperatorSelectionStrategy operatorSelectionStrategy) {
		this.operatorSelectionStrategy = operatorSelectionStrategy;
	}

	public List<ProgramVariant> getVariants() {
		return variants;
	}

	public ProgramVariantFactory getVariantFactory() {
		return variantFactory;
	}

	public MutationSupporter getMutatorSupporter() {
		return mutatorSupporter;
	}

	public void setMutatorSupporter(MutationSupporter mutatorSupporter) {
		this.mutatorSupporter = mutatorSupporter;
	}

	public PopulationController getPopulationControler() {
		return populationControler;
	}

	public void setPopulationControler(PopulationController populationControler) {
		this.populationControler = populationControler;
	}

	public void setProjectFacade(ProjectRepairFacade projectFacade) {
		this.projectFacade = projectFacade;
	}

	public void setVariantFactory(ProgramVariantFactory variantFactory) {
		this.variantFactory = variantFactory;
	}

	public ProgramValidator getProgramValidator() {
		return programValidator;
	}

	public void setProgramValidator(ProgramValidator programValidator) {
		this.programValidator = programValidator;
		this.programValidator.setStats(currentStat);
	}

	public String getSolutionData(List<ProgramVariant> variants, int generation) {
		String line = "";
		line += "\n --SOLUTIONS DESCRIPTION--\n";
		for (ProgramVariant solutionVariant : variants) {
			line += "\n ----\n";
			line += "ProgramVariant " + solutionVariant.getId() + "\n ";
			line += "\ntime(sec)= "
					+ TimeUtil.getDateDiff(this.dateInitEvolution, solutionVariant.getBornDate(), TimeUnit.SECONDS);

			for (int i = 1; i <= generation; i++) {
				List<OperatorInstance> genOperationInstances = solutionVariant.getOperations().get(i);
				if (genOperationInstances == null)
					continue;

				for (OperatorInstance genOperationInstance : genOperationInstances) {

					line += "\noperation: " + genOperationInstance.getOperationApplied().toString() + "\nlocation= "
							+ genOperationInstance.getModificationPoint().getCtClass().getQualifiedName();

					if (genOperationInstance.getModificationPoint() instanceof SuspiciousModificationPoint) {
						SuspiciousModificationPoint gs = (SuspiciousModificationPoint) genOperationInstance
								.getModificationPoint();
						line += "\nline= " + gs.getSuspicious().getLineNumber();
						line += "\nlineSuspiciousness= " + gs.getSuspicious().getSuspiciousValueString();
					}

					line += "\noriginal statement= " + genOperationInstance.getOriginal().toString();
					line += "\nfixed statement= ";
					if (genOperationInstance.getModified() != null)
						// if fix content is the same that original buggy
						// content, we do not write the patch, remaining empty
						// the property fixed statement
						if (genOperationInstance.getModified().toString() != genOperationInstance.getOriginal()
								.toString())
						line += genOperationInstance.getModified().toString();
						else {
						line += genOperationInstance.getOriginal().toString();
						}

					line += "\ngeneration= " + Integer.toString(i);
					line += "\ningredientScope= " + ((genOperationInstance.getIngredientScope() != null)
							? genOperationInstance.getIngredientScope() : "-");

				}
			}
			line += "\nvalidation=" + solutionVariant.getValidationResult().toString();
			PatchDiffCalculator cdiff = new PatchDiffCalculator();
			String diffPatch = cdiff.getDiff(getProjectFacade(), solutionVariant);
			line += "\ndiffpatch=" + diffPatch;
		}
		return line;
	}

	public List<ProgramVariant> getSolutions() {
		return solutions;
	}

	public FaultLocalizationStrategy getFaultLocalization() {
		return faultLocalization;
	}

	public void setFaultLocalization(FaultLocalizationStrategy faultLocalization) {
		this.faultLocalization = faultLocalization;
	}

	public ProjectRepairFacade getProjectFacade() {
		return projectFacade;
	}

	public OperatorSpace getOperatorSpace() {
		return operatorSpace;
	}

	public void setOperatorSpace(OperatorSpace operatorSpace) {
		this.operatorSpace = operatorSpace;
	}

	public SolutionVariantSortCriterion getPatchSortCriterion() {
		return patchSortCriterion;
	}

	public void setPatchSortCriterion(SolutionVariantSortCriterion patchSortCriterion) {
		this.patchSortCriterion = patchSortCriterion;
	}

	public void setFitnessFunction(FitnessFunction fitnessFunction) {
		this.fitnessFunction = fitnessFunction;
	}

	public FitnessFunction getFitnessFunction() {
		return fitnessFunction;
	}

	public VariantCompiler getCompiler() {
		return compiler;
	}

	public void setCompiler(VariantCompiler compiler) {
		this.compiler = compiler;
	}

	/**
	 * Load the extension Points according to the requirements of the engine
	 * 
	 * @throws Exception
	 */
	public void loadExtensionPoints() throws Exception {

		// Fault localization

		this.setFaultLocalization(
				(FaultLocalizationStrategy) PlugInLoader.loadPlugin(ExtensionPoints.FAULT_LOCALIZATION));

		// Fault localization
		this.setFitnessFunction((FitnessFunction) PlugInLoader.loadPlugin(ExtensionPoints.FITNESS_FUNCTION));

		this.setCompiler((VariantCompiler) PlugInLoader.loadPlugin(ExtensionPoints.COMPILER));

		// Population controller
		this.setPopulationControler(
				(PopulationController) PlugInLoader.loadPlugin(ExtensionPoints.POPULATION_CONTROLLER));
		//

		// We do the first validation using the standard validation (test suite
		// process)
		this.setProgramValidator(new ProcessValidator());

		// After initializing population, we set up specific validation
		// mechanism
		// Select the kind of validation of a variant.
		String validationArgument = ConfigurationProperties.properties.getProperty("validation");
		if (validationArgument.equals("evosuite")) {
			ProcessEvoSuiteValidator validator = new ProcessEvoSuiteValidator();
			this.setProgramValidator(validator);
		} else
		// if validation is different to default (process)
		if (!validationArgument.equals("process")) {
			this.setProgramValidator((ProgramValidator) PlugInLoader.loadPlugin(ExtensionPoints.VALIDATION));
		}

	};

}
