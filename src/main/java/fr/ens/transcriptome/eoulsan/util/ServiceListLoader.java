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

package fr.ens.transcriptome.eoulsan.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

/**
 * This class allow to load the list of available resources for a service
 * @since 1.2
 * @author Laurent Jourdren
 */
public class ServiceListLoader {

  private static final String PREFIX = "META-INF/services/";

  private final String serviceName;
  private final ClassLoader loader;

  /**
   * Get the list of available services.
   * @return a list with the names of the available services
   * @throws IOException if an error occurs while reading the list of services
   */
  private List<String> getServiceList() throws IOException {

    final String fullName = PREFIX + this.serviceName;
    final Enumeration<URL> urls;

    // Get the list of urls to the ressources files
    if (this.loader == null)
      urls = ClassLoader.getSystemResources(fullName);
    else
      urls = this.loader.getResources(fullName);

    // Parse the URLs files
    final List<String> result = Utils.newArrayList();
    for (URL url : Utils.newIterable(urls))
      parse(url, result);

    return result;
  }

  /**
   * Parse a resource list.
   * @param url URL of the resource list
   * @param result the result object
   * @throws IOException if an error occurs while reading the list
   */
  private void parse(final URL url, final List<String> result)
      throws IOException {

    final InputStream is = url.openStream();
    BufferedReader reader = FileUtils.createBufferedReader(is);

    String line;
    while ((line = reader.readLine()) != null) {

      final String trimLine = line.trim();
      if ("".equals(trimLine) || trimLine.startsWith("#"))
        continue;

      result.add(trimLine);
    }

    is.close();
  }

  //
  // Static methods
  //

  /**
   * Get the list of available services.
   * @param serviceName name of the service
   * @throws IOException if an error occurs while reading the resources
   */
  public static final List<String> load(final String serviceName)
      throws IOException {
    return load(serviceName, null);
  }

  /**
   * Get the list of available services.
   * @param serviceName name of the service
   * @param loader ClassLoader to use to read resources
   * @throws IOException if an error occurs while reading the resources
   */
  public static final List<String> load(final String serviceName,
      final ClassLoader loader) throws IOException {

    return new ServiceListLoader(serviceName, loader).getServiceList();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param serviceName name of the service
   * @param loader class loader to use to load resource files
   */
  private ServiceListLoader(final String serviceName, final ClassLoader loader) {

    if (serviceName == null)
      throw new NullPointerException("The service name is null");

    this.loader = loader == null ? this.getClass().getClassLoader() : loader;

    this.serviceName = serviceName;
  }

}