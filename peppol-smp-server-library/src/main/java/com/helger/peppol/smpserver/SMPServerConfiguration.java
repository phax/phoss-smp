/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.peppol.utils.ConfigFile;

/**
 * The central configuration for the SMP server. This class manages the content
 * of the "smp-server.properties" file.
 *
 * @author Philip Helger
 */
@Immutable
public final class SMPServerConfiguration
{
  private static final ConfigFile s_aConfigFile = new ConfigFile ("private-smp-server.properties",
                                                                  "smp-server.properties");

  private SMPServerConfiguration ()
  {}

  @Nonnull
  public static ConfigFile getConfigFile ()
  {
    return s_aConfigFile;
  }

  /**
   * @return The backend to be used. Depends on the different possible
   *         implementations. Should not be <code>null</code>.
   */
  @Nullable
  public static String getBackend ()
  {
    return s_aConfigFile.getString ("smp.backend");
  }

  @Nullable
  public static String getKeystorePath ()
  {
    return s_aConfigFile.getString ("smp.keystore.path");
  }

  @Nullable
  public static String getKeystorePassword ()
  {
    return s_aConfigFile.getString ("smp.keystore.password");
  }

  @Nullable
  public static String getKeystoreKeyAlias ()
  {
    return s_aConfigFile.getString ("smp.keystore.key.alias");
  }

  @Nullable
  public static char [] getKeystoreKeyPassword ()
  {
    return s_aConfigFile.getCharArray ("smp.keystore.key.password");
  }

  /**
   * @return <code>true</code> if all paths should be forced to the ROOT ("/")
   *         context, <code>false</code> if the context should remain as it is.
   */
  public static boolean isForceRoot ()
  {
    return s_aConfigFile.getBoolean ("smp.forceroot", false);
  }

  /**
   * @return <code>true</code> if the SML connection is active,
   *         <code>false</code> if not.
   */
  public static boolean isWriteToSML ()
  {
    return s_aConfigFile.getBoolean ("sml.active", false);
  }

  /**
   * @return The SML URL to use. Only relevant when {@link #isWriteToSML()} is
   *         <code>true</code>.
   */
  @Nullable
  public static String getSMLURL ()
  {
    return s_aConfigFile.getString ("sml.url");
  }

  /**
   * @return The SMP-ID to be used in the SML. Only relevant when
   *         {@link #isWriteToSML()} is <code>true</code>.
   */
  @Nullable
  public static String getSMLSMPID ()
  {
    return s_aConfigFile.getString ("sml.smpid");
  }
}
