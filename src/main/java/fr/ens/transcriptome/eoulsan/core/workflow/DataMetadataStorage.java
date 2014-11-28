package fr.ens.transcriptome.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This class define a storage for data metadata of all files generated by the
 * workflow.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DataMetadataStorage {

  private static final String METADATA_FILENAME = ".eoulsanmetadata";
  private static final String FIELD_SEPARATOR = "\t";

  private static DataMetadataStorage singleton;

  private final DataFile metadataFile;
  private final Map<String, Map<String, String>> metadata = new HashMap<>();

  /**
   * Set the metdata of a data from the metadata storage.
   * @param data the date which metadata must be set
   * @return true if the metadata for the data has been found in the metadata
   *         storage
   */
  public boolean loadMetadata(final Data data) {

    checkNotNull(data, "data argument cannot be null");

    final SimpleDataMetadata metadata =
        DataUtils.getSimpleMetadata(data.getMetadata());

    // Do nothing if metadata cannot be set
    if (metadata == null) {
      return false;
    }

    boolean result = false;
    final List<DataFile> files = DataUtils.getDataFiles(data);

    // For each file of the data
    for (DataFile file : files) {

      final String filename = file.getName();
      final Map<String, String> entries = this.metadata.get(filename);

      // Do nothing if any file is in registry
      if (entries != null) {

        // Set the values
        for (Map.Entry<String, String> e : entries.entrySet()) {
          metadata.setRaw(e.getKey(), e.getValue());
        }

        result = true;
      }
    }

    return result;
  }

  /**
   * Save metadata of a Data object.
   * @param data the data object
   */
  public void saveMetaData(final Data data) {

    checkNotNull(data, "data argument cannot be null");

    final SimpleDataMetadata metadata =
        DataUtils.getSimpleMetadata(data.getMetadata());

    // Do nothing if metadata cannot be set
    if (metadata == null) {
      return;
    }

    final List<DataFile> files = DataUtils.getDataFiles(data);

    // For each file of the data
    for (DataFile file : files) {

      final String filename = file.getName();
      final Map<String, String> newEntries = new HashMap<>();

      final StringBuilder sb = new StringBuilder();
      sb.append(filename);

      for (String key : metadata.keySet()) {

        final String value = metadata.getRaw(key);

        newEntries.put(key, value);

        sb.append(FIELD_SEPARATOR);
        sb.append(key);
        sb.append(FIELD_SEPARATOR);
        sb.append(value);
      }

      // If metadata for the file has changed
      if (!newEntries.equals(this.metadata.get(filename))) {

        // Save entries in memory
        this.metadata.put(filename, newEntries);

        // Save entries in the file
        try {
          writeMetadataEntry(sb.toString());
        } catch (EoulsanException e) {
          getLogger().warning(e.getMessage());
        }
      }

    }

  }

  //
  // Storage methods
  //

  /**
   * Load metadata.
   * @throws EoulsanException if an error occurs while reading metadata
   */
  private void loadMetaDataEntries() throws EoulsanException {

    // Test if storage exists
    if (!this.metadataFile.exists()) {
      return;
    }

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(this.metadataFile.open()))) {

      String line = null;

      while ((line = reader.readLine()) != null) {

        final String[] fields = line.split(FIELD_SEPARATOR);

        if (fields.length % 2 != 0) {

          final String filename = fields[0];
          final Map<String, String> entries = new HashMap<>();
          this.metadata.put(filename, entries);

          for (int i = 1; i < fields.length; i += 2) {
            entries.put(fields[i], fields[i + 1]);
          }
        }
      }

    } catch (IOException e) {
      throw new EoulsanException("Unable to read metadata: " + e.getMessage());
    }
  }

  private void writeMetadataEntry(final String s) throws EoulsanException {

    // Do nothing if the metadata storage is not on local file
    if (!this.metadataFile.isLocalFile()) {
      return;
    }

    try (PrintWriter out =
        new PrintWriter(new BufferedWriter(new FileWriter(
            this.metadataFile.toFile(), true)))) {

      // Write entry
      out.println(s);
    } catch (IOException e) {
      throw new EoulsanException("Unable to write metadata: " + e.getMessage());
    }
  }

  //
  // Static method
  //

  /**
   * Get the singleton.
   * @param metadataDir directory where store metadata
   * @return the DataMetadataStorage object
   */
  public static DataMetadataStorage getInstance(final DataFile metadataDir) {

    if (singleton == null) {

      singleton = new DataMetadataStorage(metadataDir);
    }

    return singleton;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param metadataDir directory where store metadata
   */
  private DataMetadataStorage(final DataFile metadataDir) {

    checkNotNull(metadataDir, "metadataDir argument cannot be null");

    // Set the metadata storage file
    this.metadataFile = new DataFile(metadataDir, METADATA_FILENAME);

    // Load metadata

    try {
      loadMetaDataEntries();
    } catch (EoulsanException e) {
      getLogger().warning(e.getMessage());
    }
  }

}
