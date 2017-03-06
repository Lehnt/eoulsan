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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.modules.mapping;

import static fr.ens.biologie.genomique.eoulsan.CommonHadoop.HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME;
import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.READS_FASTQ;

import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.bio.readsfilters.MultiReadFilter;
import fr.ens.biologie.genomique.eoulsan.bio.readsfilters.MultiReadFilterBuilder;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.Version;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.eoulsan.util.ReporterIncrementer;

/**
 * This class define an abstract module for read filtering.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractReadsFilterModule extends AbstractModule {

  protected static final String MODULE_NAME = "filterreads";

  protected static final String COUNTER_GROUP = "reads_filtering";

  private Map<String, String> readsFiltersParameters;
  private int reducerTaskCount = -1;

  //
  // Getters
  //

  /**
   * Get the parameters of the read filter.
   * @return a map with all the parameters of the filter
   */
  protected Map<String, String> getReadFilterParameters() {

    return this.readsFiltersParameters;
  }

  /**
   * Get the reducer task count.
   * @return the reducer task count
   */
  protected int getReducerTaskCount() {

    return this.reducerTaskCount;
  }

  //
  // Module methods
  //

  @Override
  public String getName() {

    return MODULE_NAME;
  }

  @Override
  public String getDescription() {

    return "This step filters reads.";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {
    return singleInputPort(READS_FASTQ);
  }

  @Override
  public OutputPorts getOutputPorts() {
    return singleOutputPort(READS_FASTQ);
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    final MultiReadFilterBuilder filterBuilder = new MultiReadFilterBuilder();

    for (Parameter p : stepParameters) {

      // Check if the parameter is deprecated
      checkDeprecatedParameter(context, p);

      switch (p.getName()) {

      case HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME:
        this.reducerTaskCount = p.getIntValueGreaterOrEqualsTo(1);

        break;

      default:
        filterBuilder.addParameter(p.getName(), p.getStringValue());
        break;
      }

    }

    // Force parameter checking
    filterBuilder.getReadFilter();

    this.readsFiltersParameters = filterBuilder.getParameters();
  }

  //
  // Other methods
  //

  /**
   * Check deprecated parameters.
   * @param context step configuration context
   * @param parameter the parameter to check
   * @throws EoulsanException if the parameter is no more supported
   */
  static void checkDeprecatedParameter(final StepConfigurationContext context,
      final Parameter parameter) throws EoulsanException {

    if (parameter == null) {
      return;
    }

    switch (parameter.getName()) {

    case "lengthThreshold":
      Modules.renamedParameter(context, parameter, "trim.length.threshold",
          true);

    case "qualityThreshold":
      Modules.renamedParameter(context, parameter, "quality.threshold", true);

    case "pairend.accept.pairend":
      Modules.renamedParameter(context, parameter,
          "pairedend.accept.paired.end", true);

    case "pairend.accept.singlend":
      Modules.renamedParameter(context, parameter,
          "pairedend.accept.single.end", true);

    case "trim.length.threshold":
      Modules.renamedParameter(context, parameter,
          "trimpolynend\" and \"length");
      break;

    default:
      break;
    }
  }

  /**
   * Get the ReadFilter object.
   * @param incrementer incrementer to use
   * @param counterGroup counter group for the incrementer
   * @return a new ReadFilter object
   * @throws EoulsanException if an error occurs while initialize one of the
   *           filter
   */
  protected MultiReadFilter getReadFilter(final ReporterIncrementer incrementer,
      final String counterGroup) throws EoulsanException {

    // As filters are not thread safe, create a new MultiReadFilterBuilder
    // with a new instance of each filter
    return new MultiReadFilterBuilder(this.readsFiltersParameters)
        .getReadFilter(incrementer, counterGroup);
  }

}
