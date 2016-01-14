/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.peppol.smpserver.data.xml.mgr;

import javax.annotation.Nonnull;

import com.helger.commons.string.ToStringGenerator;
import com.helger.peppol.smpserver.domain.ISMPManagerProvider;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.photon.basic.app.dao.impl.DAOException;

/**
 * {@link ISMPManagerProvider} implementation for this backend.
 *
 * @author Philip Helger
 */
public final class XMLManagerProvider implements ISMPManagerProvider
{
  public static final String SMP_SERVICE_GROUP_XML = "smp-servicegroup.xml";
  public static final String SMP_REDIRECT_XML = "smp-redirect.xml";
  public static final String SMP_SERVICE_INFORMATION_XML = "smp-serviceinformation.xml";

  public XMLManagerProvider ()
  {}

  @Nonnull
  public ISMPUserManager createUserMgr ()
  {
    return new XMLUserManager ();
  }

  @Nonnull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    try
    {
      return new XMLServiceGroupManager (SMP_SERVICE_GROUP_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  @Nonnull
  public ISMPRedirectManager createRedirectMgr ()
  {
    try
    {
      return new XMLRedirectManager (SMP_REDIRECT_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  @Nonnull
  public ISMPServiceInformationManager createServiceInformationMgr ()
  {
    try
    {
      return new XMLServiceInformationManager (SMP_SERVICE_INFORMATION_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).toString ();
  }
}
