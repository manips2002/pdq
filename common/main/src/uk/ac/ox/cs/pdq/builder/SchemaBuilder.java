// This file is part of PDQ (https://github.com/ProofDrivenQuerying/pdq) which is released under the MIT license.
// See accompanying LICENSE for copyright notice and full details.

package uk.ac.ox.cs.pdq.builder;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Preconditions;

import uk.ac.ox.cs.pdq.db.AccessMethodDescriptor;
import uk.ac.ox.cs.pdq.db.Attribute;
import uk.ac.ox.cs.pdq.db.Relation;
import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.db.View;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.LinearGuarded;
import uk.ac.ox.cs.pdq.fol.TGD;

// TODO: Auto-generated Javadoc
/**
 * Helper class for build schema. Allows incrementally construction, and 
 * performs various validations and checks upon initialization.
 * @author Julien Leblay
 */
public class SchemaBuilder implements uk.ac.ox.cs.pdq.builder.Builder<Schema> {

	/** The relations. */
	private Map<String, Relation> relations = new LinkedHashMap<>();
	
	/** The dependencies. */
	private Map<Integer, Dependency> dependencies = new LinkedHashMap<>();
	
	/** The disable dependencies. */
	private boolean disableDependencies = false;

	/**
	 * Instantiates a schema from an existing one.
	 */
	public SchemaBuilder() {
	}

	/**
	 * Instantiates a schema from an existing one.
	 *
	 * @param schema the schema
	 */
	public SchemaBuilder(Schema schema) {
		this.addRelations(schema.getRelations());
		this.addDependencies(schema.getAllDependencies());
	}

	/**
	 * Add the given relation to the schema under construction iff no
	 * relation other than TemporaryRelation under the name already exists.
	 *
	 * @param r the r
	 * @return this builder
	 */
	public SchemaBuilder addRelation(Relation r) {
		Preconditions.checkArgument(r != null);
		Relation existing = this.relations.get(r.getName());
		if (existing != null
				&& !(existing instanceof SchemaBuilder.TemporaryRelation)
				&& r instanceof SchemaBuilder.TemporaryRelation) {
			throw new IllegalStateException();
		}
		this.relations.put(r.getName(), r);
		return this;
	}

	/**
	 * Change the given relation's access methods to the ones provided
	 * as parameter. Any pre-existing access methods are discarded.
	 * If no access method is provided, the relation becomes inaccessible.
	 * The method has no effect, if no relation by the given name currently
	 * exists, and the relation is not a temporary one.
	 *
	 * @param name String
	 * @param am the am
	 * @return this builder
	 */
	public SchemaBuilder setAccessMethods(String name, AccessMethodDescriptor[] am) {
		Relation existing = this.relations.get(name);
		if (existing != null) {
		}
		return this;
	}

	/**
	 * Add the given relation to the schema under construction, if it does
	 * already exists. oOherwise, places a TemporaryRelation there under
	 * that name.
	 * @param name String
	 * @param attributes List<Attribute>
	 * @return this builder
	 */
	public Relation addOrReplaceRelation(String name, Attribute[] attributes) {
		return this.addOrReplaceRelation(name, attributes, false);
	}

	/**
	 * Add the given relation to the schema under construction, if it does
	 * already exists. oOherwise, places a TemporaryRelation there under
	 * that name.
	 *
	 * @param name String
	 * @param attributes List<Attribute>
	 * @param isEquality the is equality
	 * @return this builder
	 */
	public Relation addOrReplaceRelation(String name, Attribute[] attributes, boolean isEquality) {
		Relation result = this.relations.get(name);
		if (result != null && result.getAttributes().equals(attributes)) {
			return result;
		}
		this.relations.remove(name);
		result = new TemporaryRelation(name, attributes, isEquality);
		this.addRelation(result);
		return result;
	}


	/**
	 * Adds the dependency.
	 *
	 * @param dep IC
	 * @return this builder
	 */
	public SchemaBuilder addDependency(Dependency dep) {
		this.dependencies.put(((TGD) dep).getId(), dep);
		return this;
	}

	/**
	 * Removes the dependency.
	 *
	 * @param ic IC
	 * @return this builder
	 */
	public SchemaBuilder removeDependency(Dependency ic) {
		if (this.dependencies.remove(((TGD) ic).getId()) == null) {
			for (Iterator<Entry<Integer, Dependency>> it =
					this.dependencies.entrySet().iterator();
					it.hasNext();) {
			}
		};
		return this;
	}

	/**
	 * Removes the relation.
	 *
	 * @param r the relation to remove
	 * @return this builder
	 */
	public SchemaBuilder removeRelation(Relation r) {
		this.relations.remove(r.getName());
		return this;
	}

	/**
	 * Adds the dependencies.
	 *
	 * @param ics the ics
	 * @return this builder
	 */
	public SchemaBuilder addDependencies(Dependency[] ics) {
		for (Dependency ic : ics) {
			this.addDependency(ic);
		}
		return this;
	}

	/**
	 * Removes all dependencies from the schema being built.
	 * @return this builder
	 */
	public SchemaBuilder disableDependencies() {
		this.disableDependencies = true;
		return this;
	}

	/**
	 * Adds the relations.
	 *
	 * @param relations the relations
	 * @return this builder
	 */
	public SchemaBuilder addRelations(Relation[] relations) {
		for (Relation relation : relations) {
			this.addRelation(relation);
		}
		return this;
	}

	/**
	 * Adds the schema.
	 *
	 * @param schema the schema
	 * @return this builder
	 */
	public SchemaBuilder addSchema(Schema schema) {
		this.addRelations(schema.getRelations());
		this.addDependencies(schema.getAllDependencies());
		return this;
	}

	/**
	 * Gets the relation.
	 *
	 * @param name the name
	 * @return the relation currently held in the builder under the given name
	 */
	public Relation getRelation(String name) {
		return this.relations.get(name);
	}

	/**
	 * Gets the relations.
	 *
	 * @return a Collection of all relations currently held by the builder.
	 */
	public Collection<Relation> getRelations() {
		return this.relations.values();
	}

	/**
	 * Gets the relation map.
	 *
	 * @return a map from relation names to relations for all currently held
	 *         by the builder.
	 */
	public Map<String, Relation> getRelationMap() {
		return this.relations;
	}

	/**
	 * Gets the dependencies.
	 *
	 * @return Collection<IC>
	 */
	public Collection<Dependency> getDependencies() {
		return this.dependencies.values();
	}

	/**
	 * Ensure every view has its corresponding definition as constraints.
	 *
	 * @param view the view
	 */
	private void ensureViewDefinition(View view) {
		LinearGuarded d = view.getViewToRelationDependency();
		LinearGuarded t = this.findViewDependency(view);
		if (d != null) {
			TGD inverse = view.getRelationToViewDependency();

			if (t == null) {
				this.dependencies.put(d.getId(), d);
			}
			TGD i = this.findDependency(inverse);
			if (i == null) {
				this.dependencies.put(inverse.getId(), inverse);
			}
		} else {
			if (t != null) {
//				view.setDependency(t);
				TGD inverse = view.getRelationToViewDependency();
				TGD i = this.findDependency(inverse);
				if (i == null) {
					this.dependencies.put(inverse.getId(), inverse);
				}
			} else {
				throw new IllegalStateException("No linear guarded dependency found for view " + view.getName());
			}
		}
	}

	/**
	 * Ensure every relation's foreign has its corresponding constraints.
	 *
	 * @param relation Relation
	 */
	private void ensureForeignKeyDefinition(Relation relation) {
		throw new RuntimeException("ensureForeignKeyDefinition");
	}

	/**
	 * Remove dependencies that refer to relation that are not part of the schema.
	 */
	private void removeOrphanDependencies() {
		for (Iterator<Integer> i = this.dependencies.keySet().iterator(); i.hasNext();) {
			Dependency ic = this.dependencies.get(i.next());
			for (Atom p: ic.getBodyAtom(0).getAtoms()) {
				if (this.relations.get(p.getPredicate().getName()) == null) {
					i.remove();
					break;
				}
			}
		}
		for (Iterator<Integer> i = this.dependencies.keySet().iterator(); i.hasNext();) {
			Dependency ic = this.dependencies.get(i.next());
			for (Atom p: ic.getHeadAtom(0).getAtoms()) {
				if (this.relations.get(p.getPredicate().getName()) == null) {
					i.remove();
					break;
				}
			}
		}
	}

	/**
	 * Derives the dependencies that correspond to the schema views.
	 */
	private void consolidateDependencies() {
		for (Relation r : this.relations.values()) {
			if (r instanceof View) {
				this.ensureViewDefinition((View) r);
			} else {
				this.ensureForeignKeyDefinition(r);
			}
		}
		this.removeOrphanDependencies();
	}

	/**
	 * Find view dependency.
	 *
	 * @param v the v
	 * @return the linear guarded dependency currently held in the builder
	 *         for the given view. null if no such dependency was found.
	 */
	private LinearGuarded findViewDependency(View v) {
		if (this.dependencies != null) {
			for (Dependency ic : this.dependencies.values()) {
				if (ic.getBodyAtom(0).getAtoms().length == 1) {
					if (ic.getBodyAtom(0).getAtoms()[0]
							.getPredicate().getName().equals(v.getName())) {
						return (LinearGuarded) ic;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Find dependency.
	 *
	 * @param dep TGD
	 * @return the dependency currently held in the builder that is equal
	 *         (modulo the ID) to the given dependency.
	 */
	private TGD findDependency(TGD dep) {
		throw new RuntimeException("ensureForeignKeyDefinition");
	}

	/**
	 * Builds a new instance of Schema containing all the relation and
	 * dependencies added so far, plus all dependencies derivable from view
	 * definitions and foreign keys.
	 *
	 * @return a new instance of Schema
	 * @see uk.ac.ox.cs.pdq.builder.Builder#build()
	 */
	@Override
	public Schema build() {
		if (!this.disableDependencies) {
			this.consolidateDependencies();
		} else {
			this.dependencies.clear();
		}
		
		Relation[] relations = new Relation[this.relations.values().size()];
		int i = 0;
		for(Relation r : this.relations.values()) relations[i++] = r;
		Dependency[] dependencies = new Dependency[this.dependencies.values().size()];
		i = 0;
		for(Dependency d: this.dependencies.values()) dependencies[i++] = d;
		
		return new Schema(relations, dependencies);
	}

	/**
	 * A relation that temporarily hold signature related information in
	 * preparation for instantiatiating a relation.
	 * @author Julien Leblay
	 */
	private static class TemporaryRelation extends Relation {
		
		/** The Constant serialVersionUID. */
		private static final long serialVersionUID = 7049363904713889121L;

		/**
		 * Constructor for TemporaryRelation.
		 * @param name String
		 * @param attributes List<Attribute>
		 * @param isEq true if the relation acts as an equality
		 */
		public TemporaryRelation(String name, Attribute[] attributes, boolean isEq) {
			super(name, attributes, isEq);
		}
	}
}