package uk.ac.ox.cs.pdq.regression;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import uk.ac.ox.cs.pdq.ParametersException;
import uk.ac.ox.cs.pdq.datasources.accessrepository.AccessRepository;
import uk.ac.ox.cs.pdq.regression.planner.PlannerTest.PlannerTestCommand;
import uk.ac.ox.cs.pdq.regression.runtime.RuntimeTest.RuntimeTestCommand;

/**
 * The entry point for the regression package.
 *
 * @author Julien Leblay
 */
public class Bootstrap {

	/** Runner's logger. */
	private static Logger log = Logger.getLogger(Bootstrap.class);
	
	/** Program execution command (to appear un usage message). */
	public static final String PROGRAM_NAME = "java -jar pdq-regression.jar";

	/** Default error code. */
	public static final int ERROR_CODE = -1;

	/** The help. */
	@Parameter(names = { "-h", "--help" }, help = true, description = "Displays this help message.")
	private boolean help;
	
	/** The output stream. usually console or file. Console by default.*/
	protected PrintStream out;

	/**
	 * Sets up a regression test for the given test case directory.
	 *
	 * @param args the command line parameters as given by the main method.
	 */
	public Bootstrap(String... args) {
		AccessRepository.setDefaultLocation("./test/schemas/accesses");
		JCommander jc = new JCommander(this);
		Map<String, Command> commands = new LinkedHashMap<>();
		{
			Command c = new PlannerTestCommand();
			commands.put(c.name, c);
			jc.addCommand(c.name, c);
			c = new RuntimeTestCommand();
			commands.put(c.name, c);
			jc.addCommand(c.name, c);
		}
		jc.setProgramName(Bootstrap.PROGRAM_NAME);
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
		Command o = commands.get(jc.getParsedCommand());
		if (o == null) {
			jc.usage();
			return;
		}
		try {
			o.execute();
		} catch (RegressionTestException | IOException
				| ReflectiveOperationException |ParametersException e) {
			log.error(e.getMessage());
			return;
		}
	}
	
	/**
	 * A command to the regression test bootstrap. Consists of an action and a 
	 * associated parameters. 
	 * 
	 * @author Julien Leblay
	 *
	 */
	public static abstract class Command {

		/**
		 * Command name. Must not contain arguments or parameters
		 */
		public final String name;
		
		/**
		 * Command inputs or parameters
		 */
		@Parameter(names = { "-i", "--input" }, required = true,
				description = "Path to the regression test case directories.",
				validateWith=DirectoryValidator.class)
		private String input;

		/**
		 * Parameter mappings to override in the input.
		 */
		@DynamicParameter(names = "-D", required = false, 
				description = "Force the given parameters across all the test in the suite, "
						+ "ignoring those that may be specified in each parameter file. "
						+ "For instance, '-Dtimeout=10000' force a timeout 10s seconds on all tests.")
		private Map<String, String> params = new HashMap<>();
		
		/**
		 * Instantiates a new command.
		 *
		 * @param name the name
		 */
		public Command(String name) {
			this.name = name;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(name);
			result.append(' ').append(input);
			return result.toString();
					
		}

		/**
		 *
		 * @return gets the path to the initialConfig file to use.
		 */
		public String getInput() {
			return this.input;
		}

		/**
		 * This map will be used to override plannar parameters.
		 * @return the parameter overrides
		 */
		public Map<String, String> getParameterOverrides() {
			return this.params;
		}

		/**
		 * Executes the command action on the list of modules. If the module 
		 * list is empty, the action is performed on the service chaseState itself.
		 *
		 * @throws RegressionTestException the regression test exception
		 * @throws IOException Signals that an I/O exception has occurred.
		 * @throws ReflectiveOperationException the reflective operation exception
		 */
		public abstract void execute() throws RegressionTestException, IOException, ReflectiveOperationException;
	}

	/**
	 * The Class DirectoryValidator.
	 */
	public static class DirectoryValidator implements IParameterValidator {
		
		/* (non-Javadoc)
		 * @see com.beust.jcommander.IParameterValidator#validate(java.lang.String, java.lang.String)
		 */
		@Override
		public void validate(String name, String value) throws ParameterException {
			try {
				File f = new File(value);
				if (!(f.exists() && f.isDirectory())) {
					throw new ParameterException(name + " must be a valid directory.");
				}
			} catch (Exception e) {
				throw new ParameterException(name + " must be a valid directory.");
			}
		}
		
	}

	/**
	 * Checks if called for help.
	 *
	 * @return true if the line command asked for help.
	 */
	public boolean isHelp() {
		return this.help;
	}

	/**
	 * Sets the help message.
	 *
	 * @param help the new help
	 */
	public void setHelp(boolean help) {
		this.help = help;
	}

	/**
	 * Instantiates an experiment and runs it.
	 *
	 * @param args the arguments
	 */
	public static void main(String... args) {
		new Bootstrap(args);
	}
}