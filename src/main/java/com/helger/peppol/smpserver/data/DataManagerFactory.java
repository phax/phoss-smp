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
package com.helger.peppol.smpserver.data;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotations.Nonempty;
import com.helger.commons.lang.GenericReflection;
import com.helger.peppol.smpserver.CSMPServer;

/**
 * Factory for creating new DataManagers. This implementation retrieves the name
 * of the data manager from a configuration file.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Immutable
public final class DataManagerFactory
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (DataManagerFactory.class);
  private static final String CONFIG_DATA_MANAGER_CLASS = "dataManager.class";
  private static final String s_sDataManagerClassName;
  private static final IDataManager s_aInstance;

  static
  {
    s_sDataManagerClassName = CSMPServer.getConfigFile ().getString (CONFIG_DATA_MANAGER_CLASS);
    final IDataManager ret = GenericReflection.newInstance (s_sDataManagerClassName, IDataManager.class);
    if (ret == null)
      throw new IllegalStateException ("Failed to instantiate IDataManager class '" + s_sDataManagerClassName + "'");
    s_aLogger.info ("IDataManager class '" + s_sDataManagerClassName + "' instantiated");
    s_aInstance = ret;
  }

  private DataManagerFactory ()
  {}

  /**
   * @return The name of the class used to instantiate the data manager. Neither
   *         <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public static String getDataManagerClassName ()
  {
    return s_sDataManagerClassName;
  }

  /**
   * @return The same instance over and over again. Never <code>null</code>.
   */
  @Nonnull
  public static IDataManager getInstance ()
  {
    return s_aInstance;
  }
}
