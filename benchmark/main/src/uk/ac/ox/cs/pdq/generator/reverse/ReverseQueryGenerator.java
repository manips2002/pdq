package uk.ac.ox.cs.pdq.generator.reverse;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;

import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.fol.Query;
import uk.ac.ox.cs.pdq.io.xml.QueryReader;
import uk.ac.ox.cs.pdq.io.xml.SchemaReader;
import uk.ac.ox.cs.pdq.planner.PlannerParameters;
import uk.ac.ox.cs.pdq.planner.accessible.AccessibleSchema;
import uk.ac.ox.cs.pdq.planner.reasoning.ReasonerFactory;
import uk.ac.ox.cs.pdq.planner.reasoning.chase.accessiblestate.AccessibleChaseState;
import uk.ac.ox.cs.pdq.planner.reasoning.chase.accessiblestate.AccessibleDatabaseListState;
import uk.ac.ox.cs.pdq.reasoning.ReasoningParameters;
import uk.ac.ox.cs.pdq.reasoning.ReasoningParameters.ReasoningTypes;
import uk.ac.ox.cs.pdq.reasoning.chase.Chaser;
import uk.ac.ox.cs.pdq.reasoning.homomorphism.DBHomomorphismManager;
import uk.ac.ox.cs.pdq.reasoning.homomorphism.HomomorphismDetector;
import uk.ac.ox.cs.pdq.reasoning.homomorphism.HomomorphismManagerFactory;

import com.google.common.eventbus.EventBus;

public class ReverseQueryGenerator implements Runnable {

	/** Logger. */
	private static Logger log = Logger.getLogger(ReverseQueryGenerator.class);

	private static Integer seeds = 0;

	private final Integer threadId;
	private final Schema schema;
	private final ConjunctiveQuery query;

	private Integer done = 0;
	
	public ReverseQueryGenerator(Integer threadId, Schema schema, ConjunctiveQuery query) {
		this.threadId = threadId;
		this.schema = schema;
		this.query = query;
	}

	public static synchronized Integer getSeed() {
		return seeds++;
	}

	@Override
	public void run() {
		try {
			PlannerParameters params = new PlannerParameters();
			ReasoningParameters reasoningParams = new ReasoningParameters();
			this.schema.updateConstants(this.query.getSchemaConstants());
			AccessibleSchema accessibleSchema = new AccessibleSchema(this.schema);
			EventBus eb = new EventBus();
			Chaser reasoner = new ReasonerFactory(
					eb, 
					true, 
					reasoningParams).getInstance(); 
			MatchMaker mm = new MatchMaker(
					new LengthBasedQuerySelector(2, 6),
					new ConstantRatioQuerySelector(0.2),
					new CrossProductFreeQuerySelector(),
					new NoAllFreeAccessQuerySelector(),
					new DubiousRepeatedPredicateQuerySelector(),
					new JoinOnVariableQuerySelector(),
					new DiversityQuerySelector()
//					new UnanswerableQuerySelector(params, this.schema)
					);
			eb.register(mm);
			Runtime.getRuntime().addShutdownHook(new MatchReport(mm));

			Query<?> accessibleQuery = accessibleSchema.accessible(this.query);
			try(HomomorphismDetector detector =
				new HomomorphismManagerFactory().getInstance(accessibleSchema, accessibleQuery, reasoningParams)) {
				
				AccessibleChaseState state =  
				(uk.ac.ox.cs.pdq.planner.reasoning.chase.accessiblestate.AccessibleChaseState) new AccessibleDatabaseListState(query, accessibleSchema, (DBHomomorphismManager) detector);
				
				log.info("Phase 1");
				reasoner.reasonUntilTermination(state, accessibleQuery, this.schema.getDependencies());
				log.info("Phase 2");
				reasoner.reasonUntilTermination(state, accessibleQuery, CollectionUtils.union(
						accessibleSchema.getAccessibilityAxioms(),
						accessibleSchema.getInferredAccessibilityAxioms()));
				log.info("Reasoning complete.");
		}
			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
	}

	public static void main(String... args) {
		long timeout = 120000;
		try(FileInputStream fis = new FileInputStream("../pdq.benchmark/test/dag/web/schemas/schema-all.xml");
			FileInputStream qis = new FileInputStream("../pdq.benchmark/test/dag/web/queries/query-all.xml")) {
			Schema schema = new SchemaReader().read(fis);
			ConjunctiveQuery query = new QueryReader(schema).read(qis);
			ExecutorService exec = Executors.newFixedThreadPool(2);
			exec.submit(new ReverseQueryGenerator(1, schema, query));
			exec.submit(new ShowStopper(timeout));
			exec.shutdown();
		} catch (IOException e) {
			log.error(e.getMessage(),e);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
	}

	/**
	 * Report the finding of a MatchMaker
	 * 
	 * @author Julien Leblay
	 */
	public static class MatchReport extends Thread {

		private final MatchMaker mm;

		public MatchReport(MatchMaker mm) {
			this.mm = mm;
		}
		
		@Override
		public void run() {
			this.mm.report();
		}
	}
		
	
	/**
	 * This is aimed at forcing the end of a test, thus by-passing the internal 
	 * search timeout mechanism.
	 * 
	 * @author Julien Leblay
	 *
	 */
	public static class ShowStopper extends Thread {
	
		private long timeout = -1L;
		
		public ShowStopper(long timeout) {
			this.setDaemon(true);
			this.timeout = timeout;
		}
		
		@Override
		public void run() {
			if (this.timeout > 0l) {
				try {
					Thread.sleep(this.timeout);
					Runtime.getRuntime().exit(-1);
				} catch (InterruptedException e) {
					log.error(e.getMessage(),e);
				}
			}
		}
	}
}
