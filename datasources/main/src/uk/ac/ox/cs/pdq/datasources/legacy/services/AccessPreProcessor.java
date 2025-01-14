// This file is part of PDQ (https://github.com/ProofDrivenQuerying/pdq) which is released under the MIT license.
// See accompanying LICENSE for copyright notice and full details.

package uk.ac.ox.cs.pdq.datasources.legacy.services;

import com.google.common.eventbus.Subscribe;


/**
 * AccessPreProcessor event handler, that is triggered after an access was performed.
 *
 * @author Julien Leblay
 * @param <T> the generic type
 */
public interface AccessPreProcessor<T extends RequestEvent> {
	
	/**
	 * Method called upon an access RequestEvent.
	 *
	 * @param event the event
	 */
	@Subscribe
	void processAccessRequest(T event) ;
}
