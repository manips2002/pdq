// This file is part of PDQ (https://github.com/ProofDrivenQuerying/pdq) which is released under the MIT license.
// See accompanying LICENSE for copyright notice and full details.

package uk.ac.ox.cs.pdq.regression.cost.estimators;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ox.cs.pdq.algebra.RelationalTerm;
import uk.ac.ox.cs.pdq.cost.Cost;
import uk.ac.ox.cs.pdq.cost.estimators.CardinalityEstimator;
import uk.ac.ox.cs.pdq.cost.estimators.NaiveCardinalityEstimator;
import uk.ac.ox.cs.pdq.cost.estimators.TextBookCostEstimator;
import uk.ac.ox.cs.pdq.cost.statistics.Catalog;
import uk.ac.ox.cs.pdq.cost.statistics.SimpleCatalog;
import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.io.jaxb.IOManager;
import uk.ac.ox.cs.pdq.regression.utils.PlannerTestUtilities;

/**
 * Tests the WhiteBox cost estimator.
 *
 * @author Efthymia Tsamoura
 */
public class TextBookCostEstimatorTest{

	
	private static String SHEMA_PATH = "test/cost/";

	
	private static String PLAN_PATH = "test/cost/blackbox/";
	
	
	private static String CATALOG = "test/cost/erspi/catalog/catalog.properties";


	String schemata = "schema_bio.xml";

	
	String[] plans = {
			"plan_bio_1.xml",
			"plan_bio_2.xml",
			"plan_bio_3.xml",
			"plan_bio_4.xml",
			"plan_bio_5.xml",
			"plan_bio_6.xml",
			"plan_bio_7.xml",
			"plan_bio_8.xml",
			"plan_bio_9.xml",
			"plan_bio_10.xml",
			"plan_bio_11.xml",
			"plan_bio_12.xml",
			"plan_bio_13.xml",
			"plan_bio_14.xml",
			"plan_bio_15.xml"
	};

	
	boolean canonicalNames = true;

	
	String driver = null;

	
	String url = "jdbc:mysql://localhost/";

	String database = "pdq_chase";

	
	String username = "root";

	
	String password ="root";

	
	
	@Before
	public void prepare() {

	}

	
	@Test
	public void test() {
		for(int i = 0; i < this.plans.length; ++i) {
			String s = this.schemata;
			String f = this.plans[i];
			try {
				Schema schema = IOManager.importSchema(new File(SHEMA_PATH + s));
				if (schema == null) 
					throw new IllegalStateException("Schema must be provided.");
				
				Entry<RelationalTerm, Cost> plan = PlannerTestUtilities.obtainPlan(PLAN_PATH + f, schema);
				Catalog catalog = new SimpleCatalog(schema, CATALOG);
				CardinalityEstimator card = new NaiveCardinalityEstimator(catalog);
				TextBookCostEstimator costEstimator = null;
				costEstimator = new TextBookCostEstimator(card);
				Assert.assertEquals(plan.getValue(), costEstimator.cost(plan.getKey()));
			} catch (FileNotFoundException e) {
				System.out.println("Cannot find input files");
			} catch (Exception e) {
				System.out.println("EXCEPTION: " + e.getClass().getSimpleName() + " " + e.getMessage());
				e.printStackTrace();
			} catch (Error e) {
				System.out.println("ERROR: " + e.getClass().getSimpleName() + " " + e.getMessage());
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

}
