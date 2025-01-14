// This file is part of PDQ (https://github.com/ProofDrivenQuerying/pdq) which is released under the MIT license.
// See accompanying LICENSE for copyright notice and full details.

package uk.ac.ox.cs.pdq.db;

import java.io.Serializable;
import java.lang.reflect.Type;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import uk.ac.ox.cs.pdq.io.jaxb.adapters.AttributeAdapter;

/**
 * Represents a relation's attribute.
 *
 * @author Efthymia Tsamoura
 * @author Julien Leblay
 * @author Stefano
 */
@XmlJavaTypeAdapter(AttributeAdapter.class)
public class Attribute implements Serializable {
	private static final long serialVersionUID = -2103116468417078713L;

	/**  The attribute's name. */
	protected final String name;

	/**  The attribute's type. */
	protected final Type type;

	/**  String representation of the object. */
	protected String toString = null;

	protected Attribute(Type type, String name) {
		assert (type != null);
		assert (name != null);
		assert name != null;
		this.type = type;
		this.name = name;
	}

	public Attribute(Attribute attribute) {
		this(attribute.type, attribute.name);
	}

	public Type getType() {
		return this.type;
	}
	
	public String getName() {
		return this.name;
	}
	
	public static Attribute create(Type type, String name) {
		return Cache.attribute.retrieve(new Attribute(type, name));
	}

	@Override
	public String toString() {
		if (this.toString == null) {
			StringBuilder result = new StringBuilder();
			result.append(this.name);
			this.toString = result.toString().intern();
		}
		return this.toString;
	}
}
