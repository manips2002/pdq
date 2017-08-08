package uk.ac.ox.cs.pdq.runtime.exec.iterator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.Assert;

import uk.ac.ox.cs.pdq.datasources.RelationAccessWrapper;
import uk.ac.ox.cs.pdq.datasources.ResetableIterator;
import uk.ac.ox.cs.pdq.datasources.utility.Table;
import uk.ac.ox.cs.pdq.datasources.utility.Tuple;
import uk.ac.ox.cs.pdq.datasources.utility.TupleType;
import uk.ac.ox.cs.pdq.db.AccessMethod;
import uk.ac.ox.cs.pdq.db.Attribute;
import uk.ac.ox.cs.pdq.db.TypedConstant;
import uk.ac.ox.cs.pdq.runtime.util.RuntimeUtilities;


// TODO: Auto-generated Javadoc
/**
 * Access over a relation, where the input are provided by the parent operator.
 * 
 * @author Julien Leblay
 */
public class Access extends TupleIterator {

	/** The input table of the access. */
	protected final RelationAccessWrapper relation;

	/** The access method to use. */
	protected final AccessMethod accessMethod;

	/** The types of all input positions */
	protected final TupleType inputTupleType;

	/** The map some of the inputs to static values. */
	protected final Map<Integer, TypedConstant> inputConstants;
	
	protected final Attribute[] attributesOfInputPositions;

	/** Iterator over the output tuples. */
	protected Map<Tuple, ResetableIterator<Tuple>> outputTuplesCache = null;

	/** Iterator over the output tuples. */
	protected ResetableIterator<Tuple> iterator = null;

	/**Next tuple to returns. */
	protected Tuple nextTuple = null;

	/** The last input tuple bound. */
	private Tuple tupleReceivedFromParent;

	public Access(RelationAccessWrapper relation, AccessMethod accessMethod) {
		this(relation, accessMethod, new HashMap<Integer, TypedConstant>());
	}

	public Access(RelationAccessWrapper relation, AccessMethod accessMethod, Map<Integer, TypedConstant> inputConstants) {
		super(RuntimeUtilities.computeInputAttributes(relation, accessMethod, inputConstants), relation.getAttributes());
		Assert.assertNotNull(relation);
		Assert.assertNotNull(accessMethod);
		for(Integer position:inputConstants.keySet()) {
			Assert.assertTrue(position < relation.getArity());
			Assert.assertTrue(Arrays.asList(accessMethod.getInputs()).contains(position));
		}
		this.relation = relation;
		this.accessMethod = accessMethod;
		this.inputConstants = new LinkedHashMap<>();
		for(java.util.Map.Entry<Integer, TypedConstant> entry:inputConstants.entrySet()) 
			this.inputConstants.put(entry.getKey(), entry.getValue().clone());
		this.attributesOfInputPositions = RuntimeUtilities.computeInputAttributes(relation, accessMethod);
		this.inputTupleType = TupleType.DefaultFactory.createFromTyped(this.attributesOfInputPositions);
	}
	
	@Override
	public TupleIterator[] getChildren() {
		return new TupleIterator[]{};
	}

	@Override
	public TupleIterator getChild(int childIndex) {
		return null;
	}
	
	/**
	 * Gets the relation.
	 *
	 * @return the relation being accessed.
	 */
	public RelationAccessWrapper getRelation() {
		return this.relation;
	}

	/**
	 * Gets the access method.
	 *
	 * @return the access method in use
	 */
	public AccessMethod getAccessMethod() {
		return this.accessMethod;
	}

	public Map<Integer, TypedConstant> getInputConstants() {
		return this.inputConstants;
	}

	/**
	 * 
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(this.getClass().getSimpleName());
		result.append('[').append(this.relation.getName()).append('/');
		result.append(this.accessMethod).append(']');
		return result.toString();
	}

	/**
	 * 
	 * {@inheritDoc}
	 * @see uk.ac.ox.cs.pdq.datasources.ResetableIterator#open()
	 */
	@Override
	public void open() {
		Assert.assertTrue(this.open == null);
		this.outputTuplesCache = new LinkedHashMap<>();
		this.open = true;
		// If there is no dynamic input, bind the empty tuple once and for all
		if (this.inputAttributes.length == 0) {
			receiveTupleFromParentAndPassItToChildren(Tuple.EmptyTuple);
		}
	}
	
	/**
	 * Close.
	 *
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() {
		super.close();
		for (ResetableIterator<Tuple> i: this.outputTuplesCache.values()) {
			if (i instanceof TupleIterator) {
				((TupleIterator) i).close();
			}
		}
		this.outputTuplesCache = null;
	}

	/**
	 * 
	 * {@inheritDoc}
	 * @see uk.ac.ox.cs.pdq.runtime.exec.iterator.TupleIterator#interrupt()
	 */
	@Override
	public void interrupt() {
		Assert.assertTrue(this.open != null && this.open);
		Assert.assertTrue(!this.interrupted);
		this.interrupted = true;
	}

	/**
	 * 
	 * {@inheritDoc}
	 * @see uk.ac.ox.cs.pdq.datasources.ResetableIterator#reset()
	 */
	@Override
	public void reset() {
		Assert.assertTrue(this.open != null && this.open);
		Assert.assertTrue(!this.interrupted);
		this.iterator.reset();
		this.nextTuple();
	}

	/**
	 * 
	 * {@inheritDoc}
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		Assert.assertTrue(this.open != null && this.open);
		if (this.interrupted) {
			return false;
		}
		if (this.nextTuple != null) {
			return true;
		}
		this.nextTuple();
		return this.nextTuple != null;
	}

	/**
	 * 
	 * {@inheritDoc}
	 * @see java.util.Iterator#next()
	 */
	@Override
	public Tuple next() {
		Assert.assertTrue(this.open != null && this.open);
		Assert.assertTrue(!this.interrupted);
		Assert.assertTrue(this.tupleReceivedFromParent != null);
		if (this.eventBus != null) {
			this.eventBus.post(this);
		}
		Tuple result = this.nextTuple;
		this.nextTuple = null;
		if (!this.hasNext() && result == null) {
			throw new NoSuchElementException("End of operator reached.");
		}
		return result;
	}

	/**
	 * Next tuple.
	 */
	public void nextTuple() {
		this.nextTuple = null;
		if (this.interrupted) {
			return;
		}
		if (this.iterator == null) {
			// If iterator has not been set at this stage, it implies all 
			// inputs this access are statically defined.
			// Assert.assertTrue(this.inputType.size() == 0);
			Tuple tupleOfInputConstants = this.makeInputTupleByCombiningInputsFromParentsWithInputConstants(Tuple.EmptyTuple);
			Table inputs = new Table(this.attributesOfInputPositions);
			inputs.appendRow(tupleOfInputConstants);
			this.iterator = this.relation.iterator(this.attributesOfInputPositions, inputs.iterator());
			this.iterator.open();
			this.outputTuplesCache.put(tupleOfInputConstants, this.iterator);
		}
		if (this.iterator.hasNext()) {
			this.nextTuple = this.iterator.next();
		}
	}

	/**
	 * 
	 * {@inheritDoc}
	 * @see uk.ac.ox.cs.pdq.runtime.exec.iterator.TupleIterator#receiveTupleFromParentAndPassItToChildren(uk.ac.ox.cs.pdq.datasources.utility.Tuple)
	 */
	@Override
	public void receiveTupleFromParentAndPassItToChildren(Tuple tuple) {
		Assert.assertTrue(this.open != null && this.open);
		Assert.assertTrue(!this.interrupted);
		Assert.assertTrue(tuple != null);
		Assert.assertTrue(RuntimeUtilities.typeOfAttributesEqualsTupleType(tuple.getType(), this.inputAttributes));
		Tuple combinedInputs = this.makeInputTupleByCombiningInputsFromParentsWithInputConstants(tuple);
		this.iterator = this.outputTuplesCache.get(combinedInputs);
		if (this.iterator == null) {
			Table inputs = new Table(this.attributesOfInputPositions);
			inputs.appendRow(combinedInputs);
			this.iterator = this.relation.iterator(this.attributesOfInputPositions, inputs.iterator());
			this.iterator.open();
			this.outputTuplesCache.put(combinedInputs, this.iterator);
		} else {
			this.iterator.reset();
		}
		this.nextTuple();
		this.tupleReceivedFromParent = tuple;
	}
	
	/**
	 * Make input.
	 *
	 * @param dynamicInput the dynamic input
	 * @return an tuple obtained by mixing input from dynamicInput with inputs
	 * defined statically for this access.
	 */
	private Tuple makeInputTupleByCombiningInputsFromParentsWithInputConstants(Tuple dynamicInput) {
		Object[] result = new Object[this.inputTupleType.size()];
		int j = 0, k = 0;
		for (int i : this.accessMethod.getZeroBasedInputPositions()) {
			TypedConstant staticInput = this.inputConstants.get(i);
			if (staticInput != null) {
				result[k++] = staticInput.getValue();
			} else {
				result[k++] = dynamicInput.getValue(j++);
			}
		}
		return this.inputTupleType.createTuple(result);
	}
}