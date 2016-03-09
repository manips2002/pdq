package uk.ac.ox.cs.pdq.test.planner.accessible;

import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ox.cs.pdq.db.Attribute;
import uk.ac.ox.cs.pdq.db.Relation;
import uk.ac.ox.cs.pdq.db.TypedConstant;
import uk.ac.ox.cs.pdq.fol.Predicate;
import uk.ac.ox.cs.pdq.fol.Skolem;
import uk.ac.ox.cs.pdq.planner.accessibleschema.AccessibleSchema.AccessibleRelation;
import uk.ac.ox.cs.pdq.planner.accessibleschema.AccessibleSchema.InferredAccessibleRelation;
import uk.ac.ox.cs.pdq.util.Utility;

import com.google.common.collect.Lists;

// TODO: Auto-generated Javadoc
/**
 * The Class PredicateFormulaTest.
 */
public class PredicateFormulaTest {

	/** The random. */
	private Random random = new Random();
	
	/**
	 * Makes sure assertions are enabled.
	 */
	@Before 
	public void setup() {
		Utility.assertsEnabled();
	}
	
	/**
	 * Test is schema fact.
	 */
	@Test public void testIsSchemaFact() {
		Relation r = new Relation("r", Lists.newArrayList(
				new Attribute(String.class, "a1"), new Attribute(String.class, "a2"))) {};
				TypedConstant<String> c = new TypedConstant<>("c");
				Skolem s = new Skolem("s");
				Assert.assertFalse("PredicateFormula must not be of type InferredAccessible",
						new Predicate(r, Lists.newArrayList(s, c)).getSignature() instanceof InferredAccessibleRelation);
				Predicate p = new Predicate(r, Lists.newArrayList(s, c));
	}

	/**
	 * Test is accessible fact.
	 */
	@Test public void testIsAccessibleFact() {
		Relation r = AccessibleRelation.getInstance();
		Skolem s = new Skolem("s");
		Assert.assertTrue("PredicateFormula must be of type Accessible",
				new Predicate(r, Lists.newArrayList(s)).getSignature() instanceof AccessibleRelation);
	}

	/**
	 * Test is inferred accessible fact.
	 */
	@Test public void testIsInferredAccessibleFact() {
		Relation r = new InferredAccessibleRelation(
				new Relation("r", Lists.newArrayList(
						new Attribute(String.class, "a1"),
						new Attribute(String.class, "a2"))) {});
		TypedConstant<String> c = new TypedConstant<>("c");
		Skolem s = new Skolem("s");
		Assert.assertTrue("PredicateFormula must be of type InferredAccessible",
				new Predicate(r, Lists.newArrayList(s, c)).getSignature() instanceof InferredAccessibleRelation);
	}

}