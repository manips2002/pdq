// This file is part of PDQ (https://github.com/ProofDrivenQuerying/pdq) which is released under the MIT license.
// See accompanying LICENSE for copyright notice and full details.

package uk.ac.ox.cs.pdq.util;

/**
	@author Efthymia Tsamoura and Mark Ridler
	
	This package implements a number of utility classes.
	
	The contents of this sub-package include:
	-- ConsistencyChecker, which is a generic interface to check the consistency of the input parameters.
	-- DistinctIterator, which is a wrapper for an iterator to ignore duplicate elements.
	-- EventHandler, which is a super-interface to all event planner handlers.
	-- GlobalCounterProvider, which provides an auto-incremented number.
	-- LimitReachedException, which is an exception that occurs when a task's timeout is reached.
	-- Triple, which is a triple of elements.
	-- Utility, which is a collection of functions that don't fit anywhere else
*
**/