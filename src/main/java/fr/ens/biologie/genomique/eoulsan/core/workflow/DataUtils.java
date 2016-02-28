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

package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.eoulsan.core.workflow.FileNaming.toValidName;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;

import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.data.DataMetadata;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.design.Sample;

/**
 * This class define an utility on data object.
 * @since 2.0
 * @author Laurent Jourdren
 */
public final class DataUtils {

  /**
   * Change the DataFile in a Data object
   * @param data Data object to modify
   * @param dataFile new DataFile
   */
  public static void setDataFile(final Data data, final DataFile dataFile) {

    Preconditions.checkNotNull(data, "data cannot be null");

    if (data.isList()) {
      throw new IllegalArgumentException(
          "data list are not handled by this method");
    }

    final DataElement modifiableData = (DataElement) data;
    modifiableData.setDataFile(dataFile);
  }

  /**
   * Change the DataFile in a Data object
   * @param data Data object to modify
   * @param fileIndex file index
   * @param dataFile new DataFile
   */
  public static void setDataFile(final Data data, final int fileIndex,
      final DataFile dataFile) {

    checkNotNull(data, "data argument cannot be null");

    if (data.isList()) {
      throw new IllegalArgumentException(
          "data list are not handled by this method");
    }

    final DataElement modifiableData = (DataElement) data;
    modifiableData.setDataFile(fileIndex, dataFile);
  }

  /**
   * Change the DataFiles in a Data object
   * @param data Data object to modify
   * @param dataFiles DataFiles to set
   */
  public static void setDataFiles(final Data data,
      final List<DataFile> dataFiles) {

    checkNotNull(data, "data argument cannot be null");

    if (data.isList()) {
      throw new IllegalArgumentException(
          "data list are not handled by this method");
    }

    final DataElement modifiableData = (DataElement) data;
    modifiableData.setDataFiles(dataFiles);
  }

  /**
   * Get the list of the DataFile objects in a Data object.
   * @param data data object
   * @return a list of DataFile objects
   */
  public static final List<DataFile> getDataFiles(final Data data) {

    checkNotNull(data, "data argument cannot be null");

    if (data.isList()) {
      return Collections.emptyList();
    }

    return ((DataElement) data).getDataFiles();
  }

  /**
   * Set the metadata of a data object from the information of a Sample object
   * from a Design.
   * @param data the data object
   * @param sample the sample
   */
  public static final void setDataMetaData(final Data data,
      final Sample sample) {

    checkNotNull(data, "data argument cannot be null");
    checkNotNull(sample, "sample argument cannot be null");
    checkArgument(
        data.getName().equals(toValidName(sample.getId())),
        "The sample name ("
            + sample.getId() + ") does not match with data id ("
            + data.getName() + ")");

    // Do no set metadata on a data list
    if (data.isList()) {
      return;
    }

    //
    // Set Sample metadata
    //

    // Get the fields to not use (fields related to files)
    final Set<String> sampleMetadataKeysToNotUse = new HashSet<>();
    for (DataFormat format : DataFormatRegistry.getInstance().getAllFormats()) {
      if (format.getSampleMetadataKeyName() != null) {
        sampleMetadataKeysToNotUse.add(format.getSampleMetadataKeyName());
      }
    }

    // Get the data metadata object
    final SimpleDataMetadata dataMetadata =
        (SimpleDataMetadata) data.getMetadata();

    // Set the original sample name and sample id in the metadata
    dataMetadata.setSampleName(sample);
    dataMetadata.setSampleNumber(sample.getNumber());

    // TODO update reference to design data for data metadata

    // Set the other fields of the design file
    for (String key : sample.getMetadata().keySet()) {

      if (!sampleMetadataKeysToNotUse.contains(key)) {
        dataMetadata.setSampleMetadata(sample, key);
      }
    }

    //
    // Set Design metadata
    //

    // Get the fields to not use (fields related to files)
    final Set<String> designMetadataKeysToNotUse = new HashSet<>();
    for (DataFormat format : DataFormatRegistry.getInstance().getAllFormats()) {
      if (format.getSampleMetadataKeyName() != null) {
        designMetadataKeysToNotUse.add(format.getDesignMetadataKeyName());
      }
    }

    final Design design = sample.getDesign();
    for (String key : design.getMetadata().keySet()) {

      if (!designMetadataKeysToNotUse.contains(key)) {
        dataMetadata.setDesignMetadata(design, key);
      }
    }

    //
    // Set Experiment metadata
    //

    for (Experiment experiment : design.getExperimentsUsingASample(sample)) {

      for (String key : experiment.getMetadata().keySet()) {
        dataMetadata.setExperimentMetadata(experiment, key);
      }
    }

  }

  /**
   * Get the SimpleDataMetadata object from a DataMetadata object
   * @param metadata the metadata object
   * @return a SimpleDataMetadata object or null if SimpleDataMetaData cannot be
   *         find in metadata
   */
  static SimpleDataMetadata getSimpleMetadata(final DataMetadata metadata) {

    checkNotNull(metadata, "metadata argument cannot be null");

    DataMetadata md = metadata;

    // First get a metadata object that is not unmodifiable
    if (md instanceof UnmodifiableDataMetadata) {
      md = ((UnmodifiableDataMetadata) md).getMetaData();
    }

    if (md instanceof SimpleDataMetadata) {

      return (SimpleDataMetadata) md;
    }

    return null;
  }

  /**
   * Set the metadata of a data object from the information of another data
   * object.
   * @param data the data object
   * @param dataSourceOfMetadata data source of metadata
   */
  public static void setDataMetadata(final Data data,
      final Collection<Data> dataSourceOfMetadata) {

    checkNotNull(data, "data argument cannot be null");
    checkNotNull(dataSourceOfMetadata,
        "dataForMetaData argument cannot be null");

    for (Data d : dataSourceOfMetadata) {
      setDataMetadata(data, d);
    }
  }

  /**
   * Set the metadata of a data object from the information of another data
   * object.
   * @param data the data object
   * @param dataSourceOfMetadata data source of metadata
   */
  public static void setDataMetadata(final Data data,
      final Data dataSourceOfMetadata) {

    checkNotNull(data, "data argument cannot be null");
    checkNotNull(dataSourceOfMetadata,
        "dataForMetaData argument cannot be null");

    // If data is a list do nothing
    if (data.isList()) {
      return;
    }

    final DataMetadata metadata = data.getMetadata();

    if (dataSourceOfMetadata.isList()) {

      for (Data d : dataSourceOfMetadata.getListElements()) {
        metadata.set(d.getMetadata());
      }
    } else {
      metadata.set(dataSourceOfMetadata.getMetadata());
    }
  }

  //
  // Private constructor
  //

  private DataUtils() {
  }
}