/*
 * 
 */
package uk.ac.ox.cs.pdq.planner;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import uk.ac.ox.cs.pdq.cost.CostParameters;
import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.io.xml.PlanWriter;
import uk.ac.ox.cs.pdq.io.xml.QueryReader;
import uk.ac.ox.cs.pdq.io.xml.SchemaReader;
import uk.ac.ox.cs.pdq.logging.ProgressLogger;
import uk.ac.ox.cs.pdq.logging.SimpleProgressLogger;
import uk.ac.ox.cs.pdq.plan.Plan;
import uk.ac.ox.cs.pdq.planner.logging.IntervalEventDrivenLogger;
import uk.ac.ox.cs.pdq.reasoning.ReasoningParameters;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

// TODO: Auto-generated Javadoc
/**
 * Bootstrapping class for starting the planner. 
 * 
 * @author Julien Leblay
 */
public class Bootstrap {

	/** Logger. */
	private static Logger log = Logger.getLogger(Bootstrap.class); 
	
	/** The Constant PROGRAM_NAME. */
	private static final String PROGRAM_NAME = "pdq-cardinality-<version>.jar";
	
	/** The help. */
	@Parameter(names = { "-h", "--help" }, help = true, description = "Displays this help message.")
	private boolean help;
	
	/**
	 * Checks if is help.
	 *
	 * @return true, if is help
	 */
	public boolean isHelp() {
		return this.help;
	}
	
	/** The schema path. */
	@Parameter(names = { "-s", "--schema" }, required = true,
			 validateWith=FileValidator.class,
		description ="Path to the input schema definition file.")
	private String schemaPath;
	
	/**
	 * Gets the schema path.
	 *
	 * @return the schema path
	 */
	public String getSchemaPath() {
		return this.schemaPath;
	}
	
	/** The query path. */
	@Parameter(names = { "-q", "--query" }, required = true,
			 validateWith=FileValidator.class,
		description ="Path to the input query definition file.")
	private String queryPath;
	
	/**
	 * Gets the query path.
	 *
	 * @return the query path
	 */
	public String getQueryPath() {
		return this.queryPath;
	}
	
	/** The config file. */
	@Parameter(names = { "-c", "--config" }, validateWith=FileValidator.class,
			description = "Directory where to look for configuration files. "
			+ "Default is the current directory.")
	private File configFile;
	
	/**
	 * Gets the config file.
	 *
	 * @return the config file
	 */
	public File getConfigFile() {
		return this.configFile;
	}
	
	/** The verbose. */
	@Parameter(names = { "-v", "--verbose" }, required = false,
		description ="Path to the input query definition file.")
	private boolean verbose = false;
	
	/**
	 * Checks if is verbose.
	 *
	 * @return true, if is verbose
	 */
	public boolean isVerbose() {
		return this.verbose;
	}

	/** The dynamic params. */
	@DynamicParameter(names = "-D", description = "Dynamic parameters. Override values defined in the configuration files.")
	protected Map<String, String> dynamicParams = new LinkedHashMap<>();

	/**
	 * Initialize the Bootstrap by reading command line parameters, and running
	 * the planner on them.
	 * @param args String[]
	 */
	private Bootstrap(String... args) {
		JCommander jc = new JCommander(this);
		jc.setProgramName(PROGRAM_NAME);
		try {
			jc.parse(args);
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			jc.usage();
			return;
		}
		if (this.isHelp()) {
			jc.usage();
			return;
		}
		run();
	}

	/**
	 * Runs the planner from the input parameters, schema and query.
	 */
	public void run() {
		PlannerParameters planParams = this.getConfigFile() != null ?
				new PlannerParameters(this.getConfigFile()) :
				new PlannerParameters() ;
		
				CostParameters costParams = this.getConfigFile() != null ?
				new CostParameters(this.getConfigFile()) :
				new CostParameters() ;
				
		ReasoningParameters reasoningParams = this.getConfigFile() != null ?
				new ReasoningParameters(this.getConfigFile()) :
				new ReasoningParameters() ;
		
		for (String k : this.dynamicParams.keySet()) {
			planParams.set(k, this.dynamicParams.get(k));
		}
		try(FileInputStream sis = new FileInputStream(this.getSchemaPath());
			FileInputStream qis = new FileInputStream(this.getQueryPath())) {

			Schema schema = new SchemaReader().read(sis);
			ConjunctiveQuery query = new QueryReader(schema).read(qis);
			Plan plan = null;
			try(ProgressLogger pLog = new SimpleProgressLogger(System.out)) {
				Planner planner = new Planner(planParams, costParams, reasoningParams, schema, query);
				if (verbose) {
					planner.registerEventHandler(
							new IntervalEventDrivenLogger(
									pLog, planParams.getLogIntervals(),
									planParams.getShortLogIntervals()));
				}
				plan = planner.search(query);
			}
		} catch (Throwable e) {
			log.error("Planning aborted: " + e.getMessage(),e);
			System.exit(-1);
		}
	}

	/**
	 * Filters out files that do not exist or are directories.
	 * @author Julien LEBLAY
	 */
	public static class FileValidator implements IParameterValidator {
		
		/* (non-Javadoc)
		 * @see com.beust.jcommander.IParameterValidator#validate(java.lang.String, java.lang.String)
		 */
		@Override
		public void validate(String name, String value) throws ParameterException {
			try {
				File f = new File(value);
				if (!f.exists() || f.isDirectory()) {
					throw new ParameterException(name + " must be a valid configuration file.");
				}
			} catch (Exception e) {
				throw new ParameterException(name + " must be a valid configuration file.");
			}
		}
	}

	/**
	 * Instantiates the bootstrap.
	 *
	 * @param args String[]
	 */
	public static void main(String... args) {
		new Bootstrap(args);
	}
}