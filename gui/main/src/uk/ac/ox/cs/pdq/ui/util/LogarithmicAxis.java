// This file is part of PDQ (https://github.com/ProofDrivenQuerying/pdq) which is released under the MIT license.
// See accompanying LICENSE for copyright notice and full details.

package uk.ac.ox.cs.pdq.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.chart.ValueAxis;
import javafx.util.Duration;

// TODO: Auto-generated Javadoc
/**
 * A logarithmic axis implementation for JavaFX 2 charts<br>
 * <br>.
 *
 * @author Kevin Senechal
 */
public class LogarithmicAxis extends ValueAxis<Number> {

	/** The log. */
	private static Logger log = Logger.getLogger(LogarithmicAxis.class);
	
	/** The time of animation in ms. */
	private static final double ANIMATION_TIME = 2000;
	
	/** The lower range timeline. */
	private final Timeline lowerRangeTimeline = new Timeline();
	
	/** The upper range timeline. */
	private final Timeline upperRangeTimeline = new Timeline();

	/** The log upper bound. */
	private final DoubleProperty logUpperBound = new SimpleDoubleProperty();
	
	/** The log lower bound. */
	private final DoubleProperty logLowerBound = new SimpleDoubleProperty();

	/**
	 * Instantiates a new logarithmic axis.
	 */
	public LogarithmicAxis() {
		super(1, 100);
		this.bindLogBoundsToDefaultBounds();
	}

	/**
	 * Instantiates a new logarithmic axis.
	 *
	 * @param lowerBound the lower bound
	 * @param upperBound the upper bound
	 */
	public LogarithmicAxis(double lowerBound, double upperBound) {
		super(lowerBound, upperBound);
		try {
			this.validateBounds(lowerBound, upperBound);
			this.bindLogBoundsToDefaultBounds();
		} catch (IllegalLogarithmicRangeException e) {
			throw new IllegalStateException();
		}
	}

	/**
	 * Bind our logarithmic bounds with the super class bounds, consider the
	 * base 10 logarithmic scale.
	 */
	private void bindLogBoundsToDefaultBounds() {
		this.logLowerBound.bind(new DoubleBinding() {

			{
				super.bind(LogarithmicAxis.this.lowerBoundProperty());
			}

			@Override
			protected double computeValue() {
				return Math.log10(LogarithmicAxis.this.lowerBoundProperty().get());
			}
		});
		this.logUpperBound.bind(new DoubleBinding() {

			{
				super.bind(LogarithmicAxis.this.upperBoundProperty());
			}

			@Override
			protected double computeValue() {
				return Math.log10(LogarithmicAxis.this.upperBoundProperty().get());
			}
		});
	}

	/**
	 * Validate the bounds by throwing an exception if the values are not
	 * conform to the mathematics log interval: ]0,Double.MAX_VALUE]
	 *
	 * @param lowerBound the lower bound
	 * @param upperBound the upper bound
	 * @throws IllegalLogarithmicRangeException the illegal logarithmic range exception
	 */
	private void validateBounds(double lowerBound, double upperBound) throws IllegalLogarithmicRangeException {
		if (lowerBound < 0 || upperBound < 0 || lowerBound > upperBound) {
			throw new IllegalLogarithmicRangeException(
					"The logarithmic range should be include to ]0,Double.MAX_VALUE] and the lowerBound should be less than the upperBound");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Number> calculateMinorTickMarks() {
		Number[] range = this.getRange();
		List<Number> minorTickMarksPositions = new ArrayList<>();
		if (range != null) {

			Number upperBound = range[1];
			double logUpperBound = Math.log10(upperBound.doubleValue());
			int minorTickMarkCount = this.getMinorTickCount();

			for (double i = 0; i <= logUpperBound; i += 1) {
				for (double j = 0; j <= 9; j += (1. / minorTickMarkCount)) {
					double value = j * Math.pow(10, i);
					minorTickMarksPositions.add(value);
				}
			}
		}
		return minorTickMarksPositions;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Number> calculateTickValues(double length, Object range) {
		List<Number> tickPositions = new ArrayList<>();
		if (range != null) {
			Number lowerBound = ((Number[]) range)[0];
			Number upperBound = ((Number[]) range)[1];
			double logLowerBound = Math.log10(lowerBound.doubleValue());
			double logUpperBound = Math.log10(upperBound.doubleValue());
			for (double i = 0; i <= logUpperBound; i += 1) {
				for (double j = 1; j <= 9; j++) {
					double value = j * Math.pow(10, i);
					tickPositions.add(value);
				}
			}
		}
		return tickPositions;
	}

	/* (non-Javadoc)
	 * @see javafx.scene.chart.Axis#getRange()
	 */
	@Override
	protected Number[] getRange() {
		return new Number[] { this.lowerBoundProperty().get(), this.upperBoundProperty().get() };
	}

	/* (non-Javadoc)
	 * @see javafx.scene.chart.Axis#getTickMarkLabel(java.lang.Object)
	 */
	@Override
	protected String getTickMarkLabel(Number value) {
		return this.getTickLabelFormatter().toString(value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setRange(Object range, boolean animate) {
		if (range != null) {
			Number lowerBound = ((Number[]) range)[0];
			Number upperBound = ((Number[]) range)[1];
			try {
				this.validateBounds(lowerBound.doubleValue(), upperBound.doubleValue());
			} catch (IllegalLogarithmicRangeException e) {
				log.error(e.getMessage(),e);
			}
			if (animate) {
				try {
					this.lowerRangeTimeline.getKeyFrames().clear();
					this.upperRangeTimeline.getKeyFrames().clear();

					this.lowerRangeTimeline.getKeyFrames().addAll(
							new KeyFrame(Duration.ZERO, new KeyValue(this.lowerBoundProperty(), this
									.lowerBoundProperty().get())),
							new KeyFrame(new Duration(ANIMATION_TIME), new KeyValue(this.lowerBoundProperty(),
									lowerBound.doubleValue())));

					this.upperRangeTimeline.getKeyFrames().addAll(
							new KeyFrame(Duration.ZERO, new KeyValue(this.upperBoundProperty(), this
									.upperBoundProperty().get())),
							new KeyFrame(new Duration(ANIMATION_TIME), new KeyValue(this.upperBoundProperty(),
									upperBound.doubleValue())));
					this.lowerRangeTimeline.play();
					this.upperRangeTimeline.play();
				} catch (Exception e) {
					this.lowerBoundProperty().set(lowerBound.doubleValue());
					this.upperBoundProperty().set(upperBound.doubleValue());
				}
			}
			this.lowerBoundProperty().set(lowerBound.doubleValue());
			this.upperBoundProperty().set(upperBound.doubleValue());
		}
	}

	/* (non-Javadoc)
	 * @see javafx.scene.chart.ValueAxis#getValueForDisplay(double)
	 */
	@Override
	public Number getValueForDisplay(double displayPosition) {
		double delta = this.logUpperBound.get() - this.logLowerBound.get();
		if (this.getSide().isVertical()) {
			return Math.pow(10, (((displayPosition - this.getHeight()) / -this.getHeight()) * delta)
					+ this.logLowerBound.get());
		}
		return Math.pow(10, (((displayPosition / this.getWidth()) * delta) + this.logLowerBound.get()));
	}

	/* (non-Javadoc)
	 * @see javafx.scene.chart.ValueAxis#getDisplayPosition(java.lang.Number)
	 */
	@Override
	public double getDisplayPosition(Number value) {
		double delta = this.logUpperBound.get() - this.logLowerBound.get();
		double deltaV = Math.log10(value.doubleValue()) - this.logLowerBound.get();
		if (this.getSide().isVertical()) {
			return (1. - ((deltaV) / delta)) * this.getHeight();
		}
		return ((deltaV) / delta) * this.getWidth();
	}
}