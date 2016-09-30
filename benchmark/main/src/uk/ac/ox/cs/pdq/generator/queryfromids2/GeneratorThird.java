package uk.ac.ox.cs.pdq.generator.queryfromids2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import uk.ac.ox.cs.pdq.benchmark.BenchmarkParameters;
import uk.ac.ox.cs.pdq.cost.CostParameters;
import uk.ac.ox.cs.pdq.db.ReasoningParameters;
import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.generator.AbstractGenerator;
import uk.ac.ox.cs.pdq.generator.tgdsfromquery.SchemaGeneratorFirst;
import uk.ac.ox.cs.pdq.io.xml.QueryReader;
import uk.ac.ox.cs.pdq.io.xml.QueryWriter;
import uk.ac.ox.cs.pdq.io.xml.SchemaReader;
import uk.ac.ox.cs.pdq.io.xml.SchemaWriter;
import uk.ac.ox.cs.pdq.planner.ExplorationSetUp;
import uk.ac.ox.cs.pdq.planner.PlannerException;
import uk.ac.ox.cs.pdq.planner.PlannerParameters;

// TODO: Auto-generated Javadoc
/**
 * Creates inclusion dependencies and then queries using the previously created dependencies.
 *
 * @author Efthymia Tsamoura
 */
public class GeneratorThird extends AbstractGenerator{

	/** The log. */
	private static Logger log = Logger.getLogger(GeneratorThird.class);
	
	/**
	 * Instantiates a new generator third.
	 *
	 * @param parameters the parameters
	 * @param schemaFile the schema file
	 * @param queryFile the query file
	 * @param out the out
	 */
	public GeneratorThird(BenchmarkParameters parameters, String schemaFile, String queryFile, PrintStream out) {
		super(parameters, schemaFile, queryFile, out);
	}

	/* (non-Javadoc)
	 * @see uk.ac.ox.cs.pdq.generator.AbstractGenerator#make()
	 */
	@Override
	public void make() throws IOException {
		// Load the statistic collector/logger
		Schema schema = null;
		ConjunctiveQuery query = null;
		if (this.schemaFile != null && !this.schemaFile.trim().isEmpty()) {
			try (FileInputStream fis = new FileInputStream(this.schemaFile)) {
				schema = Schema.builder(new SchemaReader().read(fis)).build();
			}
			query = new QueryGeneratorThird(schema, this.parameters).generate();
			new QueryWriter().write(this.out, query);
			return;
		}
		schema = this.makeSchema();
		new SchemaWriter().write(this.out, schema);
	}

	/**
	 * Makes a query from the given parameters.
	 *
	 * @return the schema
	 */
	public Schema makeSchema() {
		// Loading schema
		Schema schema = new SchemaGeneratorFirst(this.parameters).generate();
		// Creation random fk/inclusion dependencies
		schema = new DependencyGeneratorThird(schema, this.parameters).generate();
		if (schema.getRelations().isEmpty()) {
			throw new IllegalStateException("Input schema is empty. Cannot proceed.");
		}
		return schema;
	}

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws SQLException 
	 */
	public static void main (String... args) throws SQLException {
		try(FileInputStream fis = new FileInputStream("test/input/web-schema.xml")) {
			PlannerParameters planParams = new PlannerParameters();
			CostParameters costParams = new CostParameters();
			ReasoningParameters reasoningParams = new ReasoningParameters();
			Schema schema = new SchemaReader().read(fis);
			String[] queryFiles = new File("test/output/").list(new FilenameFilter() {
				@Override public boolean accept(File dir, String name) {
					return name.startsWith("query") && name.endsWith(".xml");
				}});
			Arrays.sort(queryFiles);
			for (String queryFile: queryFiles) {
				try(FileInputStream qis = new FileInputStream("test/output/" + queryFile)) {
					ConjunctiveQuery query = new QueryReader(schema).read(qis);
					
					log.trace(queryFile);
					planParams.setMaxDepth(1);
					ExplorationSetUp planner = new ExplorationSetUp(planParams, costParams, reasoningParams, schema);
					if (planner.search(query) != null) {
						log.trace(" not answerable without constraints");
					}
					planParams.setMaxDepth(10);
					planner = new ExplorationSetUp(planParams, costParams, reasoningParams, schema);
					if (planner.search(query) != null) {
						log.trace(", not answerable with constraints (depth=10)");
					}
					log.trace("\n");
				}
			}
		} catch (PlannerException e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage(),e);
		} catch (FileNotFoundException e) {
			log.error(e.getMessage(),e);
		} catch (IOException e) {
			log.error(e.getMessage(),e);
		}
	}
}
