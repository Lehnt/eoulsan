package fr.ens.transcriptome.eoulsan.steps.mapping.hadoop;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileReader;
import htsjdk.samtools.SamFileHeaderMerger;
import htsjdk.samtools.util.BlockCompressedStreamConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.seqdoop.hadoop_bam.SAMFormat;
import org.seqdoop.hadoop_bam.cli.Utils;
import org.seqdoop.hadoop_bam.util.SAMOutputPreparer;

public class HadoopBamUtils {

  private static final String HEADERMERGER_SORTORDER_PROP =
      "hadoopbam.headermerger.sortorder";

  /**
   * Computes the merger of the SAM headers in the files listed in
   * HEADERMERGER_INPUTS_PROPERTY. The sort order of the result is set according
   * to the last call to setHeaderMergerSortOrder, or otherwise to "unsorted".
   * The result is cached locally to prevent it from being recomputed too often.
   */
  public static SamFileHeaderMerger getSAMHeaderMerger(Configuration conf)
      throws IOException {
    // TODO: it would be preferable to cache this beforehand instead of
    // having every task read the header block of every input file. But that
    // would be trickier, given that SamFileHeaderMerger isn't trivially
    // serializable.

    final List<SAMFileHeader> headers = new ArrayList<SAMFileHeader>();

    for (final String in : conf.getStrings(Utils.HEADERMERGER_INPUTS_PROPERTY)) {
      final Path p = new Path(in);

      final SAMFileReader r = new SAMFileReader(p.getFileSystem(conf).open(p));
      headers.add(r.getFileHeader());
      r.close();
    }

    final String orderStr = conf.get(HEADERMERGER_SORTORDER_PROP);
    final SAMFileHeader.SortOrder order =
        orderStr == null
            ? SAMFileHeader.SortOrder.unsorted : SAMFileHeader.SortOrder
                .valueOf(orderStr);

    return new SamFileHeaderMerger(order, headers, true);
  }

  /**
   * Merges the files in the given directory that have names given by
   * getMergeableWorkFile() into out in the given SAMFormat, using
   * getSAMHeaderMerger().getMergedHeader() as the header. Outputs progress
   * reports if commandName is non-null.
   */
  public static void mergeSAMInto(Path out, Path directory, String basePrefix,
      String basePostfix, SAMFormat format, Configuration conf,
      String commandName) throws IOException {

    final OutputStream outs = out.getFileSystem(conf).create(out);

    // First, place the SAM or BAM header.
    //
    // Don't use the returned stream, because we're concatenating directly
    // and don't want to apply another layer of compression to BAM.
    new SAMOutputPreparer().prepareForRecords(outs, format,
        getSAMHeaderMerger(conf).getMergedHeader());

    // Then, the actual SAM or BAM contents.
    mergeInto(outs, directory, basePrefix, basePostfix, conf, commandName);

    // And if BAM, the BGZF terminator.
    if (format == SAMFormat.BAM)
      outs.write(BlockCompressedStreamConstants.EMPTY_GZIP_BLOCK);

    outs.close();
  }

  /**
   * Merges the files in the given directory that have names given by
   * getMergeableWorkFile() into out. Outputs progress reports if commandName is
   * non-null.
   */
  public static void mergeInto(OutputStream out, Path directory,
      String basePrefix, String basePostfix, Configuration conf,
      String commandName) throws IOException {
    final FileSystem fs = directory.getFileSystem(conf);

    final FileStatus[] parts =
        fs.globStatus(new Path(directory, basePrefix
            + conf.get(Utils.WORK_FILENAME_PROPERTY) + basePostfix
            + "-[0-9][0-9][0-9][0-9][0-9][0-9]*"));

    int i = 0;

    for (final FileStatus part : parts) {

      final InputStream in = fs.open(part.getPath());
      IOUtils.copyBytes(in, out, conf, false);
      in.close();
    }

    for (final FileStatus part : parts) {
      fs.delete(part.getPath(), false);
    }
  }

}
