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
package com.helger.phoss.smp.domain.serviceinfo;

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

/**
 * The sortable and searchable columns of an {@link ISMPServiceInformation}.
 *
 * @author Philip Helger
 * @since 8.2.1
 */
public enum ESMPServiceInformationColumn implements ISMPTableColumn <ISMPServiceInformation>
{
  /** The participant identifier of the owning service group */
  SERVICE_GROUP ("servicegroup",
                 new String [] { "sm.businessIdentifierScheme", "sm.businessIdentifier" },
                 new String [] { "sgid" },
                 true,
                 true,
                 ESortOrder.ASCENDING,
                 ISMPServiceInformation::getServiceGroupID),
  /** The document type identifier */
  DOCUMENT_TYPE_ID ("doctypeid",
                    new String [] { "sm.documentIdentifierScheme", "sm.documentIdentifier" },
                    new String [] { "doctypeid" },
                    true,
                    true,
                    ESortOrder.ASCENDING,
                    x -> x.getDocumentTypeIdentifier ().getURIEncoded ());

  private final String m_sID;
  private final String [] m_aSQLColumnNames;
  private final String [] m_aMongoFieldNames;
  private final boolean m_bSortable;
  private final boolean m_bSearchable;
  private final ESortOrder m_eDefaultSortOrder;
  private final Function <ISMPServiceInformation, String> m_aValueProvider;

  ESMPServiceInformationColumn (@NonNull @Nonempty final String sID,
                               final String @Nullable [] aSQLColumnNames,
                               final String @Nullable [] aMongoFieldNames,
                               final boolean bSortable,
                               final boolean bSearchable,
                               @Nullable final ESortOrder eDefaultSortOrder,
                               @NonNull final Function <ISMPServiceInformation, String> aValueProvider)
  {
    m_sID = sID;
    m_aSQLColumnNames = aSQLColumnNames;
    m_aMongoFieldNames = aMongoFieldNames;
    m_bSortable = bSortable;
    m_bSearchable = bSearchable;
    m_eDefaultSortOrder = eDefaultSortOrder;
    m_aValueProvider = aValueProvider;
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

  public boolean isSortable ()
  {
    return m_bSortable;
  }

  public boolean isSearchable ()
  {
    return m_bSearchable;
  }

  @Nullable
  public ESortOrder getDefaultSortOrder ()
  {
    return m_eDefaultSortOrder;
  }

  @NonNull
  public Function <ISMPServiceInformation, String> getValueProvider ()
  {
    return m_aValueProvider;
  }


  @Nullable
  public static ESMPServiceInformationColumn getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESMPServiceInformationColumn.class, sID);
  }
}
