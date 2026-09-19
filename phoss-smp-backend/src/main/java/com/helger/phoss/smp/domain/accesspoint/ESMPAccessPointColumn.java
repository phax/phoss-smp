/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.accesspoint;

import java.util.Comparator;
import java.util.function.Function;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.compare.ESortOrder;
import com.helger.base.lang.EnumHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.smp.domain.ISMPTableColumn;
import com.helger.photon.core.paging.TableColumnHelper;

/**
 * The sortable and searchable columns of an {@link ISMPAccessPoint}. This is the basis for the
 * server side paging of the Access Point list in the UI.
 *
 * @author Philip Helger
 * @since 8.4.4
 */
public enum ESMPAccessPointColumn implements ISMPTableColumn <ISMPAccessPoint>
{
  /** The unique name of the Access Point */
  NAME ("name",
        new String [] { "name" },
        new String [] { "name" },
        true,
        true,
        ESortOrder.ASCENDING,
        ISMPAccessPoint::getName),
  /** The endpoint reference URL of the Access Point */
  ENDPOINT_REFERENCE ("endpointreference",
                      new String [] { "endpointReference" },
                      new String [] { "endpointreference" },
                      true,
                      true,
                      null,
                      ISMPAccessPoint::getEndpointReference);

  private final String m_sID;
  private final String [] m_aSQLColumnNames;
  private final String [] m_aMongoFieldNames;
  private final ESortOrder m_eDefaultSortOrder;
  private final Comparator <ISMPAccessPoint> m_aComparator;
  private final Function <ISMPAccessPoint, String> m_aSearchValueProvider;

  ESMPAccessPointColumn (@NonNull @Nonempty final String sID,
                         final String @Nullable [] aSQLColumnNames,
                         final String @Nullable [] aMongoFieldNames,
                         final boolean bSortable,
                         final boolean bSearchable,
                         @Nullable final ESortOrder eDefaultSortOrder,
                         @NonNull final Function <ISMPAccessPoint, String> aValueProvider)
  {
    m_sID = sID;
    m_aSQLColumnNames = aSQLColumnNames;
    m_aMongoFieldNames = aMongoFieldNames;
    m_eDefaultSortOrder = eDefaultSortOrder;
    m_aComparator = bSortable ? TableColumnHelper.createComparator (aValueProvider) : null;
    m_aSearchValueProvider = bSearchable ? aValueProvider : null;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nullable
  @ReturnsMutableCopy
  public ICommonsList <String> getAllSQLColumnNames ()
  {
    return m_aSQLColumnNames == null ? null : new CommonsArrayList <> (m_aSQLColumnNames);
  }

  @Nullable
  @ReturnsMutableCopy
  public ICommonsList <String> getAllMongoFieldNames ()
  {
    return m_aMongoFieldNames == null ? null : new CommonsArrayList <> (m_aMongoFieldNames);
  }

  @Nullable
  public Function <ISMPAccessPoint, String> getSearchValueProvider ()
  {
    return m_aSearchValueProvider;
  }

  @Nullable
  public Comparator <ISMPAccessPoint> getComparator ()
  {
    return m_aComparator;
  }

  @Nullable
  public ESortOrder getDefaultSortOrder ()
  {
    return m_eDefaultSortOrder;
  }

  @Nullable
  public static ESMPAccessPointColumn getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESMPAccessPointColumn.class, sID);
  }
}
