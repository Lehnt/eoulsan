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

package fr.ens.transcriptome.eoulsan.core;

import java.io.Serializable;

import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.io.CompressionType;

public class SimpleOutputPort extends AbstractPort implements OutputPort,
    Serializable {

  private static final long serialVersionUID = 3565485272173523695L;

  private final CompressionType compression;

  @Override
  public CompressionType getCompression() {

    return this.compression;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param name name of the port
   * @param format format of the port
   */
  SimpleOutputPort(final String name, final DataFormat format) {

    this(name, format, null);
  }

  /**
   * Constructor.
   * @param name name of the port
   * @param format format of the port
   * @param compression compression of the output
   */
  protected SimpleOutputPort(final String name, final DataFormat format,
      final CompressionType compression) {

    // Set the name and the format
    super(name, format);

    // Set the compression
    if (compression == null)
      this.compression = CompressionType.NONE;
    else
      this.compression = compression;
  }

  /**
   * Constructor.
   * @param outputPort output port to clone
   */
  SimpleOutputPort(final OutputPort outputPort) {

    this(outputPort.getName(), outputPort.getFormat(), outputPort
        .getCompression());
  }

}