// This file is part of PDQ (https://github.com/ProofDrivenQuerying/pdq) which is released under the MIT license.
// See accompanying LICENSE for copyright notice and full details.

package uk.ac.ox.cs.pdq.ui.model;

import java.util.List;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableNumberValue;
import javafx.beans.value.ObservableValue;
import uk.ac.ox.cs.pdq.algebra.Plan;
import uk.ac.ox.cs.pdq.cost.Cost;
import uk.ac.ox.cs.pdq.planner.linear.LinearChaseConfiguration;
import uk.ac.ox.cs.pdq.ui.proof.Proof;

// TODO: Auto-generated Javadoc
/**
 * Representation a Planner's search space state.
 * 
 * @author Julien Leblay
 *
 */
public class ObservableSearchState {

	/**  Time elapsed since the beginning of the search. */
	private final SimpleDoubleProperty time =  new SimpleDoubleProperty(this, "time");

	/**  Number of iterations since the beginning of the search. */
	private final SimpleIntegerProperty iterations =  new SimpleIntegerProperty(this, "iterations");

	/**  Cost of the best plan found so far. */
	private final SimpleObjectProperty<Number> cost = new SimpleObjectProperty<>(this, "cost");

	/**  Best plan found so far. */
	private final SimpleObjectProperty<Plan> plan = new SimpleObjectProperty<>(this, "plan");

	/**  Best proof found so far. */
	private final SimpleObjectProperty<Proof> proof = new SimpleObjectProperty<>(this, "proof");

	/**
	 * Default constructor.
	 *
	 * @param time the time
	 * @param rounds the rounds
	 * @param pl the pl
	 * @param bestConfigurationsList the best configurations list
	 */
	public ObservableSearchState(Double time, Integer rounds, Plan pl, Cost co, List<LinearChaseConfiguration> bestConfigurationsList) {
		this.plan.set(pl);
		this.proof.set(bestConfigurationsList == null ? null : Proof.toProof(bestConfigurationsList));
		this.cost.set(pl == null ? null : co.getValue());
		this.time.set(time);
		this.iterations.set(rounds);
	}

	/**
	 * Time property.
	 *
	 * @return the observable number value
	 */
	public ObservableNumberValue timeProperty() {
		return this.time;
	}
	
	/**
	 * Iterations property.
	 *
	 * @return the observable number value
	 */
	public ObservableNumberValue iterationsProperty() {
		return this.iterations;
	}

	/**
	 * Proof property.
	 *
	 * @return the observable value
	 */
	public ObservableValue<Proof> proofProperty() {
		return this.proof;
	}
	
	/**
	 * Cost property.
	 *
	 * @return the observable value
	 */
	public ObservableValue<Number> costProperty() {
		return this.cost;
	}
	
	/**
	 */
	public Plan getPlan() {
		return this.plan.get();
	}
	
	/**
	 */
	public Proof getProof() {
		return this.proof.get();
	}

	/**
	 */
	public Number getTime() {
		return this.time.get();
	}

	/**
	 */
	public Integer getMaxIterations() {
		return this.iterations.get();
	}

	/**
	 */
	public Number getCost() {
		return this.cost.get();
	}
	
	/**
	 * Sets the proof.
	 *
	 * @param p the new proof
	 */
	public void setProof(Proof p) {
		this.proof.set(p);
	}
	
	/**
	 * Sets the cost.
	 *
	 * @param c the new cost
	 */
	public void setCost(Number c) {
		this.cost.set(c);
	}
	
	/**
	 * Sets the time.
	 *
	 * @param t the new time
	 */
	public void setTime(Double t) {
		this.time.set(t);
	}
	
	/**
	 * Sets the iterations.
	 *
	 * @param i the new iterations
	 */
	public void setIterations(Integer i) {
		this.iterations.set(i);
	}

}
