/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.mock;

import java.util.function.Predicate;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smpserver.domain.sml.ISMLInfoManager;

/**
 * Mock implementation of {@link ISMLInfoManager}.
 *
 * @author Philip Helger
 */
final class MockSMLInfoManager implements ISMLInfoManager
{
  public EChange updateSMLInfo (final String sSMLInfoID,
                                final String sDisplayName,
                                final String sDNSZone,
                                final String sManagementServiceURL,
                                final boolean bClientCertificateRequired)
  {
    throw new UnsupportedOperationException ();
  }

  public EChange removeSMLInfo (final String sSMLInfoID)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMLInfo getSMLInfoOfID (final String sID)
  {
    return null;
  }

  public ISMLInfo findFirst (final Predicate <? super ISMLInfo> aFilter)
  {
    return null;
  }

  public ICommonsList <ISMLInfo> getAllSMLInfos ()
  {
    throw new UnsupportedOperationException ();
  }

  public ISMLInfo createSMLInfo (final String sDisplayName,
                                 final String sDNSZone,
                                 final String sManagementServiceURL,
                                 final boolean bClientCertificateRequired)
  {
    throw new UnsupportedOperationException ();
  }

  public boolean containsSMLInfoWithID (final String sID)
  {
    return false;
  }
}
