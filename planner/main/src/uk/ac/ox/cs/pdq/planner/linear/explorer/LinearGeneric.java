package uk.ac.ox.cs.pdq.planner.linear.explorer;

import static uk.ac.ox.cs.pdq.planner.logging.performance.PlannerStatKeys.MILLI_CLOSE;
import static uk.ac.ox.cs.pdq.planner.logging.performance.PlannerStatKeys.MILLI_QUERY_MATCH;

import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.jgrapht.graph.DefaultEdge;

import com.google.common.eventbus.EventBus;

import uk.ac.ox.cs.pdq.algebra.RelationalTerm;
import uk.ac.ox.cs.pdq.cost.Cost;
import uk.ac.ox.cs.pdq.cost.estimators.CostEstimator;
import uk.ac.ox.cs.pdq.db.DatabaseConnection;
import uk.ac.ox.cs.pdq.db.Match;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.planner.PlannerException;
import uk.ac.ox.cs.pdq.planner.accessibleschema.AccessibleSchema;
import uk.ac.ox.cs.pdq.planner.linear.LinearConfiguration;
import uk.ac.ox.cs.pdq.planner.linear.explorer.node.NodeFactory;
import uk.ac.ox.cs.pdq.planner.linear.explorer.node.SearchNode;
import uk.ac.ox.cs.pdq.planner.linear.explorer.node.SearchNode.NodeStatus;
import uk.ac.ox.cs.pdq.planner.util.PlanTree;
import uk.ac.ox.cs.pdq.reasoning.chase.Chaser;
import uk.ac.ox.cs.pdq.util.LimitReachedException;


// TODO: Auto-generated Javadoc
/**
 * Exhaustively searches the proof space
 *
 * @author Efthymia Tsamoura
 */
public class LinearGeneric extends LinearExplorer {

	protected List<Entry<RelationalTerm, Cost>> exploredPlans = new ArrayList<>();
	
	/**
	 * Instantiates a new linear generic.
	 *
	 * @param eventBus the event bus
	 * @param collectStats the collect stats
	 * @param query 		The input user query
	 * @param accessibleQuery 		The accessible counterpart of the user query
	 * @param accessibleSchema 		The accessible counterpart of the input schema
	 * @param chaser 		Runs the chase algorithm
	 * @param detector 		Detects homomorphisms during chasing
	 * @param costEstimator 		Estimates the cost of a plan
	 * @param nodeFactory the node factory
	 * @param depth the depth
	 * @param reasoningParameters 
	 * @throws PlannerException the planner exception
	 * @throws SQLException 
	 */
	public LinearGeneric(
			EventBus eventBus, 
			boolean collectStats,
			ConjunctiveQuery query,
			ConjunctiveQuery accessibleQuery,
			AccessibleSchema accessibleSchema, 
			Chaser chaser,
			DatabaseConnection connection,
			CostEstimator costEstimator,
			NodeFactory nodeFactory,
			int depth) throws PlannerException, SQLException {
		super(eventBus, collectStats, query, accessibleQuery, accessibleSchema, chaser, connection, costEstimator, nodeFactory, depth);
	}

	/**
	 * _explore.
	 *
	 * @throws PlannerException the planner exception
	 * @throws LimitReachedException the limit reached exception
	 */
	@Override
	public void performSingleExplorationStep() throws PlannerException, LimitReachedException {
		SearchNode selectedNode;
		Candidate candidate;

		// Choose the next node to explore below it
		selectedNode = this.chooseNode();
		if (selectedNode == null) 
			return;
		
		LinearConfiguration selectedConfiguration = selectedNode.getConfiguration();

		/*
		 * Choose a new candidate fact. A candidate fact F(c1,c2,...,cN) is one for which
		 * (i) there exists Accessible(c_i) facts for any c_i
		 * (ii) AccessedF(c_1,c_2,...,c_N) does not exist in the current configuration
		 */
		candidate = selectedConfiguration.chooseCandidate();

		// Search for other candidate facts that could be exposed along with the selected candidate.
		Set<Candidate> similarCandidates = selectedConfiguration.getSimilarCandidates(candidate);
		selectedConfiguration.removeCandidates(similarCandidates);
		if (!selectedConfiguration.hasCandidates()) 
			selectedNode.setStatus(NodeStatus.TERMINAL);
		

		// Create a new node from the exposed facts and add it to the plan tree
		SearchNode freshNode = this.nodeFactory.getInstance(selectedNode, similarCandidates);
		freshNode.getConfiguration().detectCandidates(this.accessibleSchema);
		if (!freshNode.getConfiguration().hasCandidates()) 
			freshNode.setStatus(NodeStatus.TERMINAL);
	
		Cost cost = this.costEstimator.cost(freshNode.getConfiguration().getPlan());
		freshNode.getConfiguration().setCost(cost);
		
		this.stats.start(MILLI_CLOSE);
		freshNode.close(this.chaser, this.accessibleSchema.getInferredAccessibilityAxioms());
		this.stats.stop(MILLI_CLOSE);
		
		this.planTree.addVertex(freshNode);
		this.planTree.addEdge(selectedNode, freshNode, new DefaultEdge());

		// Check for query match
		this.stats.start(MILLI_QUERY_MATCH);
		List<Match> matches = freshNode.matchesQuery(this.accessibleQuery);
		this.stats.stop(MILLI_QUERY_MATCH);

		// If there exists at least one query match
		if (!matches.isEmpty()) {
			freshNode.setStatus(NodeStatus.SUCCESSFUL);
			this.exploredPlans.add(new AbstractMap.SimpleEntry<RelationalTerm, Cost>(freshNode.getConfiguration().getPlan(), freshNode.getConfiguration().getCost()));
			// Update the best plan found so far
			if (this.bestPlan == null || (this.bestPlan != null && freshNode.getConfiguration().getCost().lessThan(this.bestCost))) {
				this.bestPlan =  freshNode.getConfiguration().getPlan();
				this.bestCost = freshNode.getConfiguration().getCost();
			}
		}
	}
	
	public List<Entry<RelationalTerm, Cost>> getExploredPlans() {
		return this.exploredPlans;
	}
	
	public PlanTree<SearchNode> getPlanTree() {
		return this.planTree;
	}
	
}
