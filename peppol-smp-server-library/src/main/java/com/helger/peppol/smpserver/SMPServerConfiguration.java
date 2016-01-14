/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.smpserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.system.SystemProperties;
import com.helger.peppol.utils.ConfigFile;

/**
 * The central configuration for the SMP server. This class manages the content
 * of the "smp-server.properties" file. The order of the properties file
 * resolving is as follows:
 * <ol>
 * <li>Check for the value of the system property
 * <code>smp.server.properties.path</code></li>
 * <li>The filename <code>private-smp-server.properties</code> in the root of
 * the classpath</li>
 * <li>The filename <code>smp-server.properties</code> in the root of the
 * classpath</li>
 * </ol>
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class SMPServerConfiguration
{
  /**
   * The name of the system property which points to the smp-server.properties
   * files
   */
  public static final String SYSTEM_PROPERTY_SMP_SERVER_PROPERTIES_PATH = "smp.server.properties.path";
  public static final String PATH_PRIVATE_SMP_SERVER_PROPERTIES = "private-smp-server.properties";
  public static final String PATH_SMP_SERVER_PROPERTIES = "smp-server.properties";

  private static final Logger s_aLogger = LoggerFactory.getLogger (SMPServerConfiguration.class);
  private static final ReadWriteLock s_aRWLock = new ReentrantReadWriteLock ();
  @GuardedBy ("s_aRWLock")
  private static ConfigFile s_aConfigFile;

  static
  {
    reloadConfiguration ();
  }

  /**
   * Reload the configuration file. It checks if the system property
   * {@link #SYSTEM_PROPERTY_SMP_SERVER_PROPERTIES_PATH} is present and if so,
   * tries it first, than {@link #PATH_PRIVATE_SMP_SERVER_PROPERTIES} is checked
   * and finally the {@link #PATH_SMP_SERVER_PROPERTIES} path is checked.
   *
   * @return {@link ESuccess}
   */
  @Nonnull
  public static ESuccess reloadConfiguration ()
  {
    final List <String> aFilePaths = new ArrayList <> ();
    // Check if the system property is present
    final String sPropertyPath = SystemProperties.getPropertyValue (SYSTEM_PROPERTY_SMP_SERVER_PROPERTIES_PATH);
    if (StringHelper.hasText (sPropertyPath))
      aFilePaths.add (sPropertyPath);

    // Use the default paths
    aFilePaths.add (PATH_PRIVATE_SMP_SERVER_PROPERTIES);
    aFilePaths.add (PATH_SMP_SERVER_PROPERTIES);

    s_aRWLock.writeLock ().lock ();
    try
    {
      s_aConfigFile = new ConfigFile (ArrayHelper.newArray (aFilePaths, String.class));
      if (!s_aConfigFile.isRead ())
      {
        s_aLogger.warn ("Failed to read smp-server.properties from any of the paths: " + aFilePaths);
        return ESuccess.FAILURE;
      }

      s_aLogger.info ("Read smp-server.properties from " + s_aConfigFile.getReadResource ().getPath ());
      return ESuccess.SUCCESS;
    }
    finally
    {
      s_aRWLock.writeLock ().unlock ();
    }
  }

  private SMPServerConfiguration ()
  {}

  /**
   * @return The configuration file. Never <code>null</code>.
   */
  @Nonnull
  public static ConfigFile getConfigFile ()
  {
    s_aRWLock.readLock ().lock ();
    try
    {
      return s_aConfigFile;
    }
    finally
    {
      s_aRWLock.readLock ().unlock ();
    }
  }

  /**
   * @return The backend to be used. Depends on the different possible
   *         implementations. Should not be <code>null</code>. Property
   *         <code>smp.backend</code>.
   */
  @Nullable
  public static String getBackend ()
  {
    return getConfigFile ().getString ("smp.backend");
  }

  /**
   * @return The path to the keystore. May be a classpath or an absolute file
   *         path. Property <code>smp.keystore.path</code>.
   */
  @Nullable
  public static String getKeystorePath ()
  {
    return getConfigFile ().getString ("smp.keystore.path");
  }

  /**
   * @return The password required to open the keystore. Property
   *         <code>smp.keystore.password</code>.
   */
  @Nullable
  public static String getKeystorePassword ()
  {
    return getConfigFile ().getString ("smp.keystore.password");
  }

  /**
   * @return The alias of the SMP key in the keystore. Property
   *         <code>smp.keystore.key.alias</code>.
   */
  @Nullable
  public static String getKeystoreKeyAlias ()
  {
    return getConfigFile ().getString ("smp.keystore.key.alias");
  }

  /**
   * @return The password used to access the private key. May be different than
   *         the password to the overall keystore. Property
   *         <code>smp.keystore.key.password</code>.
   */
  @Nullable
  public static char [] getKeystoreKeyPassword ()
  {
    return getConfigFile ().getCharArray ("smp.keystore.key.password");
  }

  /**
   * @return <code>true</code> if all paths should be forced to the ROOT ("/")
   *         context, <code>false</code> if the context should remain as it is.
   *         Property <code>smp.forceroot</code>.
   */
  public static boolean isForceRoot ()
  {
    return getConfigFile ().getBoolean ("smp.forceroot", false);
  }

  /**
   * @return The server URL that should be used to create absolute URLs inside
   *         the application. This may be helpful when running on a proxied
   *         Tomcat behind a web server. Property <code>smp.publicurl</code>.
   */
  @Nullable
  public static String getPublicServerURL ()
  {
    return getConfigFile ().getString ("smp.publicurl");
  }

  /**
   * Check if the writable parts of the REST API are disabled. If this is the
   * case, only the read-only part of the API can be used. The writable REST API
   * will return an HTTP 404 error.
   *
   * @return <code>true</code> if it is disabled, <code>false</code> if it is
   *         enabled. By the default the writable API is enabled. Property
   *         <code>smp.rest.writableapi.disabled</code>.
   */
  public static boolean isRESTWritableAPIDisabled ()
  {
    return getConfigFile ().getBoolean ("smp.rest.writableapi.disabled", false);
  }

  /**
   * @return <code>true</code> if the SML connection is active,
   *         <code>false</code> if not. Property <code>sml.active</code>.
   */
  public static boolean isWriteToSML ()
  {
    return getConfigFile ().getBoolean ("sml.active", false);
  }

  /**
   * @return The SML URL to use. Only relevant when {@link #isWriteToSML()} is
   *         <code>true</code>. Property <code>sml.url</code>.
   */
  @Nullable
  public static String getSMLURL ()
  {
    return getConfigFile ().getString ("sml.url");
  }

  /**
   * @return The SMP-ID to be used in the SML. Only relevant when
   *         {@link #isWriteToSML()} is <code>true</code>. Property
   *         <code>sml.smpid</code>.
   */
  @Nullable
  public static String getSMLSMPID ()
  {
    return getConfigFile ().getString ("sml.smpid");
  }
}
