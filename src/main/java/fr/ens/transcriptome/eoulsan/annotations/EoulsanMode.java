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

package fr.ens.transcriptome.eoulsan.annotations;

import java.lang.annotation.Annotation;

/**
 * This class define an enum for the mode of Eoulsan.
 * @author Laurent Jourdren
 * @since 1.3
 */
public enum EoulsanMode {

  NONE, LOCAL_ONLY, HADOOP_COMPATIBLE, HADOOP_ONLY;

  /**
   * Get the Eoulsan annotation class that corresponds to the Eoulsan mode.
   * @return an annotation class
   */
  public Class<? extends Annotation> getAnnotationClass() {

    switch (this) {

    case LOCAL_ONLY:
      return LocalOnly.class;

    case HADOOP_COMPATIBLE:
      return HadoopCompatible.class;

    case HADOOP_ONLY:
      return HadoopOnly.class;

    case NONE:
    default:
      return null;

    }
  }

  /**
   * Test if the mode is compatible with local mode.
   * @return true if the mode is compatible with local mode
   */
  public boolean isLocalCompatible() {

    switch (this) {

    case LOCAL_ONLY:
    case HADOOP_COMPATIBLE:
      return true;

    case HADOOP_ONLY:
    case NONE:
    default:
      return false;
    }
  }

  /**
   * Test if the mode is compatible with Hadoop mode.
   * @return true if the mode is compatible with Hadoop mode
   */
  public boolean isHadoopCompatible() {

    switch (this) {

    case HADOOP_COMPATIBLE:
    case HADOOP_ONLY:
      return true;

    case LOCAL_ONLY:
    case NONE:
    default:
      return false;
    }
  }

  //
  // Static methods
  //

  /**
   * Check that annotation of a class is compatible with the Eoulsan mode (local
   * or Hadoop).
   * @param clazz class to test
   * @param hadoopMode Hadoop mode
   * @return true if the annotation of the class is compatible with the Eoulsan
   *         mode
   */
  public static boolean accept(Class<?> clazz, final boolean hadoopMode) {

    if (clazz == null)
      return false;

    final EoulsanMode mode = getEoulsanMode(clazz);

    switch (mode) {

    case LOCAL_ONLY:
      return hadoopMode == false;

    case HADOOP_COMPATIBLE:
      return true;

    case HADOOP_ONLY:
      return hadoopMode == true;

    case NONE:
    default:
      return false;

    }
  }

  /**
   * Get the Eoulsan mode of a class.
   * @param clazz class to test
   * @return an EoulsanMode object
   */
  public static EoulsanMode getEoulsanMode(final Class<?> clazz) {

    if (clazz == null)
      return null;

    EoulsanMode result = null;

    for (EoulsanMode mode : EoulsanMode.values()) {

      final Class<? extends Annotation> annotation = mode.getAnnotationClass();
      if (annotation != null && clazz.getAnnotation(annotation) != null) {

        if (result != null)
          throw new IllegalStateException(
              "A class can not have more than one Eoulsan mode: "
                  + clazz.getName());

        result = mode;
      }
    }

    if (result == null)
      return NONE;

    return result;
  }

}
