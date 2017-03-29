/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.name.IHasDisplayName;

/**
 * Defines the type of REST responses to be returned. Either PEPPOL (using
 * objects in namespace http://busdox.org/serviceMetadata/publishing/1.0/) or
 * BDXR (using objects in namespace
 * http://docs.oasis-open.org/bdxr/ns/SMP/2016/05)
 *
 * @author Philip Helger
 */
public enum ESMPRESTType implements IHasID <String>, IHasDisplayName
{
  PEPPOL ("peppol", "PEPPOL"),
  BDXR ("bdxr", "OASIS BDXR");

  private final String m_sID;
  private final String m_sDisplayName;

  private ESMPRESTType (@Nonnull @Nonempty final String sID, @Nonnull @Nonempty final String sDisplayName)
  {
    m_sID = sID;
    m_sDisplayName = sDisplayName;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  @Nonempty
  public String getDisplayName ()
  {
    return m_sDisplayName;
  }

  /**
   * @return <code>true</code> if this REST type supports retrieving the
   *         complete service group
   */
  public boolean isCompleteServiceGroupSupported ()
  {
    return this == PEPPOL;
  }

  public boolean isBDXR ()
  {
    return this == BDXR;
  }

  @Nullable
  public static ESMPRESTType getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESMPRESTType.class, sID);
  }

  @Nullable
  public static ESMPRESTType getFromIDOrDefault (@Nullable final String sID, @Nullable final ESMPRESTType eDefault)
  {
    return EnumHelper.getFromIDOrDefault (ESMPRESTType.class, sID, eDefault);
  }
}
