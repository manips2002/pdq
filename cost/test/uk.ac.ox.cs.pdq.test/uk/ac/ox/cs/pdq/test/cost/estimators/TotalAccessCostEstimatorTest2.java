package uk.ac.ox.cs.pdq.test.cost.estimators;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import uk.ac.ox.cs.pdq.algebra.predicates.ConjunctivePredicate;
import uk.ac.ox.cs.pdq.algebra.predicates.ConstantEqualityPredicate;
import uk.ac.ox.cs.pdq.cost.statistics.Catalog;
import uk.ac.ox.cs.pdq.cost.statistics.SimpleCatalog;
import uk.ac.ox.cs.pdq.cost.statistics.estimators.ConstraintCardinalityEstimator;
import uk.ac.ox.cs.pdq.db.Attribute;
import uk.ac.ox.cs.pdq.db.Constraint;
import uk.ac.ox.cs.pdq.db.EGD;
import uk.ac.ox.cs.pdq.db.Relation;
import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.db.TGD;
import uk.ac.ox.cs.pdq.db.TypedConstant;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.fol.Query;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;
import uk.ac.ox.cs.pdq.io.xml.QueryReader;
import uk.ac.ox.cs.pdq.io.xml.SchemaReader;
import uk.ac.ox.cs.pdq.plan.Access;
import uk.ac.ox.cs.pdq.plan.Command;
import uk.ac.ox.cs.pdq.plan.CommandToTGDTranslator;
import uk.ac.ox.cs.pdq.plan.Join;
import uk.ac.ox.cs.pdq.plan.NormalisedPlan;
import uk.ac.ox.cs.pdq.plan.Project;
import uk.ac.ox.cs.pdq.plan.Select;
import uk.ac.ox.cs.pdq.reasoning.HomomorphismException;
import uk.ac.ox.cs.pdq.reasoning.ReasoningParameters.HomomorphismDetectorTypes;
import uk.ac.ox.cs.pdq.reasoning.homomorphism.DBHomomorphismManager;
import uk.ac.ox.cs.pdq.reasoning.homomorphism.HomomorphismDetector;
import uk.ac.ox.cs.pdq.reasoning.homomorphism.HomomorphismManagerFactory;

import com.beust.jcommander.internal.Sets;
import com.google.common.collect.Lists;

public class TotalAccessCostEstimatorTest2 {

	
	String driver = null;
	String url = "jdbc:mysql://localhost/";
	String database = "pdq_chase";
	String username = "root";
	String password ="root";
	
	@Test
	public void callTestPlan2() throws FileNotFoundException, IOException {
		String PATH = "C:/Users/tsamoura/workspace2/dev4.benchmark/local/bio/queries/schema2/DAG/CONSTRAINT_CARDINALITY";
		String schemaPath = "/case_008b/schema.xml";
		String queryPath = "/case_008b/query.xml";
		String catalogPath = "C:/Users/tsamoura/workspace2/dev4.benchmark/catalog5/catalog.properties";

		try(FileInputStream sis = new FileInputStream(PATH + schemaPath);
				FileInputStream qis = new FileInputStream(PATH + queryPath)) {

			//Load query and schema
			Schema schema = new SchemaReader().read(sis);
			ConjunctiveQuery query = new QueryReader(schema).read(qis);

			if (schema == null || query == null) {
				throw new IllegalStateException("Schema and query must be provided.");
			}
			schema.updateConstants(query.getSchemaConstants());
			
			//Define keys 
			Relation activityFree = schema.getRelation("ActivityFree");
			Attribute activity_id = activityFree.getAttribute(1);
			activityFree.setKey(Lists.newArrayList(activity_id));
			Relation activityLimited = schema.getRelation("ActivityLimited");
			activity_id = activityLimited.getAttribute(1);
			activityLimited.setKey(Lists.newArrayList(activity_id));
			
			Relation AssayFree = schema.getRelation("AssayFree");
			Attribute assay_chembl_id = AssayFree.getAttribute(2);
			AssayFree.setKey(Lists.newArrayList(assay_chembl_id));
			Relation AssayLimited = schema.getRelation("AssayLimited");
			assay_chembl_id = AssayLimited.getAttribute(2);
			AssayLimited.setKey(Lists.newArrayList(assay_chembl_id));
			
//			Relation CellLineFree = schema.getRelation("CellLineFree");
//			Attribute cell_chembl_id = CellLineFree.getAttribute(0);
//			CellLineFree.setKey(Lists.newArrayList(cell_chembl_id));
//			Relation CellLineLimited = schema.getRelation("CellLineLimited");
//			cell_chembl_id = CellLineLimited.getAttribute(0);
//			CellLineLimited.setKey(Lists.newArrayList(cell_chembl_id));
			
			Relation DocumentFree = schema.getRelation("DocumentFree");
			Attribute document_chembl_id = DocumentFree.getAttribute(2);
			DocumentFree.setKey(Lists.newArrayList(document_chembl_id));
			Relation DocumentLimited = schema.getRelation("DocumentLimited");
			document_chembl_id = DocumentLimited.getAttribute(2);
			DocumentLimited.setKey(Lists.newArrayList(document_chembl_id));
			
			Relation MoleculeFree = schema.getRelation("MoleculeFree");
			Attribute molecule_chembl_id = MoleculeFree.getAttribute(14);
			MoleculeFree.setKey(Lists.newArrayList(molecule_chembl_id));
			Relation MoleculeLimited = schema.getRelation("MoleculeLimited");
			molecule_chembl_id = MoleculeLimited.getAttribute(14);
			MoleculeLimited.setKey(Lists.newArrayList(molecule_chembl_id));
			
			Relation TargetFree = schema.getRelation("TargetFree");
			Attribute target_chembl_id = TargetFree.getAttribute(3);
			TargetFree.setKey(Lists.newArrayList(target_chembl_id));
			Relation TargetLimited = schema.getRelation("TargetLimited");
			target_chembl_id = TargetLimited.getAttribute(3);
			TargetLimited.setKey(Lists.newArrayList(target_chembl_id));
			
			Relation TargetComponentFree = schema.getRelation("TargetComponentFree");
			Attribute component_id = TargetComponentFree.getAttribute(1);
			TargetComponentFree.setKey(Lists.newArrayList(component_id));
			Relation TargetComponentLimited = schema.getRelation("TargetComponentLimited");
			component_id = TargetComponentLimited.getAttribute(1);
			TargetComponentLimited.setKey(Lists.newArrayList(component_id));
			
			
			Relation PublicationFull = schema.getRelation("PublicationFull");
			Attribute id = PublicationFull.getAttribute(0);
			PublicationFull.setKey(Lists.newArrayList(id));
//			Relation PublicationShort = schema.getRelation("PublicationShort");
//			id = PublicationShort.getAttribute(0);
//			PublicationShort.setKey(Lists.newArrayList(id));
			
			Relation Citation = schema.getRelation("Citation");
			id = Citation.getAttribute(0);
			Citation.setKey(Lists.newArrayList(id));
			
			Relation Reference = schema.getRelation("Reference");
			id = Reference.getAttribute(0);
			Reference.setKey(Lists.newArrayList(id));
			
			Relation PathwayBySpecies = schema.getRelation("PathwayBySpecies");
			Attribute pathwayId = PathwayBySpecies.getAttribute(0);
			PathwayBySpecies.setKey(Lists.newArrayList(pathwayId));
			
			Relation PathwayById = schema.getRelation("PathwayById");
			pathwayId = PathwayById.getAttribute(0);
			PathwayById.setKey(Lists.newArrayList(pathwayId));
			
			Relation OrganismById = schema.getRelation("OrganismById");
			Attribute organismId = OrganismById.getAttribute(0);
			OrganismById.setKey(Lists.newArrayList(organismId));
			
			Relation OrganismFree = schema.getRelation("OrganismFree");
			organismId = OrganismFree.getAttribute(0);
			OrganismFree.setKey(Lists.newArrayList(organismId));
			
			Relation ProteinLimited = schema.getRelation("ProteinLimited");
			id = ProteinLimited.getAttribute(1);
			ProteinLimited.setKey(Lists.newArrayList(id));
			
			Relation ProteinFree = schema.getRelation("ProteinFree");
			id = ProteinFree.getAttribute(0);
			ProteinFree.setKey(Lists.newArrayList(id));
			
			//Load the catalog
			Catalog catalog = new SimpleCatalog(schema, catalogPath);
			
			this.testPlan2(schema, query, catalog);
		}

	}
	
	
	//Creates the plan for the configuration 
	//PCOMPOSE(PCOMPOSE(JCOMPOSE(JCOMPOSE(APPLYRULE(TargetComponentFree(){TargetComponentFree(c446,c447,PROTEIN,k488,c443,k489,k490,k491)}),
	//APPLYRULE(ProteinLimited(4){ProteinLimited(k492,c446,k493,c443)})),
	//APPLYRULE(TargetLimited(1){TargetLimited(c443,c444,c445,c433,c446,c447,c448,SINGLE PROTEIN)})),
	//APPLYRULE(AssayLimited(22){AssayLimited(c412,c413,c414,c415,c416,c417,c418,c419,c420,c421,c422,c423,c424,c425,c426,c427,c428,c429,c430,c431,c432,c433)})),
	//APPLYRULE(DocumentLimited(3){DocumentLimited(c434,PUBLICATION,c428,c435,c436,c437,c438,c439,c440,c441,c442,2015)}))
	private void testPlan2(Schema schema, Query<?> query, Catalog catalog) {
		//Define all schema and chase constants
		Term _accession = new Variable("accession");
		Term _component_id = new Variable("component_id");
		Term protein = new Variable("cprotein");
		Term _description0 = new Variable("_description0");
		Term _organism = new Variable("organism");
		Term _protein_classification_id = new Variable("protein_classification_id");
		Term _sequence = new Variable("sequence");
		Term _tax_id = new Variable("tax_id");
		Term _input_id = new Variable("input_id");
		Term _entry_name = new Variable("entry_name");
		Term _pref_name = new Variable("pref_name");
		Term _species_group_flag = new Variable("species_group_flag");
		Term _target_chembl_id = new Variable("target_chembl_id");
		Term _target_component_type = new Variable("target_component_type");
		Term singleprotein = new Variable("csingleprotein");
		Term _assay_category = new Variable("assay_category");
		Term _assay_cell_type = new Variable("assay_cell_type");
		Term _assay_chembl_id = new Variable("assay_chembl_id");
		Term _assay_organism = new Variable("assay_organism");
		Term _assay_strain = new Variable("assay_strain");
		Term _assay_subcellular_fraction = new Variable("assay_subcellular_fraction");
		Term _assay_tax_id = new Variable("assay_tax_id");
		Term _assay_test_type = new Variable("assay_test_type");
		Term _assay_tissue = new Variable("assay_tissue");
		Term _assay_type = new Variable("assay_type");
		Term _assay_type_description = new Variable("assay_type_description");
		Term _bao_format = new Variable("bao_format");
		Term _cell_chembl_id = new Variable("cell_chembl_id");
		Term _confidence_description = new Variable("confidence_description");
		Term _confidence_score = new Variable("confidence_score");
		Term _description = new Variable("description");
		Term _document_chembl_id = new Variable("document_chembl_id");
		Term _relationship_description = new Variable("relationship_description");
		Term _relationship_type = new Variable("relationship_type");
		Term _src_assay_id = new Variable("src_assay_id");
		Term _src_id = new Variable("src_id");
		
		Term _authors = new Variable("authors");
		Term pub = new Variable("cpub");
		Term _doi = new Variable("doi");
		Term _first_page = new Variable("first_page");
		Term _issue = new Variable("issue");
		Term _journal = new Variable("journal");
		Term _last_page = new Variable("last_page");
		Term _pubmed_id = new Variable("pubmed_id");
		Term _title = new Variable("title");
		Term _volume = new Variable("volume");
		Term year = new Variable("2015");
		
		
		//Define the plan
		Command access0 = new Access(schema.getRelation("TargetComponentFree"), schema.getRelation("TargetComponentFree").getAccessMethod("chembl_target_component_free"), 
				Lists.newArrayList(_accession,_component_id,protein,_description0,_organism,_protein_classification_id,_sequence,_tax_id), null, null);
		ConstantEqualityPredicate p00 = new ConstantEqualityPredicate(2, new TypedConstant<String>("PROTEIN"));
		Command selection0 = new Select(new ConjunctivePredicate(Lists.newArrayList(p00)), access0.getOutput());
		Attribute attr = (Attribute) selection0.getOutput().getHeader().get(3);
		Command projection0 = new Project(Lists.newArrayList(attr), selection0.getOutput());
				
		Command access1 = new Access(schema.getRelation("ProteinLimited"), schema.getRelation("ProteinLimited").getAccessMethod("uniprot_protein_2"), 
				Lists.newArrayList(_input_id,_accession,_entry_name,_organism), projection0.getOutput(), null);
		Command join1 = new Join(access1.getOutput(), selection0.getOutput());
		Attribute attr1 = (Attribute) join1.getOutput().getHeader().get(3);
		Command projection1 = new Project(Lists.newArrayList(attr1), join1.getOutput());
		
		Command access2 = new Access(schema.getRelation("TargetLimited"), schema.getRelation("TargetLimited").getAccessMethod("chembl_target_limited_1"), 
				Lists.newArrayList(_organism,_pref_name, _species_group_flag,_target_chembl_id,_accession,_component_id,_target_component_type,singleprotein), projection1.getOutput(), null);
		ConstantEqualityPredicate p20 = new ConstantEqualityPredicate(7, new TypedConstant<String>("SINGLE PROTEIN"));
		Command selection2 = new Select(new ConjunctivePredicate(Lists.newArrayList(p20)), access2.getOutput());
		Command join2 = new Join(selection2.getOutput(), join1.getOutput());
		Attribute attr2 = (Attribute) join2.getOutput().getHeader().get(3);
		Command projection2 = new Project(Lists.newArrayList(attr2), join2.getOutput());
		
		Command access3 = new Access(schema.getRelation("AssayLimited"), schema.getRelation("AssayLimited").getAccessMethod("chembl_assay_limited_3"), 
				Lists.newArrayList(_assay_category,_assay_cell_type,_assay_chembl_id,_assay_organism,_assay_strain,_assay_subcellular_fraction,_assay_tax_id,_assay_test_type,_assay_tissue,_assay_type,_assay_type_description,_bao_format,_cell_chembl_id,_confidence_description,_confidence_score,_description,_document_chembl_id,_relationship_description,_relationship_type,_src_assay_id,_src_id,_target_chembl_id), 
				projection2.getOutput(), null);
		Command join3 = new Join(access3.getOutput(), join2.getOutput());
		Attribute attr3 = (Attribute) join3.getOutput().getHeader().get(16);
		Command projection3 = new Project(Lists.newArrayList(attr3), join3.getOutput());
		
		Command access4 = new Access(schema.getRelation("DocumentLimited"), schema.getRelation("DocumentLimited").getAccessMethod("chembl_document_limited"), 
				Lists.newArrayList(_authors,pub,_document_chembl_id,_doi,_first_page,_issue,_journal,_last_page,_pubmed_id,_title,_volume,year), projection3.getOutput(), null);
		ConstantEqualityPredicate p40 = new ConstantEqualityPredicate(1, new TypedConstant<String>("PUBLICATION"));
		Command selection4 = new Select(new ConjunctivePredicate(Lists.newArrayList(p40)), access4.getOutput());
		Command join4 = new Join(selection4.getOutput(), join3.getOutput());
		
		
		NormalisedPlan plan = new NormalisedPlan(Lists.newArrayList(access0, selection0, projection0, access1, join1, projection1, access2, selection2, join2,
				projection2, access3, join3, projection3, access4, selection4, join4));
		
		
		HomomorphismDetector detector = null;
		try {
			detector = new HomomorphismManagerFactory().getInstance(
					schema, 
					query, 
					HomomorphismDetectorTypes.DATABASE,
					this.driver,
					this.url,
					this.database,
					this.username,			
					this.password);
			((DBHomomorphismManager) detector).consolidateBaseTables(plan.getTables());
		} catch (HomomorphismException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ConstraintCardinalityEstimator estimator = new ConstraintCardinalityEstimator(schema, (DBHomomorphismManager) detector);
		
		//Get the tgds from the input commands;
		Collection<TGD> forwardTgds = new CommandToTGDTranslator().toTGD(plan);
		Collection<TGD> backwardTgds = Sets.newLinkedHashSet();
		for(TGD tgd:forwardTgds) {
			backwardTgds.add(tgd.invert());
		}
		Collection<Constraint> constraints = CollectionUtils.union(CollectionUtils.union(schema.getDependencies(), forwardTgds), backwardTgds);
		
		Collection<EGD> keyDependencies = Lists.newArrayList();
		for(Relation relation:schema.getRelations()) {
			if(!relation.getKey().isEmpty()) {
				keyDependencies.add(EGD.getEGDs(relation, relation.getKey()));
			}
		}
		
//		/*
//		//Test keys and inclusion dependencies between the tables
//		boolean r1 = estimator.existsInclustionDependency(access1.getOutput(), selection0.getOutput(), constraints);
//		boolean r2 = estimator.existsInclustionDependency(selection0.getOutput(), access1.getOutput(), constraints);
//		*/
//		
//		Attribute k1 = (Attribute) access1.getOutput().getHeader().get(1);
//		boolean r3 = estimator.isKey(access1.getOutput(), Lists.newArrayList(k1), CollectionUtils.union(keyDependencies, constraints));
//		boolean r4 = estimator.isKey(selection0.getOutput(), Lists.newArrayList(k1), CollectionUtils.union(keyDependencies, constraints));
//		
//		/*
//		boolean r5 = estimator.existsInclustionDependency(selection2.getOutput(), join1.getOutput(), constraints);
//		boolean r6 = estimator.existsInclustionDependency(join1.getOutput(), selection2.getOutput(), constraints);
//		*/
//		
//		Attribute k2 = (Attribute) selection2.getOutput().getHeader().get(0);
//		boolean r7 = estimator.isKey(selection2.getOutput(), Lists.newArrayList(k2), CollectionUtils.union(keyDependencies, constraints));
//		boolean r8 = estimator.isKey(join1.getOutput(), Lists.newArrayList(k2), CollectionUtils.union(keyDependencies, constraints));
//		
//		/*
//		boolean r9 = estimator.existsInclustionDependency(access3.getOutput(), join2.getOutput(), constraints);
//		boolean r10 = estimator.existsInclustionDependency(join2.getOutput(), access3.getOutput(), constraints);
//		*/
//		
//		Attribute k3 = (Attribute) join2.getOutput().getHeader().get(3);
//		boolean r11 = estimator.isKey(access3.getOutput(), Lists.newArrayList(k3), CollectionUtils.union(keyDependencies, constraints));
//		boolean r12 = estimator.isKey(join2.getOutput(), Lists.newArrayList(k3), CollectionUtils.union(keyDependencies, constraints));
//		
//		/*
//		boolean r13 = estimator.existsInclustionDependency(selection4.getOutput(), join3.getOutput(), constraints);
//		boolean r14 = estimator.existsInclustionDependency(join3.getOutput(), selection4.getOutput(), constraints);
//		*/
		
		Attribute k4 = (Attribute) selection4.getOutput().getHeader().get(1);
		boolean r15 = estimator.isKey(selection4.getOutput(), Lists.newArrayList(k4), CollectionUtils.union(keyDependencies, constraints));
		boolean r16 = estimator.isKey(join3.getOutput(), Lists.newArrayList(k4), CollectionUtils.union(keyDependencies, constraints));
		
		/*
		Pair<Integer, Boolean> estim1 = estimator.constraintDriven(projection0.getOutput(), plan, catalog);
//		Integer estim3 = estimator.commandDriven(projection0.getOutput(), plan, catalog);
		Integer estim5 = estimator.simple(projection0.getOutput(), plan, catalog);
		
		Pair<Integer, Boolean> estim2 = estimator.constraintDriven(projection1.getOutput(), plan, catalog);
//		Integer estim4 = estimator.commandDriven(projection1.getOutput(), plan, catalog);
		Integer estim6 = estimator.simple(projection1.getOutput(), plan, catalog);
		
		Pair<Integer, Boolean> estim7 = estimator.constraintDriven(projection2.getOutput(), plan, catalog);
//		Integer estim8 = estimator.commandDriven(projection2.getOutput(), plan, catalog);
		Integer estim9 = estimator.simple(projection2.getOutput(), plan, catalog);
		
		Pair<Integer, Boolean> estim10 = estimator.constraintDriven(projection3.getOutput(), plan, catalog);
//		Integer estim11 = estimator.commandDriven(projection3.getOutput(), plan, catalog);
		Integer estim12 = estimator.simple(projection3.getOutput(), plan, catalog);
		*/
	}

}
