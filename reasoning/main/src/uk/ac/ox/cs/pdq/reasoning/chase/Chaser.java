package uk.ac.ox.cs.pdq.reasoning.chase;

import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;

import uk.ac.ox.cs.pdq.db.Dependency;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.fol.Constant;
import uk.ac.ox.cs.pdq.fol.Query;
import uk.ac.ox.cs.pdq.fol.Variable;
import uk.ac.ox.cs.pdq.logging.performance.StatisticsCollector;
import uk.ac.ox.cs.pdq.reasoning.chase.state.ChaseState;
import uk.ac.ox.cs.pdq.reasoning.homomorphism.HomomorphismDetector;

// TODO: Auto-generated Javadoc
/**
 * (From A. C. Onet) 
 * The chase procedure is an iteration of steps that either adds a new tuple to satisfy
	a TGD, either changes the instance to model some equality-generating-dependency,
	or fails when the instance could not be changed to satisfy an equality-generating dependency. 
 * @author Efthymia Tsamoura
 */
public abstract class Chaser {

	/** The log. */
	protected static Logger log = Logger.getLogger(Chaser.class);

	/**  Collects statistics related to chasing *. */
	protected final StatisticsCollector statistics;

	/**
	 * Constructor for Chaser.
	 *
	 * @param statistics the statistics
	 */
	public Chaser(StatisticsCollector statistics) {
		this.statistics = statistics;
	}
	

	/**
	 * Chases the input state until termination.
	 *
	 * @param <S> the generic type
	 * @param instance the instance
	 * @param dependencies the dependencies
	 */
	public abstract <S extends ChaseState> void reasonUntilTermination(S instance, Collection<? extends Dependency> dependencies);
	
	/**
	 * Entails.
	 *
	 * @param <S> the generic type
	 * @param instance the instance
	 * @param free 		Mapping of query's free variables to constants
	 * @param target the target
	 * @param constraints the constraints
	 * @return 		true if the input instance with the given set of free variables and constraints implies the target query.
	 */
	public abstract <S extends ChaseState> boolean entails(S instance, Map<Variable, Constant> free, ConjunctiveQuery target, Collection<? extends Dependency<?,?>> constraints);
	
	
	/**
	 * Entails.
	 *
	 * @param <S> the generic type
	 * @param source the source
	 * @param target the target
	 * @param constraints the constraints
	 * @return 		true if the source query entails the target query
	 */
	public abstract boolean entails(ConjunctiveQuery source, ConjunctiveQuery target, Collection<? extends Dependency<?,?>> constraints, HomomorphismDetector detector);
	

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public abstract Chaser clone();
}
