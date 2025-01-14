// This file is part of PDQ (https://github.com/ProofDrivenQuerying/pdq) which is released under the MIT license.
// See accompanying LICENSE for copyright notice and full details.

package uk.ac.ox.cs.pdq.ui.model;

import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.TGD;

// TODO: Auto-generated Javadoc
/**
 * 
 */
public class ObservableDependency extends TGD {

	/** */
	private static final int MAX_LENGTH = 30;
	
	/**  */
	private Dependency dependency;
	
	/**
	 * Instantiates a new observable dependency.
	 *
	 * @param dep the dep
	 */
	public ObservableDependency(TGD dep) {
		super(dep.getBodyAtoms(), dep.getHeadAtoms(), dep.getName());
		this.dependency = dep;
	}

	/* (non-Javadoc)
	 * @see uk.ac.ox.cs.pdq.db.TGD#toString()
	 */
	@Override
	public String toString() {
		String result = String.valueOf(this.dependency);
		if (result.length() > MAX_LENGTH) {
			result = result.substring(MAX_LENGTH) + "...";
		}
		return result;
	}
}