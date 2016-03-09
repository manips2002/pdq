package uk.ac.ox.cs.pdq.generator.tgdsfromquery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import uk.ac.ox.cs.pdq.benchmark.BenchmarkParameters;
import uk.ac.ox.cs.pdq.db.Relation;
import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.fol.AcyclicQuery;
import uk.ac.ox.cs.pdq.fol.Conjunction;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Signature;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;
import uk.ac.ox.cs.pdq.generator.QueryGenerator;
import uk.ac.ox.cs.pdq.util.Utility;

import com.google.common.collect.Lists;

// TODO: Auto-generated Javadoc
/**
 * Creates guarded (queries having a guard in their body), chain guarded (a chain query with a guard) 
 * or acyclic queries given the relations of an input schema.  
 * 
 * @author Efthymia Tsamoura
 * 
 */
public class QueryGeneratorFirst extends AbstractDependencyGenerator implements QueryGenerator{
	
	/**
	 * Instantiates a new query generator first.
	 *
	 * @param schema the schema
	 * @param params the params
	 */
	public QueryGeneratorFirst(Schema schema, BenchmarkParameters params) {
		super(schema, params);
	}

	/* (non-Javadoc)
	 * @see uk.ac.ox.cs.pdq.generator.QueryGenerator#generate()
	 */
	@Override
	public ConjunctiveQuery generate() {
		switch (this.params.getQueryType()) {
		case GUARDED:
			return this.generateGuardedQuery();
		case CHAINGUARDED:
			return this.generateChainGuardedQuery();
		case ACYCLIC:
			return this.generateAcyclicQuery();
		default:
			throw new IllegalArgumentException("Unknown query type " + this.params.getQueryType());
		}
	}

	/**
	 * 
	 * Creates acyclic queries having a given number of conjuncts.
	 * The algorithms starts by randomly selecting the predicates that will appear in the query's body, 
	 * along with their variables. Different predicates are associated with different variables.
	 * The user can choose whether a query will have repeated predicates or not.
	 * Then the algorithm continues by populating the body of the query with atoms.
	 * Two atoms A_i and A_i+1 join have only one join variable. 
	 * 
	 * @return
	 * 		an acyclic query of the target number of conjunctions 
	 */
	private AcyclicQuery generateAcyclicQuery() {
		SortedSet<Predicate> tmpBody = new TreeSet<>(new Comparator<Predicate>() {
			@Override public int compare(Predicate o1, Predicate o2) {
				return o1 != null
						? (o2 != null ?
								o1.getSignature().getArity() - o2.getSignature().getArity() : 1)
						: (o2 != null ? -1 : 0);
			}
		});
		
		//Load the schema relations
		List<Relation> relations = new ArrayList<>(this.schema.getRelations());

		// Creating the conjunctions in the query's body
		while (tmpBody.size() < this.params.getQueryConjuncts()
				&& !relations.isEmpty()) {
			int choice = this.random.nextInt(relations.size());
			Relation selectedRelation = relations.get(choice);
			if (!this.params.getRepeatedRelations()) {
				relations.remove(choice);
			}
			tmpBody.add(new Predicate(selectedRelation, Utility.generateVariables(selectedRelation)));
		}

		// Swapping variable ensuring no cycles are created
		List<Predicate> body = new ArrayList<>(tmpBody.size());
		Set<Term> usedVars = new LinkedHashSet<>();
		PriorityQueue<Predicate> unusedPreds = new PriorityQueue<>(tmpBody);
		Predicate pick1 = unusedPreds.poll();
		do {
			List<Term> terms = Lists.newArrayList(pick1.getTerms());
			if (!unusedPreds.isEmpty()) {
				Predicate pick2 = unusedPreds.peek();
				List<Term> terms1 = Lists.newArrayList(pick1.getTerms());
				List<Term> terms2 = Lists.newArrayList(pick2.getTerms());
				terms1.removeAll(usedVars);
				if (!terms1.isEmpty()) {
					int pos = terms.indexOf(terms1.get(this.random.nextInt(terms1.size())));
					Term v2 = terms2.get(this.random.nextInt(terms2.size()));
					terms.set(pos, v2);
					usedVars.add(v2);
					unusedPreds.remove(pick2);
				}
				body.add(new Predicate(pick1.getSignature(), terms));
				pick1 = pick2;
			} else {
				body.add(new Predicate(pick1.getSignature(), terms));
			}
		} while(!unusedPreds.isEmpty());

		List<Variable> free = this.pickFreeVariables(body);
		Signature relationQ = new Signature("Q", free.size());
		Predicate atom = new Predicate(relationQ, free);
		return new AcyclicQuery(atom, Conjunction.of(body));
	}

	/**
	 * Creates conjunctive guarded queries having a given number of conjuncts.
	 * The algorithm starts by creating a list of variables V.
	 * These variables are passed to a method for creating the body of the query.
	 * The body is then created as follows: 
	 * the algorithm picks a random sets of relations.
	 * These relations form atoms with variables randomly selected variables from V. 
	 * The relation of the maximum arity forms the guard which is populated with all 
	 * variables from V. 
	 * 
	 * @return
	 * 		a guarded query having the target number of conjunctions 
	 */
	private ConjunctiveQuery generateGuardedQuery() {
		List<Variable> variables = this.createVariables(this.schema.getMaxArity());
		ConjunctionInfo ret = this.createGuardedConjunction(
				variables,
				this.params.getQueryConjuncts(),
				this.params.getRepeatedRelations());
		List<Predicate> queryBodyAtoms = Lists.newArrayList(ret.getConjuncts());
		List<Variable> free = this.pickFreeVariables(queryBodyAtoms);
		Signature relationQ = new Signature("Q", free.size());
		Predicate atom = new Predicate(relationQ, free);
		return new ConjunctiveQuery(atom, Conjunction.of(queryBodyAtoms));
	}

	
	/**
	 * TODO This method seems to return a chain query not a chain guarded query.
	 * Creates chain guarded queries having a given number of conjuncts.
	 * The algorithm starts by creating a list of variables V.
	 * These variables are passed to a method for creating the body of the query.
	 * The body is then created as follows: 
	 * the algorithm picks a random sets of relations.
	 * These relations form atoms with variables randomly selected variables from V. 
	 * Two atoms A_i and A_i+1 join have only one join variable. 
	 * 
	 * @return
	 * 		a chain guarded query of the target number of conjunctions 
	 */
	public ConjunctiveQuery generateChainGuardedQuery() {
		List<Variable> variables = this.createVariables(this.params.getArity());
		ConjunctionInfo ret = this.createGuardedConjunction(
				variables, 
				this.params.getQueryConjuncts(),
				this.params.getRepeatedRelations());
		List<Predicate> queryBodyAtoms = this.createChainConjuncts(ret.getConjuncts());
		List<Variable> free = this.pickFreeVariables(queryBodyAtoms);
		Signature relationQ = new Signature("Q", free.size());
		Predicate atom = new Predicate(relationQ, free);
		return new ConjunctiveQuery(atom, Conjunction.of(queryBodyAtoms));
	}
}