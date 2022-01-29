/*
 * Copyright (C) 2014-2022 Philip Helger and contributors
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
package com.helger.phoss.smp.exchange;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsIterable;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;

@NotThreadSafe
public final class ServiceGroupImportData
{
  private final ICommonsList <ISMPServiceInformation> m_aServiceInfos = new CommonsArrayList <> ();
  private final ICommonsList <ISMPRedirect> m_aRedirects = new CommonsArrayList <> ();

  public void addServiceInfo (@Nonnull final ISMPServiceInformation aServiceInfo)
  {
    m_aServiceInfos.add (aServiceInfo);
  }

  @Nonnull
  public ICommonsIterable <ISMPServiceInformation> getServiceInfo ()
  {
    return m_aServiceInfos;
  }

  public void addRedirect (@Nonnull final ISMPRedirect aRedirect)
  {
    m_aRedirects.add (aRedirect);
  }

  @Nonnull
  public ICommonsIterable <ISMPRedirect> getRedirects ()
  {
    return m_aRedirects;
  }
}
