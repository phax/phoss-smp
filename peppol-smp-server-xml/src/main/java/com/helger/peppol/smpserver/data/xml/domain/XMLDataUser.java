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
package com.helger.peppol.smpserver.data.xml.domain;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.peppol.smpserver.domain.user.ISMPUser;
import com.helger.photon.security.user.IUser;

/**
 * An implementation of {@link ISMPUser} on top of the ph-oton {@link IUser}.
 * 
 * @author Philip Helger
 */
public class XMLDataUser implements ISMPUser
{
  private final IUser m_aUser;
  private final String m_sName;

  public XMLDataUser (@Nonnull final IUser aUser)
  {
    m_aUser = ValueEnforcer.notNull (aUser, "User");
    m_sName = aUser.getLoginName () + " (" + aUser.getDisplayName () + ")";
  }

  @Nonnull
  public IUser getUser ()
  {
    return m_aUser;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_aUser.getID ();
  }

  @Nonnull
  @Nonempty
  public String getUserName ()
  {
    return m_sName;
  }
}
