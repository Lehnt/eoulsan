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

package fr.ens.transcriptome.eoulsan.core.workflow;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.core.AbstractPorts;
import fr.ens.transcriptome.eoulsan.core.InputPort;
import fr.ens.transcriptome.eoulsan.core.InputPorts;

/**
 * This class define a group of Workflow input ports.
 * @since 1.3
 * @author Laurent Jourdren
 */
class WorkflowInputPorts extends AbstractPorts<WorkflowInputPort> {

  //
  // Static methods
  //

  static WorkflowInputPorts noInputPort() {

    final Set<WorkflowInputPort> ports = Collections.emptySet();
    return new WorkflowInputPorts(ports);
  }

  /**
   * Convert an OutputPorts object to a set of WorkflowOutputPort
   * @param step step related to the WorkflowOutputPort objects
   * @param ports ports to convert
   * @return a new set
   */
  private static final Set<WorkflowInputPort> convert(
      final AbstractWorkflowStep step, final InputPorts ports) {

    if (ports == null)
      throw new NullPointerException("Ports is null");

    final Set<WorkflowInputPort> result = new HashSet<WorkflowInputPort>();

    for (InputPort port : ports) {
      if (port != null)
        result.add(new WorkflowInputPort(step, port.getName(),
            port.getFormat(), port.getCompressionsAccepted(), port
                .isRequieredInWorkingDirectory()));

    }

    return result;
  }

  //
  // Constructor.
  //

  /**
   * Constructor.
   * @param ports ports to add.
   */
  WorkflowInputPorts(final Set<WorkflowInputPort> ports) {
    super(ports);
  }

  /**
   * Constructor.
   * @param step step related to the WorkflowOutputPort objects
   * @param ports port to convert
   */
  WorkflowInputPorts(final AbstractWorkflowStep step, final InputPorts ports) {

    super(convert(step, ports));
  }

}