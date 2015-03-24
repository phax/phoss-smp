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
package com.helger.peppol.smpserver.smlhook;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.lang.GenericReflection;
import com.helger.peppol.smpserver.CSMPServer;

/**
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Immutable
public final class RegistrationHookFactory
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (RegistrationHookFactory.class);
  private static final String CONFIG_REGISTRATION_HOOK_CLASS = "registrationHook.class";

  private RegistrationHookFactory ()
  {}

  /**
   * Create a new instance every time this method is invoked.
   *
   * @return A new instance of {@link IRegistrationHook} according to the
   *         configuration file.
   * @throws IllegalStateException
   *         If the class could not be instantiated
   */
  @Nonnull
  public static IRegistrationHook createInstance ()
  {
    final String sRegHookName = CSMPServer.getConfigFile ().getString (CONFIG_REGISTRATION_HOOK_CLASS);
    final IRegistrationHook aHook = GenericReflection.newInstance (sRegHookName, IRegistrationHook.class);
    if (aHook == null)
      throw new IllegalStateException ("Failed to create registration hook instance from class '" + sRegHookName + "'");
    s_aLogger.info ("Registration hook class '" + sRegHookName + "' instantiated");
    return aHook;
  }
}
