// This file is part of PDQ (https://github.com/ProofDrivenQuerying/pdq) which is released under the MIT license.
// See accompanying LICENSE for copyright notice and full details.

package uk.ac.ox.cs.pdq.datasources.legacy.services.policies;

import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;

import uk.ac.ox.cs.pdq.datasources.AccessException;
import uk.ac.ox.cs.pdq.datasources.legacy.services.AccessEvent;

/**
 * This handler is required by the EventBus class which does not allow
 * subscribing method to throw exceptions.
 * 
 * @author Julien Leblay
 *
 */
public class UsageViolationExceptionHandler implements SubscriberExceptionHandler {

	/*
	 * (non-Javadoc)
	 * @see com.google.common.eventbus.SubscriberExceptionHandler#handleException(java.lang.Throwable, com.google.common.eventbus.SubscriberExceptionContext)
	 */
	@Override
	public void handleException(Throwable arg0, SubscriberExceptionContext arg1) {
		if (arg0 instanceof AccessException) {
			((AccessEvent) arg1.getEvent()).setUsageViolationMessage(arg0.getMessage());
		}
		arg0.printStackTrace();
	}

}
