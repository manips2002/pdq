package uk.ac.ox.cs.pdq.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.Logger;

import com.google.common.collect.Sets;

import uk.ac.ox.cs.pdq.db.sql.DerbyStatementBuilder;
import uk.ac.ox.cs.pdq.db.sql.ExecuteSQLQueryThread;
import uk.ac.ox.cs.pdq.db.sql.ExecuteSynchronousSQLUpdateThread;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Formula;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Variable;
import uk.ac.ox.cs.pdq.util.LimitReachedException;
import uk.ac.ox.cs.pdq.util.LimitReachedException.Reasons;
import uk.ac.ox.cs.pdq.util.Utility;
/**
 * 
 * A database instance is a set of facts stored in an RDBMS. 
 * This object provides basic functionalities, for storing, indexing querying and deleting a database instance.
 * 
 * @author Gabor
 */
public abstract class DatabaseInstance implements Instance {
	protected static Logger log = Logger.getLogger(DatabaseInstance.class);
	
	/** Number of parallel threads. **/
	protected final long timeout = 3600000;
	protected final TimeUnit unit = TimeUnit.MILLISECONDS;
	protected final static int insertCacheSize = 1000; 

	protected Set<String> existingIndices =  new LinkedHashSet<String>();

	/** Statements that drop the query indices. */
	Set<String> dropQueryIndexStatements = Sets.newLinkedHashSet();
	//------------------------------------------------------//

	protected DatabaseConnection databaseConnection;
	private ExecutorService executorService = null;

	public DatabaseInstance(DatabaseConnection databaseConnection) {
		this.databaseConnection = databaseConnection;
	}

	public void addFacts(Collection<Atom> facts) {
		try {
			Queue<String> queries = new ConcurrentLinkedQueue<>();
			if(this.databaseConnection.getSQLStatementBuilder() instanceof DerbyStatementBuilder) {
				queries.addAll(this.databaseConnection.getSQLStatementBuilder().createInsertStatements(facts, this.databaseConnection.getRelationNamesToDatabaseTables()));
			}
			else {
				Map<Predicate, List<Atom>> clusters = Utility.clusterAtomsWithSamePredicateName(facts);
				//Find the total number of tuples that will be inserted in the database
				int totalTuples = facts.size();
				int tuplesPerThread;
				if(totalTuples < this.databaseConnection.getNumberOfSynchronousConnections()) {
					tuplesPerThread = totalTuples;
				}
				else {
					tuplesPerThread = (int) Math.ceil(totalTuples / this.databaseConnection.getNumberOfSynchronousConnections());
				}
				if(tuplesPerThread > insertCacheSize) {
					tuplesPerThread = insertCacheSize;
				}
				for(Entry<Predicate, List<Atom>> entry:clusters.entrySet()) {
					Predicate predicate = entry.getKey();
					List<Atom> clusterFacts = entry.getValue();
					while(!clusterFacts.isEmpty()) {
						int position = tuplesPerThread < clusterFacts.size() ? tuplesPerThread:clusterFacts.size();
						List<Atom> subList = clusterFacts.subList(0, position);
						queries.add(this.databaseConnection.getSQLStatementBuilder().createBulkInsertStatement(predicate, subList));
						subList.clear();
					}
				}
				clusters.clear();
			}
			executeUpdates(queries);
		} catch(Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}


	/** Utiliti function for add and delete fact updates.
	 * @param queries
	 */
	private void executeUpdates(Queue<String> queries) {		
		try {
			if (executorService==null) {
				//	Create a pool of threads to run in parallel
				executorService = Executors.newFixedThreadPool(this.databaseConnection.getNumberOfSynchronousConnections());
			}
			List<Callable<Boolean>> threads = new ArrayList<>();
			for(int j = 0; j < this.databaseConnection.getNumberOfSynchronousConnections(); ++j) {
				//Create the threads that will run the database update statements
				threads.add(new ExecuteSynchronousSQLUpdateThread(queries, this.getDatabaseConnection().getSynchronousConnections(j)));
			}
			long start = System.currentTimeMillis();
			try {
				for(Future<Boolean> output:executorService.invokeAll(threads, this.timeout, this.unit)){
					output.get();
				}
			} catch(java.util.concurrent.CancellationException e) {
				executorService.shutdownNow();
				if (this.timeout <= (System.currentTimeMillis() - start)) {
					try {
						throw new LimitReachedException(Reasons.TIMEOUT);
					} catch (LimitReachedException e1) {
						throw new RuntimeException(e1);
					}
				}
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		} 
	}

	public void deleteFacts(Collection<Atom> facts) {
		try {
			Queue<String> queries = new ConcurrentLinkedQueue<>();
			Map<Predicate, List<Atom>> clusters = Utility.clusterAtomsWithSamePredicateName(facts);
	
			//Find the total number of tuples that will be deleted from the database
			int totalTuples = facts.size();
			int tuplesPerThread;
			if(totalTuples < this.databaseConnection.getNumberOfSynchronousConnections()) {
				tuplesPerThread = totalTuples;
			}
			else {
				tuplesPerThread = (int) Math.ceil(totalTuples / this.databaseConnection.getNumberOfSynchronousConnections());
			}
			if(tuplesPerThread > insertCacheSize) {
				tuplesPerThread = insertCacheSize;
			}
			for(Entry<Predicate, List<Atom>> entry:clusters.entrySet()) {
				Predicate predicate = entry.getKey();
				List<Atom> clusterFacts = entry.getValue();
				while(!clusterFacts.isEmpty()) {
					int position = tuplesPerThread > clusterFacts.size() ? clusterFacts.size():tuplesPerThread;
					List<Atom> subList = clusterFacts.subList(0, position);
					queries.add(this.databaseConnection.getSQLStatementBuilder().createBulkDeleteStatement(predicate, subList, this.databaseConnection.getRelationNamesToDatabaseTables()));
					subList.clear();
				}
			}
			executeUpdates(queries);
		} catch(Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}

	/** Main query function used by getTriggers and getMatches. The correct argument is a mystery. 
	 * 
	 * @param queries A queue of triples, representing a query.bEach triple holds, 
	 * - the query or the constraint we want to detect homomorphisms for
	 * - the SQL query expression we will execute over the database
	 * - a map of projected variables
	 * @return matches of the queries
	 */
	protected List<Match> answerQueries(Queue<Triple<Formula, String, LinkedHashMap<String, Variable>>> queries) {
		List<Match> result = new LinkedList<>();
		//Run the SQL query statements in multiple threads
		try {
			//Create a pool of threads to run in parallel
			if (executorService==null)
				executorService = Executors.newFixedThreadPool(this.databaseConnection.getNumberOfSynchronousConnections());
			List<Callable<List<Match>>> threads = new ArrayList<>();
			for(int j = 0; j < this.databaseConnection.getNumberOfSynchronousConnections(); ++j) {
				//Create the threads that will run the database queries
				String dbName = this.databaseConnection.getSQLStatementBuilder().getDatabaseName();
				if (this.databaseConnection.getDatabaseParameters().getDatabaseDriver().contains("derby")) {
					// derby doesn't like the USE databaseName command, so we switch it off.
					dbName = null;
				}
				threads.add(new ExecuteSQLQueryThread(queries, this.databaseConnection.getSchema().getConstants(), this.getDatabaseConnection().getSynchronousConnections(j), dbName));
			}
			long start = System.currentTimeMillis();
			try {
				for(Future<List<Match>> output:executorService.invokeAll(threads, this.timeout, this.unit)){
					result.addAll(output.get());
				}
			} catch(java.util.concurrent.CancellationException e) {
				if (this.timeout <= (System.currentTimeMillis() - start)) {
					try {
						throw new LimitReachedException(Reasons.TIMEOUT);
					} catch (LimitReachedException e1) {
						executorService.shutdownNow();
						executorService = Executors.newFixedThreadPool(this.databaseConnection.getNumberOfSynchronousConnections());
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				return null;
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return null;
		}
		return result;
	}
	
	protected DatabaseConnection getDatabaseConnection() {
		return this.databaseConnection;
	}

	/** Shuts this databaseInstance down. Closes all connections and threadpools.
	 * @throws Exception
	 */
	public void close() throws Exception {
		if (executorService!=null)
			executorService.shutdownNow();
		//is this the right thing to do?
		this.databaseConnection.close();
	}

}
