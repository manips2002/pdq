// This file is part of PDQ (https://github.com/ProofDrivenQuerying/pdq) which is released under the MIT license.
// See accompanying LICENSE for copyright notice and full details.

package uk.ac.ox.cs.pdq.planner.linear.cost;

/**
 * The linear proofs that are constructed during exploration are organised into a tree. 
 * The nodes of this tree correspond to (partial) linear configurations. 
 * This package contains classes to propagate a path-to-success (if it exists) to the root a plan tree.
 * Propagation takes place every time a new successful path is found or when a path is found to be equivalent to another path in the plan tree.
	@author Efthymia Tsamoura
 */
