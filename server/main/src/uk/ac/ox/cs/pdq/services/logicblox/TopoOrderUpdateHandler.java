package uk.ac.ox.cs.pdq.services.logicblox;

import org.apache.log4j.Logger;

import uk.ac.ox.cs.pdq.services.MessageHandler;

import com.google.protobuf.GeneratedMessage;
import com.logicblox.connect.BloxCommand.CommandResponse;
import com.logicblox.connect.BloxCommand.TopoOrderUpdate;
import com.logicblox.connect.BloxCommand.TopoOrderUpdate.Entry;
import com.logicblox.connect.BloxCommand.TopoOrderUpdateResponse;

/**
 * Handles update messages on the topological ordering of predicates and rules,
 * in the database-lifetime execution graph.
 * 
 * @author Julien LEBLAY
 */
public class TopoOrderUpdateHandler implements MessageHandler<TopoOrderUpdate> {

	/** Logger. */
	static final Logger log = Logger.getLogger(TopoOrderUpdateHandler.class);

	/**  Handle on the LogicBlox service. */
	private final SemanticOptimizationService master;

	/**
	 * Default constructor.
	 *
	 * @param master the master
	 */
	public TopoOrderUpdateHandler(SemanticOptimizationService master) {
		this.master = master;
	}

	/**
	 *  Handles the LB message, called "command", which is a BloxCommand.TopoOrderUpdate object defined in the external 
	 *	LB library. As soon as a request comes in, we resolve the associated PDQ context. The incoming message has a list of
	 *  BoxCommand.TopoOrderUpdate.Entry objects. Each entry contains a long number "rank" and a string "name", representing
	 *  the topological order of the relation with that name inside LB. 
	 *  These entries are delegated to Context: if the rank is < 0 the object is removed for the topoOrder map that Context 
	 *  maintains (probably meaning that the relation is not relevant any more). 
	 *  Else a new entry in the Context's map is registered (overwriting older ones with the same rank).
	 */
	@Override
	public GeneratedMessage handle(TopoOrderUpdate command) {
	    log.debug("TopoOrderUpdate: " + command.getWorkspace() + ": "
	    			+ command.getEntriesCount() + " entries.");
		Context context = this.master.resolve(command.getWorkspace());
		try {
			for (Entry entry: command.getEntriesList()) {
				long rank = entry.getRank();
				String name = entry.getObject();
				if (rank < 0) {
				    log.debug("Removing: " + name);
					context.removeRank(name);
					continue;
				}
			    log.debug("Removing: " + entry.getObject());
				context.updateRank(name, rank);
			}
		} catch (Exception e) {
			log.warn(e);
			return CommandResponse.newBuilder()
					.setTopoOrderUpdate(TopoOrderUpdateResponse.newBuilder()
							.setSuccess(false).build()).build();
		}
		return CommandResponse.newBuilder().setTopoOrderUpdate(
				TopoOrderUpdateResponse.newBuilder()
						.setSuccess(true).build()).build();
	}

}
