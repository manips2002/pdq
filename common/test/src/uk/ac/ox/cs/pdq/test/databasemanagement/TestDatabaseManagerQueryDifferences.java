package uk.ac.ox.cs.pdq.test.databasemanagement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ox.cs.pdq.databasemanagement.DatabaseManager;
import uk.ac.ox.cs.pdq.databasemanagement.exception.DatabaseException;
import uk.ac.ox.cs.pdq.db.DatabaseParameters;
import uk.ac.ox.cs.pdq.db.Match;
import uk.ac.ox.cs.pdq.db.Relation;
import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.db.TypedConstant;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Conjunction;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.UntypedConstant;
import uk.ac.ox.cs.pdq.fol.Variable;
import uk.ac.ox.cs.pdq.test.util.PdqTest;

/**
 * tests the creation and basic usages of a database.
 * 
 * @author Gabor
 *
 */
public class TestDatabaseManagerQueryDifferences extends PdqTest {

	 @Test
	 public void largeTableQueryDifferenceDerby() throws DatabaseException {
		 largeTableQueryDifferenceTGD(DatabaseParameters.Derby);
		 largeTableQueryDifferenceEGD(DatabaseParameters.Derby);
	 }
	 @Test
	 public void largeTableQueryDifferenceMySql() throws DatabaseException {
		 largeTableQueryDifferenceTGD(DatabaseParameters.MySql);
		 largeTableQueryDifferenceEGD(DatabaseParameters.MySql);
	 }
	 @Test
	 public void largeTableQueryDifferencePostgres() throws DatabaseException {
		 largeTableQueryDifferenceTGD(DatabaseParameters.Postgres);
		 largeTableQueryDifferenceEGD(DatabaseParameters.Postgres);
	 }

	//@Test
	public void largeTableQueryDifferenceMemory() throws DatabaseException {
		largeTableQueryDifferenceTGD(DatabaseParameters.Memory);
		largeTableQueryDifferenceEGD(DatabaseParameters.Memory);
	}

	private void largeTableQueryDifferenceTGD(DatabaseParameters parameters) throws DatabaseException {
		DatabaseManager manager = new DatabaseManager(parameters);
		manager.initialiseDatabaseForSchema(new Schema(new Relation[] { R, S, T }));
		List<Atom> facts = new ArrayList<>();
		
		// add some disjoint test data
		for (int i = 0; i < 100; i++) {
			Atom a1 = Atom.create(this.R, new Term[] { TypedConstant.create(10000 + i), TypedConstant.create(20000 + i), TypedConstant.create(30000 + i) });
			facts.add(a1);
			if (i < 90) {
				Atom c1 = Atom.create(this.T, new Term[] { TypedConstant.create(30000 + i), TypedConstant.create(20000 + i), TypedConstant.create(90000 + i) });
				facts.add(c1);
			}
		}
		
		// add test record that represents a not active dependency
		Atom a1 = Atom.create(this.R, new Term[] { TypedConstant.create(13), TypedConstant.create(14), TypedConstant.create(15) });
		Atom b1 = Atom.create(this.S, new Term[] { TypedConstant.create(13), TypedConstant.create(14) });
		Atom c1 = Atom.create(this.T, new Term[] { TypedConstant.create(15), TypedConstant.create(16), TypedConstant.create(17) });
		facts.add(a1);
		facts.add(b1);
		facts.add(c1);
		// add test record that represents an active dependency
		Atom a2 = Atom.create(this.R, new Term[] { TypedConstant.create(113), TypedConstant.create(114), TypedConstant.create(115) });
		Atom b2 = Atom.create(this.S, new Term[] { TypedConstant.create(113), TypedConstant.create(114) });
		facts.add(a2);
		facts.add(b2);
		manager.addFacts(facts);
		

		// form queries
		Atom q1 = Atom.create(this.R, new Term[] { Variable.create("x"), Variable.create("y"), Variable.create("z") });
		Atom q2 = Atom.create(this.S, new Term[] { Variable.create("x"), Variable.create("y")});
		Atom q3 = Atom.create(this.T, new Term[] { Variable.create("z"), Variable.create("res1"), Variable.create("res2") });
		ConjunctiveQuery left = ConjunctiveQuery.create(new Variable[] { z}, Conjunction.create(q1, q2));

		ConjunctiveQuery right = ConjunctiveQuery.create(new Variable[] { Variable.create("res1"), Variable.create("res2")  }, (Conjunction)Conjunction.of(q1, q2, q3));
		// check left and right queries
		
		List<Match> leftFacts = manager.answerQueries(Arrays.asList(new ConjunctiveQuery[] { left }));
		Assert.assertEquals(2, leftFacts.size());
		List<Match> rightFacts = manager.answerQueries(Arrays.asList(new ConjunctiveQuery[] { right }));
		Assert.assertEquals(1, rightFacts.size());
	//	Assert.assertNull(rightFacts.get(0).getMapping().get(Variable.create("z")));

		List<Match> diffFacts = manager.answerQueryDifferences(left, right);
		Assert.assertEquals(1, diffFacts.size());
		Assert.assertTrue(diffFacts.get(0).getMapping().containsKey(Variable.create("z")));
		if (parameters.getDatabaseDriver().contains("memory")) {
			Assert.assertEquals(TypedConstant.create(115),diffFacts.get(0).getMapping().get(Variable.create("z")));
		} else {
			Assert.assertEquals(UntypedConstant.create("115"),diffFacts.get(0).getMapping().get(Variable.create("z")));
		}
	}
	
	private void largeTableQueryDifferenceEGD(DatabaseParameters parameters) throws DatabaseException {
		DatabaseManager manager = new DatabaseManager(parameters);
		manager.initialiseDatabaseForSchema(new Schema(new Relation[] { R, S, T }));
		List<Atom> facts = new ArrayList<>();
		
		// add some disjoint test data
		for (int i = 0; i < 100; i++) {
			Atom a1 = Atom.create(this.R, new Term[] { TypedConstant.create(10000 + i), TypedConstant.create(20000 + i), TypedConstant.create(30000 + i) });
			if (i < 90) {
				Atom c1 = Atom.create(this.T, new Term[] { TypedConstant.create(30000 + i), TypedConstant.create(20000 + i), TypedConstant.create(90000 + i) });
				facts.add(c1);
			}
			facts.add(a1);
		}
		
		// add test record that represents a not active dependency
		Atom a1 = Atom.create(this.R, new Term[] { TypedConstant.create(13), TypedConstant.create(14), TypedConstant.create(15) });
		Atom b1 = Atom.create(this.S, new Term[] { TypedConstant.create(13), TypedConstant.create(14) });
		Atom c1 = Atom.create(this.T, new Term[] { TypedConstant.create(16), TypedConstant.create(17), TypedConstant.create(15) });
		facts.add(a1);
		facts.add(b1);
		facts.add(c1);
		// add test record that represents an active dependency
		Atom a2 = Atom.create(this.R, new Term[] { TypedConstant.create(113), TypedConstant.create(114), TypedConstant.create(115) });
		Atom b2 = Atom.create(this.S, new Term[] { TypedConstant.create(113), TypedConstant.create(114) });
		Atom c2 = Atom.create(this.T, new Term[] { TypedConstant.create(116), TypedConstant.create(117), TypedConstant.create(215) });
		facts.add(a2);
		facts.add(b2);
		facts.add(c2);
		manager.addFacts(facts);
		
		// form queries
		Atom q1 = Atom.create(this.R, new Term[] { Variable.create("x"), Variable.create("y"), Variable.create("z") });
		Atom q2 = Atom.create(this.S, new Term[] { Variable.create("x"), Variable.create("y")});
		Atom q3 = Atom.create(this.T, new Term[] { Variable.create("res1"), Variable.create("res2"), Variable.create("z") });
		ConjunctiveQuery left = ConjunctiveQuery.create(new Variable[] { z}, Conjunction.create(q1, q2));

		ConjunctiveQuery right = ConjunctiveQuery.create(new Variable[] { Variable.create("res1") }, (Conjunction)Conjunction.of(q1, q2, q3));
		// check left and right queries
		List<Match> leftFacts = manager.answerQueries(Arrays.asList(new ConjunctiveQuery[] { left }));
		Assert.assertEquals(2, leftFacts.size());
		List<Match> rightFacts = manager.answerQueries(Arrays.asList(new ConjunctiveQuery[] { right }));
		Assert.assertEquals(1, rightFacts.size());
		Assert.assertNull(rightFacts.get(0).getMapping().get(Variable.create("z")));

		List<Match> diffFacts = manager.answerQueryDifferences(left, right);
		System.out.println(diffFacts);
		Assert.assertEquals(1, diffFacts.size());
		Assert.assertTrue(diffFacts.get(0).getMapping().containsKey(Variable.create("z")));
		
		if (parameters.getDatabaseDriver().contains("memory")) {
			Assert.assertEquals(TypedConstant.create(115),diffFacts.get(0).getMapping().get(Variable.create("z")));
		} else {
			Assert.assertEquals(UntypedConstant.create("115"),diffFacts.get(0).getMapping().get(Variable.create("z")));
		}
	}

}
