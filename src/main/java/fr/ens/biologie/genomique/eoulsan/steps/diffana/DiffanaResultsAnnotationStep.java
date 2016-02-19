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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.steps.diffana;

import static fr.ens.biologie.genomique.eoulsan.core.ParallelizationMode.OWN_PARALLELIZATION;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ADDITIONAL_ANNOTATION_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATED_EXPRESSION_RESULTS_ODS;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATED_EXPRESSION_RESULTS_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATED_EXPRESSION_RESULTS_XLSX;
import static fr.ens.biologie.genomique.eoulsan.translators.TranslatorUtils.loadTranslator;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.HadoopCompatible;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.ParallelizationMode;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.StepContext;
import fr.ens.biologie.genomique.eoulsan.core.StepResult;
import fr.ens.biologie.genomique.eoulsan.core.StepStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.steps.AbstractStep;
import fr.ens.biologie.genomique.eoulsan.steps.Steps;
import fr.ens.biologie.genomique.eoulsan.translators.Translator;
import fr.ens.biologie.genomique.eoulsan.translators.TranslatorUtils;
import fr.ens.biologie.genomique.eoulsan.translators.io.ODSTranslatorOutputFormat;
import fr.ens.biologie.genomique.eoulsan.translators.io.TSVTranslatorOutputFormat;
import fr.ens.biologie.genomique.eoulsan.translators.io.TranslatorOutputFormat;
import fr.ens.biologie.genomique.eoulsan.translators.io.XLSXTranslatorOutputFormat;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;
import fr.ens.biologie.genomique.eoulsan.util.Version;

/**
 * This class define a step that create annotated expression files in TSV, ODS
 * or XLSX format.
 * @since 2.0
 * @author Laurent Jourdren
 */
@HadoopCompatible
public class DiffanaResultsAnnotationStep extends AbstractStep {

  public static final String STEP_NAME = "diffanaresultsannotation";

  private static final DataFormat DEFAULT_FORMAT =
      ANNOTATED_EXPRESSION_RESULTS_TSV;

  private static final String DEFAULT_FILE_INPUT_GLOB_PATTERN = "diffana_*.tsv";

  private final Map<String, DataFormat> outputFormats = new HashMap<>();

  private PathMatcher pathMatcher;
  private String outputPrefix;
  private boolean useAdditionalAnnotationFile = true;

  //
  // Step methods
  //

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "This step add annotation to diffana files.";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    // Add the port for the additional annotation
    if (this.useAdditionalAnnotationFile) {
      return InputPortsBuilder.singleInputPort(ADDITIONAL_ANNOTATION_TSV);
    }

    return InputPortsBuilder.noInputPort();
  }

  @Override
  public OutputPorts getOutputPorts() {

    return OutputPortsBuilder.noOutputPort();
  }

  @Override
  public ParallelizationMode getParallelizationMode() {

    final Collection<DataFormat> formats = this.outputFormats.values();

    // XLSX and ODS file creation require lot of memory so multithreading is
    // disable to avoid out of memory
    if (formats.contains(ANNOTATED_EXPRESSION_RESULTS_ODS)
        || formats.contains(ANNOTATED_EXPRESSION_RESULTS_XLSX)) {
      return OWN_PARALLELIZATION;
    }

    // TSV creation can be multithreaded
    return ParallelizationMode.STANDARD;
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    String pattern = DEFAULT_FILE_INPUT_GLOB_PATTERN;
    this.outputPrefix = context.getCurrentStep().getId();

    for (final Parameter p : stepParameters) {

      switch (p.getName()) {

      case "annotationfile":
        Steps.removedParameter(context, p);
        break;

      case "use.additional.annotation.file":
        this.useAdditionalAnnotationFile = p.getBooleanValue();
        break;

      case "outputformat":
        Steps.renamedParameter(context, p, "output.format");
      case "output.format":

        // Set output format
        for (String format : Splitter.on(',').trimResults().omitEmptyStrings()
            .split(p.getValue())) {

          switch (format) {

          case "tsv":
            this.outputFormats.put(format, ANNOTATED_EXPRESSION_RESULTS_TSV);
            break;

          case "ods":
            this.outputFormats.put(format, ANNOTATED_EXPRESSION_RESULTS_ODS);
            break;

          case "xlsx":
            this.outputFormats.put(format, ANNOTATED_EXPRESSION_RESULTS_XLSX);
            break;

          default:
            throw new EoulsanException("Unknown output format: " + format);
          }
        }

        break;

      case "files":
        pattern = p.getStringValue();
        break;

      case "output.prefix":
        this.outputPrefix = p.getStringValue();
        break;

      default:
        // Unknown option
        Steps.unknownParameter(context, p);
        break;
      }
    }

    // Set the default format
    if (this.outputFormats.isEmpty()) {
      this.outputFormats.put(DEFAULT_FORMAT.getDefaultExtension().substring(1),
          DEFAULT_FORMAT);
    }

    // Set the PathMatcher
    this.pathMatcher =
        FileSystems.getDefault().getPathMatcher("glob:" + pattern);
  }

  @Override
  public StepResult execute(final StepContext context,
      final StepStatus status) {

    // Get hypertext links file
    final DataFile linksFile =
        TranslatorUtils.getLinksFileFromSettings(context.getSettings());

    // Load translator
    final Translator translator;

    try {

      if (this.useAdditionalAnnotationFile) {

        // If no annotation file parameter set
        Data additionalAnnotationData =
            context.getInputData(ADDITIONAL_ANNOTATION_TSV);

        // Create translator with additional annotation file
        translator =
            loadTranslator(additionalAnnotationData.getDataFile(), linksFile);

      } else {

        // Create translator without additional annotation file
        translator = TranslatorUtils.loadTranslator(linksFile);
      }

    } catch (IOException e) {
      return status.createStepResult(e);
    }

    // Description string
    final StringBuilder descriptionString = new StringBuilder();

    try {

      final DataFile outputDir = context.getOutputDirectory();
      final List<DataFile> files = outputDir.list();
      final List<DataFile> filesToConvert = new ArrayList<>();

      // Filter files to convert
      for (DataFile f : files) {
        if (this.pathMatcher.matches(new File(f.getName()).toPath())) {
          filesToConvert.add(f);
        }
      }

      // Annotate all selected files
      for (DataFile inFile : filesToConvert) {

        // For each formats
        for (Map.Entry<String, DataFormat> e : this.outputFormats.entrySet()) {

          // Get format
          final DataFormat format = e.getValue();

          final String prefix = this.outputPrefix
              + StringUtils.filenameWithoutExtension(inFile.getName());

          final TranslatorOutputFormat of;
          final DataFile outFile;

          if (format == ANNOTATED_EXPRESSION_RESULTS_XLSX) {

            // XLSX output
            outFile = new DataFile(outputDir, prefix
                + ANNOTATED_EXPRESSION_RESULTS_XLSX.getDefaultExtension());
            checkIfFileExists(outFile, context);
            of = new XLSXTranslatorOutputFormat(outFile.create());

          } else if (format == ANNOTATED_EXPRESSION_RESULTS_ODS) {

            // ODS output
            outFile = new DataFile(outputDir, prefix
                + ANNOTATED_EXPRESSION_RESULTS_ODS.getDefaultExtension());
            checkIfFileExists(outFile, context);
            of = new ODSTranslatorOutputFormat(outFile.create());

          } else {

            // TSV output
            outFile = new DataFile(outputDir, prefix
                + ANNOTATED_EXPRESSION_RESULTS_TSV.getDefaultExtension());
            checkIfFileExists(outFile, context);
            of = new TSVTranslatorOutputFormat(outFile.create());
          }

          TranslatorUtils.addTranslatorFields(inFile.open(), 0, translator, of);
          descriptionString.append("Convert ");
          descriptionString.append(inFile);
          descriptionString.append(" to ");
          descriptionString.append(outFile);
          descriptionString.append("\n");
        }
      }

    } catch (IOException e) {
      return status.createStepResult(e);
    }

    // Set the description of the context
    status.setDescription(descriptionString.toString());

    // Return the result
    return status.createStepResult();
  }

  /**
   * Check if the output file already exists.
   * @param file the output file
   * @param context the step context
   * @throws IOException if the the output file already exists
   */
  private static void checkIfFileExists(final DataFile file,
      final StepContext context) throws IOException {

    if (file.exists()) {
      throw new IOException("Output file of the \""
          + context.getCurrentStep().getId() + "\" already exists: " + file);
    }

  }

}
