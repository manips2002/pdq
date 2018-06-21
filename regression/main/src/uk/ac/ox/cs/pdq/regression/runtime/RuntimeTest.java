package uk.ac.ox.cs.pdq.regression.runtime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import uk.ac.ox.cs.pdq.algebra.RelationalTerm;
import uk.ac.ox.cs.pdq.cost.Cost;
import uk.ac.ox.cs.pdq.cost.CostParameters;
import uk.ac.ox.cs.pdq.databasemanagement.DatabaseParameters;
import uk.ac.ox.cs.pdq.datasources.AccessException;
import uk.ac.ox.cs.pdq.datasources.accessrepository.AccessRepository;
import uk.ac.ox.cs.pdq.datasources.io.jaxb.DbIOManager;
import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.io.jaxb.IOManager;
import uk.ac.ox.cs.pdq.logging.ProgressLogger;
import uk.ac.ox.cs.pdq.logging.SimpleProgressLogger;
import uk.ac.ox.cs.pdq.planner.ExplorationSetUp;
import uk.ac.ox.cs.pdq.planner.PlannerParameters;
import uk.ac.ox.cs.pdq.planner.logging.IntervalEventDrivenLogger;
import uk.ac.ox.cs.pdq.reasoning.ReasoningParameters;
import uk.ac.ox.cs.pdq.regression.Bootstrap.Command;
import uk.ac.ox.cs.pdq.regression.RegressionParameters;
import uk.ac.ox.cs.pdq.regression.RegressionTest;
import uk.ac.ox.cs.pdq.regression.RegressionTestException;
import uk.ac.ox.cs.pdq.regression.acceptance.AcceptanceCriterion.AcceptanceResult;
import uk.ac.ox.cs.pdq.regression.acceptance.ExpectedCardinalityAcceptanceCheck;
import uk.ac.ox.cs.pdq.regression.planner.PlannerTestUtilities;
import uk.ac.ox.cs.pdq.runtime.Runtime;
import uk.ac.ox.cs.pdq.runtime.RuntimeParameters;

/**
 * Runs regression tests for the runtime, evaluated plans may come in
 * pre-serialized plan files or directly from the planner.
 * 
 * @author Julien Leblay
 */
public class RuntimeTest extends RegressionTest {

	/** Runner's logger. */
	private static Logger log = Logger.getLogger(RuntimeTest.class);

	/**  File name where planning related parameters must be stored in a test case directory. */
	private static final String PLAN_PARAMETERS_FILE = "case.properties";

	/**  File name where the schema must be stored in a test case directory. */
	private static final String SCHEMA_FILE = "schema.xml";

	/**  File name where the query must be stored in a test case directory. */
	private static final String QUERY_FILE = "query.xml";

	/**  File name where the expected plan must be stored in a test case directory. */
	private static final String PLAN_FILE = "expected-plan.xml";

	/**  File name where the input data must be stored in a test case directory. */
	private static final String DATA_FILE_EXTENSION = ".csv";

	/** The full. */
	private final boolean full;

	/**
	 * The Class RuntimeTestCommand.
	 */
	@Parameters(separators = ",", commandDescription = "Runs regression tests on the runtime libraries.")
	public static class RuntimeTestCommand extends Command {

		/** The full. */
		@Parameter(names = { "-f", "--full-run" }, required = false,
				description = "If true, the planner is used to create a plan, otherwise the plan is read from file. Default is false")
		private boolean full = false;

		/**
		 * Instantiates a new runtime test command.
		 */
		public RuntimeTestCommand() {
			super("runtime");
		}

		/* (non-Javadoc)
		 * @see uk.ac.ox.cs.pdq.test.Bootstrap.Command#execute()
		 */
		@Override
		public void execute() throws RegressionTestException, IOException, ReflectiveOperationException {
			new RuntimeTest(this.full).recursiveRun(new File(getInput()));
		}
	}

	/**
	 * Sets up a regression test for the given test case directory.
	 *
	 * @param fr the fr
	 * @param dv the dv
	 * @throws ReflectiveOperationException the reflective operation exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws RegressionTestException the regression test exception
	 */
	public RuntimeTest(boolean fr) throws ReflectiveOperationException, IOException, RegressionTestException {
		this.full = fr;
	}

	/**
	 * Runs all the test case in the given directory.
	 *
	 * @param directory the directory
	 * @return boolean
	 * @throws RegressionTestException the regression test exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ReflectiveOperationException the reflective operation exception
	 */
	@Override
	protected boolean run(File directory) throws RegressionTestException, IOException, ReflectiveOperationException {
		return this.loadCase(directory, this.full);
	}

	/**
	 * Obtain plan.
	 *
	 * @param directory File
	 * @param schema Schema
	 * @param query Query
	 * @param full boolean
	 * @return 
	 * @return Plan
	 */
	private  Entry<RelationalTerm, Cost> obtainPlan(File directory, Schema schema, ConjunctiveQuery query, boolean full) {
		if (full) {
			PlannerParameters plParams = new PlannerParameters(new File(directory.getAbsolutePath() + '/' + PLAN_PARAMETERS_FILE));
			CostParameters costParams = new CostParameters(new File(directory.getAbsolutePath() + '/' + PLAN_PARAMETERS_FILE));
			ReasoningParameters reasoningParams = new ReasoningParameters(new File(directory.getAbsolutePath() + '/' + PLAN_PARAMETERS_FILE));
			DatabaseParameters dbParams = new DatabaseParameters(new File(directory.getAbsolutePath() + '/' + PLAN_PARAMETERS_FILE));
			try (ProgressLogger pLog = new SimpleProgressLogger(this.out)) {
				ExplorationSetUp planner = new ExplorationSetUp(plParams, costParams, reasoningParams, dbParams, schema);
				planner.registerEventHandler(new IntervalEventDrivenLogger(pLog, plParams.getLogIntervals(), plParams.getShortLogIntervals()));
				return planner.search(query);
			} catch (Exception e) {
				log.debug(e);
				return null;
			}
		}
		return PlannerTestUtilities.obtainPlan(directory.getAbsolutePath() + '/' + PLAN_FILE, schema);
	}

	/**
	 * Runs a single test case base on the.
	 *
	 * @param directory the directory
	 * @param full boolean
	 * @return boolean
	 * @throws ReflectiveOperationException the reflective operation exception
	 * @throws AccessException the access exception
	 */
	private boolean loadCase(File directory, boolean full) throws ReflectiveOperationException {
		boolean result = true;
		try {
			this.out.println("Starting case '" + directory.getAbsolutePath() + "'");

			// Loading schema & query
			Schema schema = DbIOManager.importSchema(new File(directory.getAbsolutePath() + '/' + SCHEMA_FILE));
			ConjunctiveQuery query = IOManager.importQuery(new File(directory.getAbsolutePath() + '/' + QUERY_FILE));
			if (schema == null || query == null) {
				this.out.println("\tSKIP: Could not read schema/query in " + directory.getAbsolutePath());
				return true;
			}
			Entry<RelationalTerm, Cost> plan = this.obtainPlan(directory, schema, query, full);
			if (plan == null) {
				this.out.println("\tSKIP: Plan is empty in " + directory.getAbsolutePath());
				return true;
			}

			// Loading data
			Collection<Atom> facts = null;
			File[] dataFiles = directory.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(DATA_FILE_EXTENSION);
				}
			});
			if (dataFiles != null && dataFiles.length > 0) {
				facts = new ArrayList<>();
				for (File dataFile : dataFiles) {
					facts = DbIOManager.importFacts(schema.getRelation(dataFile.getName().substring(0, dataFile.getName().length()-4)), dataFile);
					this.out.println("\tData file '" + dataFile.getAbsolutePath() + "'");
				}
				result &= this.run(directory, schema, query, plan.getKey(), facts);
			} else {
				this.out.println("\tUsing underlying database ");
				result &= this.run(directory, schema, query, plan.getKey(), facts);
			}

		} catch (FileNotFoundException e) {
			log.debug(e);
			this.out.println("SKIP: '" + directory.getAbsolutePath() + "' (not a case directory)");
		} catch (Exception e) {
			e.printStackTrace(this.out);
			this.out.println("\tEXCEPTION: " + e.getClass().getSimpleName() + " " + e.getMessage());
			return false;
		}
		return result;
	}

	/**
	 * Runs a single case.
	 *
	 * @param directory the directory
	 * @param s the s
	 * @param q the q
	 * @param p the p
	 * @param f the f
	 * @return boolean
	 * @throws EvaluationException the evaluation exception
	 * @throws AccessException the access exception
	 */
	private boolean run(File directory, Schema s, ConjunctiveQuery q, RelationalTerm p, Collection<Atom> f)
			throws Exception {
		RuntimeParameters params = new RuntimeParameters(new File(directory.getAbsolutePath() + '/' + PLAN_PARAMETERS_FILE));
		Runtime runtime = new Runtime(params, s, f);

		RegressionParameters regParams = new RegressionParameters(new File(directory.getAbsolutePath() + '/' + PLAN_PARAMETERS_FILE));
		if (!regParams.getSkipRuntime()) {
			File accesses = new File(directory,params.getAccessDirectory());
			if (!accesses.exists()) {
				accesses = new File(params.getAccessDirectory());
			}
			if (accesses.exists() && accesses.isDirectory() && accesses.list().length>0) {
				runtime.setAccessRepository(AccessRepository.getRepository(accesses.getAbsolutePath()));
			}
			AcceptanceResult result = new ExpectedCardinalityAcceptanceCheck()
					.check(regParams.getExpectedCardinality(), runtime.evaluatePlan(p));
			result.report(this.out);
		} else {
			this.out.println("\tSKIP: runtime test explicitly skipped.");
		}
		return true;
	}

	/**
	 * Gets the full run.
	 *
	 * @return the value of the "full-run" argument
	 */
	public boolean getFullRun() {
		return this.full;
	}
}
