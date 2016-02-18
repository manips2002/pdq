package uk.ac.ox.cs.pdq.plan;

import java.util.List;
import java.util.Objects;

import com.google.common.base.Preconditions;

import uk.ac.ox.cs.pdq.db.Attribute;
import uk.ac.ox.cs.pdq.db.TGD;
import uk.ac.ox.cs.pdq.util.Table;

// TODO: Auto-generated Javadoc
/**
 * The Class ProjectCommand.
 *
 * @author Efthymia Tsamoura
 */
public class ProjectCommand implements Command{
	
	/**  The input table *. */
	private final Table input;
	
	/**  The output table *. */
	private final Table output;
	
	/**  The attributes to project *. */
	private final List<Attribute> toProject;
	
	/**
	 * Creates a project command based on the input table and the input attributes that will be projected.
	 *
	 * @param toProject the to project
	 * @param input the input
	 */
	public ProjectCommand(List<Attribute> toProject, Table input) {
		Preconditions.checkNotNull(toProject);
		Preconditions.checkNotNull(input);
		this.input = input;
		this.toProject = toProject;
		this.output = new Table(toProject);
	}

	/* (non-Javadoc)
	 * @see uk.ac.ox.cs.pdq.plan.Command#getOutput()
	 */
	@Override
	public Table getOutput() {
		return this.output;
	}

	/**
	 * Gets the input.
	 *
	 * @return the input
	 */
	public Table getInput() {
		return input;
	}

	/**
	 * Gets the to project.
	 *
	 * @return the to project
	 */
	public List<Attribute> getToProject() {
		return toProject;
	}
	
	/**
	 * Equals.
	 *
	 * @param o Object
	 * @return boolean
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null) {
			return false;
		}
		return ProjectCommand.class.isInstance(o)
				&& this.toProject.equals(((ProjectCommand) o).toProject)
				&& this.input.equals(((ProjectCommand) o).input);
	}

	/**
	 * Hash code.
	 *
	 * @return int
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.toProject, this.input);
	}
}
