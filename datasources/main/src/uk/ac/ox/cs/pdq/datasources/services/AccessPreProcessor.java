package uk.ac.ox.cs.pdq.datasources.services;


/**
 * AccessPreProcessor event handler, that is triggered after an access was performed.
 *
 * @author Julien Leblay
 * @param <T> the generic type
 */
public interface AccessPreProcessor<T> {
	
	/**
	 * Method called upon an access RequestEvent.
	 *
	 * @param event the event
	 */
	void processAccessRequest(T event) ;
}
