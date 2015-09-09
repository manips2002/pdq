package uk.ac.ox.cs.pdq.cost;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.io.xml.QueryReader;
import uk.ac.ox.cs.pdq.io.xml.SchemaReader;
import uk.ac.ox.cs.pdq.reasoning.chase.state.ChaseState;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * Bootstrapping class for starting the reasoner. 
 * 
 * @author Efthymia Tsamoura
 */
public class Bootstrap {

	/** Logger. */
	private static Logger log = Logger.getLogger(Bootstrap.class); 
	
	private static final String PROGRAM_NAME = "pdq-cost-<version>.jar";
	
	@Parameter(names = { "-h", "--help" }, help = true, description = "Displays this help message.")
	private boolean help;
	public boolean isHelp() {
		return this.help;
	}
	
	@Parameter(names = { "-s", "--schema" }, required = true,
			 validateWith=FileValidator.class,
		description ="Path to the input schema definition file.")
	private String schemaPath;
	public String getSchemaPath() {
		return this.schemaPath;
	}
	
	@Parameter(names = { "-q", "--query" }, required = true,
			 validateWith=FileValidator.class,
		description ="Path to the input query definition file.")
	private String queryPath;
	public String getQueryPath() {
		return this.queryPath;
	}
	
	@Parameter(names = { "-c", "--config" }, validateWith=FileValidator.class,
			description = "Directory where to look for configuration files. "
			+ "Default is the current directory.")
	private File configFile;
	public File getConfigFile() {
		return this.configFile;
	}
	
	@Parameter(names = { "-v", "--verbose" }, required = false,
		description ="Path to the input query definition file.")
	private boolean verbose = false;
	public boolean isVerbose() {
		return this.verbose;
	}

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
	public <S extends ChaseState> void run() {				
		CostParameters reasoningParams = this.getConfigFile() != null ?
				new CostParameters(this.getConfigFile()) :
				new CostParameters() ;
		for (String k : this.dynamicParams.keySet()) {
			reasoningParams.set(k, this.dynamicParams.get(k));
		}
		try(FileInputStream sis = new FileInputStream(this.getSchemaPath());
			FileInputStream qis = new FileInputStream(this.getQueryPath())) {

			Schema schema = new SchemaReader().read(sis);
			ConjunctiveQuery query = new QueryReader(schema).read(qis);
			
			if (schema == null || query == null) {
				throw new IllegalStateException("Schema and query must be provided.");
			}
			schema.updateConstants(query.getSchemaConstants());
			
			//TODO Call the cost function
			
			
			
		} catch (Throwable e) {
			log.error("Planning aborted: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Filters out files that do not exist or are directories.
	 * @author Julien LEBLAY
	 */
	public static class FileValidator implements IParameterValidator {
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
	 * Instantiates the bootstrap
	 * @param args String[]
	 */
	public static void main(String... args) {
		new Bootstrap(args);
	}
}
