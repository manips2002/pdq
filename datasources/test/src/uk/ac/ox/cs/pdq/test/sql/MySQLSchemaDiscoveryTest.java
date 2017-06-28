package uk.ac.ox.cs.pdq.test.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import uk.ac.ox.cs.pdq.builder.BuilderException;
import uk.ac.ox.cs.pdq.datasources.sql.MySQLSchemaDiscoverer;
import uk.ac.ox.cs.pdq.util.Utility;
import uk.ac.ox.cs.pdq.db.AccessMethod;
import uk.ac.ox.cs.pdq.db.AccessMethod.Types;
import uk.ac.ox.cs.pdq.db.Attribute;
import uk.ac.ox.cs.pdq.db.Schema;

import com.google.common.collect.Lists;

// TODO: Auto-generated Javadoc
/**
 * The Class MySQLSchemaDiscoveryTest.
 */
@Ignore public class MySQLSchemaDiscoveryTest {

	/** The schema. */
	private Schema schema = null;
	
	/**
	 * The Class Relation.
	 */
	private static class Relation {
		
		/** The name. */
		String name;
		
		/** The attributes. */
		List<String> attributes;
		
		/** The bindings. */
		List<AccessMethod> bindings;
	}
	
	/** The relations. */
	private Map<String, Relation> relations = new LinkedHashMap<>();
	
	/** The relation names. */
	private List<String> relationNames = Lists.newArrayList(
			new String[] {"customer", "lineitem", "nation", "orders",
					"part", "partsupp", "region", "supplier"}
	);
	
	/** The relation arities. */
	private List<Integer> relationArities = Lists.newArrayList(
			new Integer[] {8, 16, 4, 9, 9, 5, 3, 7}
	);
	
	/** The binding types. */
	private List<Types> bindingTypes = Lists.newArrayList(
			new Types[] {
					Types.FREE, Types.FREE, Types.FREE, 
					Types.FREE, Types.FREE, Types.FREE, 
					Types.FREE, Types.FREE
			}
	);
	
	/** The binding positions. */
	private List<Integer[]> bindingPositions = Lists.newArrayList(
			new Integer[][] {{}, {}, {}, {}, {}, {}, {}, {}}
	);
	
	/** The attributes names. */
	private List<String[]> attributesNames = Lists.newArrayList(
			new String[][] {
					{"c_custkey", "c_name", "c_address", "c_nationkey", 
						"c_phone", "c_acctbal", "c_mktsegment", "c_comment"},
					{"l_orderkey", "l_partkey", "l_suppkey", "l_linenumber",
							"l_quantity", "l_extendedprice", "l_discount",
							"l_tax", "l_returnflag", "l_linestatus", 
							"l_shipdate", "l_commitdate", "l_receiptdate", 
							"l_shipinstruct", "l_shipmode", "l_comment"},
					{"n_nationkey", "n_name", "n_regionkey", "n_comment"},
					{"o_orderkey", "o_custkey", "o_orderstatus", 
						"o_totalprice", "o_orderdate", "o_orderpriority",
						"o_clerk", "o_shippriority", "o_comment"},
					{"p_partkey", "p_name", "p_mfgr", "p_brand", "p_type",
						"p_size", "p_container", "p_retailprice", "p_comment"},
					{"ps_partkey", "ps_suppkey", "ps_availqty", "ps_supplycost",
							"ps_comment"},
					{"r_regionkey", "r_name", "r_comment"},
					{"s_suppkey", "s_name", "s_address", "s_nationkey", 
						"s_phone", "s_acctbal", "s_comment"}
			}
	);
	
	/**
	 * Instantiates a new my sql schema discovery test.
	 *
	 * @throws BuilderException the builder exception
	 */
	public MySQLSchemaDiscoveryTest() throws BuilderException {
		Properties properties = new Properties();
		properties.put("url", "jdbc:mysql://localhost/");
		properties.put("database", "tpch_1");
		properties.put("username", "pdq");
		properties.put("password", "pdq");
		int i = 0;
		for(String n: this.relationNames) {
			Relation r = new Relation();
			r.name = n;
			r.attributes = Lists.newArrayList(this.attributesNames.get(i));
			r.bindings = Lists.newArrayList(new AccessMethod(
					this.bindingTypes.get(i), 
					Lists.newArrayList(this.bindingPositions.get(i))));
			this.relations.put(n, r);
			i++;
		}
		MySQLSchemaDiscoverer disco = new MySQLSchemaDiscoverer();
		disco.setProperties(properties);
		this.schema = disco.discover();
	}
	
	/**
	 * Makes sure assertions are enabled.
	 */
	@Before 
	public void setup() {
		Utility.assertsEnabled();
	}
		
	/**
	 * Test number of relations.
	 */
	@Test
	public void testNumberOfRelations() {
		assertEquals(this.schema.getRelations().size(), this.relationNames.size());
	}
	
	/**
	 * Test relation names.
	 */
	@Test
	public void testRelationNames() {
		for (uk.ac.ox.cs.pdq.db.Relation r: this.schema.getRelations()) {
			assertTrue(r.getName() + " not present.", this.relations.containsKey(r.getName()));
		}
	}
	
	/**
	 * Test relation arities.
	 */
	@Test
	public void testRelationArities() {
		for (uk.ac.ox.cs.pdq.db.Relation r: this.schema.getRelations()) {
			assertEquals((Integer) r.getArity(), (Integer) this.relations.get(r.getName()).attributes.size());
		}
	}
	
	/**
	 * Test attribute names.
	 */
	@Test
	public void testAttributeNames() {
		for (uk.ac.ox.cs.pdq.db.Relation r: this.schema.getRelations()) {
			int j = 0;
			for (Attribute a: r.getAttributes()) {
				assertEquals(a.getName(),  this.relations.get(r.getName()).attributes.get(j++));
			}
		}
	}

	/**
	 * Test access method methods.
	 */
	@Test
	public void testAccessMethodMethods() {
		for (uk.ac.ox.cs.pdq.db.Relation r: this.schema.getRelations()) {
			int i = 0;
			for (AccessMethod b: r.getAccessMethods()) {
				assertEquals(b.getType(), this.relations.get(r.getName()).bindings.get(i).getType());
				assertEquals(b.getInputs(), this.relations.get(r.getName()).bindings.get(i++).getInputs());
			}
		}
	}

}
