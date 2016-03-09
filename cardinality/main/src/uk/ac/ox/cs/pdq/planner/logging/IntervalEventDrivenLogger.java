/*
 * 
 */
package uk.ac.ox.cs.pdq.planner.logging;

import uk.ac.ox.cs.pdq.EventHandler;
import uk.ac.ox.cs.pdq.logging.ProgressLogger;
import uk.ac.ox.cs.pdq.planner.explorer.Explorer;

import com.google.common.eventbus.Subscribe;

// TODO: Auto-generated Javadoc
/**
 * This logger works as a proxy to other progress logger, and performs a log
 * every time a Explorer event is thrown, modulo some predefined intervals.
 *
 * @author Julien Leblay
 *
 */
public class IntervalEventDrivenLogger implements EventHandler {

	/** The intervals. */
	private final int intervals;
	
	/** The short intervals. */
	private final int shortIntervals;
	
	/** The logger. */
	private final ProgressLogger logger;

	/**
	 * Constructor for IntervalEventDrivenLogger.
	 * @param pLog ProgressLogger
	 * @param intervals int
	 * @param shortIntervals int
	 */
	public IntervalEventDrivenLogger(ProgressLogger pLog, int intervals, int shortIntervals) {
		this.logger = pLog;
		this.intervals = intervals;
		this.shortIntervals = shortIntervals;
	}

	/**
	 * Log.
	 *
	 * @param explorer Explorer<?>
	 */
	@Subscribe
	public void log(Explorer<?> explorer) {
		int rounds = explorer.getRounds();
		if (rounds % this.intervals == 0
				|| rounds % this.shortIntervals == 0) {
			this.logger.log();
		}
	}

	/**
	 * Force log.
	 *
	 * @param additionalInfo String
	 */
	public void forceLog(String additionalInfo) {
		this.logger.log(additionalInfo);
	}
}