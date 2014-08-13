/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.core.schedulers;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Multimaps.synchronizedMultimap;
import static java.util.Collections.synchronizedMap;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.core.workflow.AbstractWorkflowStep;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskContext;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskResult;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskRunner;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepResult;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepStatus;

/**
 * This class define an abstract task scheduler.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractTaskScheduler implements TaskScheduler {

  private static final int SLEEP_TIME_IN_MS = 500;

  private final Multimap<WorkflowStep, Integer> submittedContexts;
  private final Multimap<WorkflowStep, Integer> runningContexts;
  private final Multimap<WorkflowStep, Integer> doneContexts;
  private final Map<Integer, WorkflowStep> contexts;
  private final Map<WorkflowStep, WorkflowStepStatus> status;
  private final Map<WorkflowStep, WorkflowStepResult> results;

  private boolean isStarted;
  private boolean isStopped;
  private boolean isPaused;

  //
  // Protected methods
  //

  /**
   * Add a task result to its step result.
   * @param step the step of the result
   * @param result the result to add
   */
  private void addResult(final WorkflowStep step, final TaskResult result) {

    this.results.get(step).addResult(result);
  }

  /**
   * Get the step related to a context.
   * @param context the context
   * @return the step related to the context
   */
  protected WorkflowStep getStep(final TaskContext context) {

    checkNotNull(context, "context argument cannot be null");

    return getStep(context.getId());
  }

  /**
   * Get the step related to a context.
   * @param contextId the context id
   * @return the step related to the context
   */
  protected WorkflowStep getStep(final int contextId) {

    // Test if the contextId has been submitted
    checkState(this.contexts.containsKey(contextId), "The context ("
        + contextId + ") has never been submitted");

    return this.contexts.get(contextId);
  }

  /**
   * Set a context in the running state
   * @param context the context
   */
  private void addRunningContext(final TaskContext context) {

    checkNotNull(context, "context argument cannot be null");

    addRunningContext(context.getId());
  }

  /**
   * Set a context in the running state
   * @param contextId the context id
   */
  private void addRunningContext(final int contextId) {

    // Check execution state
    checkExecutionState();

    // Test if the contextId has been submitted
    checkState(this.contexts.containsKey(contextId), "The context ("
        + contextId + ") has never been submitted");

    // Test if the context is already running
    checkState(!this.runningContexts.containsValue(contextId), "The context ("
        + contextId + ") already running");

    // Test if the context has been already done
    checkState(!this.doneContexts.containsValue(contextId), "The context ("
        + contextId + ") has been already done");

    synchronized (this) {
      this.runningContexts.put(getStep(contextId), contextId);
    }
  }

  /**
   * Set a context in done state.
   * @param context the context
   */
  private void addDoneContext(final TaskContext context) {

    checkNotNull(context, "context argument cannot be null");

    addDoneContext(context.getId());
  }

  /**
   * Set a context in done state.
   * @param contextId the context id
   */
  private void addDoneContext(final int contextId) {

    // Check execution state
    checkExecutionState();

    // Test if the contextId has been submitted
    checkState(this.contexts.containsKey(contextId), "The context ("
        + contextId + ") has never been submitted");

    // Test if the context is running
    checkState(this.runningContexts.containsValue(contextId), "The context ("
        + contextId + ") is not running");

    // Test if the context has been already done
    checkState(!this.doneContexts.containsValue(contextId), "The context ("
        + contextId + ") has been already done");

    final WorkflowStep step = getStep(contextId);
    synchronized (this) {
      this.runningContexts.remove(step, contextId);
      this.doneContexts.put(step, contextId);
    }
  }

  /**
   * Set the state of the context before executing a task.
   * @param context the context to execute
   */
  protected void beforeExecuteTask(final TaskContext context) {

    checkNotNull(context, "context argument is null");

    // Check execution state
    checkExecutionState();

    // Update counters
    addRunningContext(context);
  }

  /**
   * Set the state of the context after executing a task.
   * @param context the context to execute
   */
  protected void afterExecuteTask(final TaskContext context,
      final TaskResult result) {

    checkNotNull(context, "context argument is null");
    checkNotNull(result, "result argument is null");

    // Add the context result to the step result
    addResult(getStep(context.getId()), result);

    // Update counters
    addDoneContext(context);
  }

  /**
   * Default executing context method.
   * @param context the context
   * @return a TaskResult object
   */
  protected TaskResult executeTask(final TaskContext context) {

    checkNotNull(context, "context argument is null");

    // Get the step of the context
    final WorkflowStep step = getStep(context.getId());

    // Create context runner
    final TaskRunner contextRunner = new TaskRunner(context, getStatus(step));

    // Run the step context
    contextRunner.run();

    // Return the result
    return contextRunner.getResult();
  }

  //
  // TaskScheduler interface
  //

  @Override
  public void submit(final WorkflowStep step, final Set<TaskContext> contexts) {

    checkNotNull(contexts, "contexts argument cannot be null");

    for (TaskContext context : contexts) {
      submit(step, context);
    }
  }

  @Override
  public void submit(final WorkflowStep step, final TaskContext context) {

    // Check execution state
    checkExecutionState();

    checkNotNull(step, "step argument cannot be null");
    checkNotNull(context, "context argument cannot be null");

    // Test if the context has been already submitted
    checkState(!this.submittedContexts.containsEntry(step, context.getId()),
        "The context (#" + context.getId() + ") has been already submitted");

    synchronized (this) {

      // If this the first context of the step
      if (!this.status.containsKey(step)) {

        this.status.put(step, new WorkflowStepStatus(
            (AbstractWorkflowStep) step));
        this.results.put(step, new WorkflowStepResult(
            (AbstractWorkflowStep) step));
      }

      this.submittedContexts.put(step, context.getId());
      this.contexts.put(context.getId(), step);
    }
  }

  @Override
  public WorkflowStepStatus getStatus(final WorkflowStep step) {

    return this.status.get(step);
  }

  public WorkflowStepResult getResult(final WorkflowStep step) {

    return this.results.get(step);
  }

  @Override
  public int getTaskSubmitedCount(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    // Test if contexts for the step has been submitted
    if (!this.submittedContexts.containsKey(step)) {
      return 0;
    }

    return this.submittedContexts.get(step).size();
  }

  @Override
  public int getTaskRunningCount(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    // Test if contexts for the step has been submitted
    if (!this.runningContexts.containsKey(step)) {
      return 0;
    }

    return this.runningContexts.get(step).size();
  }

  @Override
  public int getTaskDoneCount(final WorkflowStep step) {

    checkNotNull(step, "step argument cannot be null");

    // Test if contexts for the step has been submitted
    if (!this.doneContexts.containsKey(step)) {
      return 0;
    }

    return this.doneContexts.get(step).size();
  }

  @Override
  public int getTotalTaskSubmitedCount() {

    return this.submittedContexts.size();
  }

  @Override
  public int getTotalTaskRunningCount() {

    return this.runningContexts.size();
  }

  @Override
  public int getTotalTaskDoneCount() {

    return this.doneContexts.size();
  }

  int getTotalWaitingCount() {

    return getTotalTaskSubmitedCount()
        - getTotalTaskRunningCount() - getTotalTaskDoneCount();
  }

  @Override
  public void waitEndOfTasks(final WorkflowStep step) {

    // Check execution state
    checkExecutionState();

    while (!isStopped()
        && (getTaskRunningCount(step) > 0 || getTaskSubmitedCount(step) > getTaskDoneCount(step))) {

      try {
        Thread.sleep(SLEEP_TIME_IN_MS);
      } catch (InterruptedException e) {
        EoulsanLogger.getLogger().severe(e.getMessage());
      }
    }
  }

  @Override
  public void start() {

    // Check execution state
    checkState(!this.isStopped, "The scheduler is stopped");

    synchronized (this) {
      this.isStarted = true;
    }
  }

  protected boolean isStarted() {
    return this.isStarted;
  }

  @Override
  public void stop() {

    // Check execution state
    checkExecutionState();

    synchronized (this) {
      this.isStopped = true;
    }
  }

  protected boolean isStopped() {
    return this.isStopped;
  }

  /**
   * Pause the scheduler.
   */
  void pause() {

    // Check execution state
    checkExecutionState();

    checkState(!this.isPaused, "The execution is already paused");

    synchronized (this) {
      this.isPaused = true;
    }
  }

  /**
   * Resume the scheduler.
   */
  void resume() {

    // Check execution state
    checkExecutionState();

    checkState(this.isPaused, "The execution is not paused");

    synchronized (this) {
      this.isPaused = false;
    }
  }

  /**
   * Test if the scheduler is paused.
   * @return true if the scheduler is paused
   */
  boolean isPaused() {
    return this.isPaused;
  }

  private void checkExecutionState() {

    checkState(this.isStarted, "The scheduler is not started");
    checkState(!this.isStopped, "The scheduler is stopped");
  }

  //
  // Constructor
  //

  /**
   * Protected constructor.
   */
  protected AbstractTaskScheduler() {

    final Multimap<WorkflowStep, Integer> mm1 = HashMultimap.create();
    final Multimap<WorkflowStep, Integer> mm2 = HashMultimap.create();
    final Multimap<WorkflowStep, Integer> mm3 = HashMultimap.create();

    this.submittedContexts = synchronizedMultimap(mm1);
    this.runningContexts = synchronizedMultimap(mm2);
    this.doneContexts = synchronizedMultimap(mm3);

    final Map<Integer, WorkflowStep> m1 = Maps.newHashMap();
    final Map<WorkflowStep, WorkflowStepStatus> m2 = Maps.newHashMap();
    final Map<WorkflowStep, WorkflowStepResult> m3 = Maps.newHashMap();

    this.contexts = synchronizedMap(m1);
    this.status = synchronizedMap(m2);
    this.results = synchronizedMap(m3);
  }

}