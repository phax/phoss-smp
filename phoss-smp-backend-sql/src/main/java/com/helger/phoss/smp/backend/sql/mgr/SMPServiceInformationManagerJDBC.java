/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.sql.mgr;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.MustImplementEqualsAndHashcode;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.numeric.mutable.MutableBoolean;
import com.helger.base.state.EChange;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.wrapper.Wrapper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.collection.commons.ICommonsSet;
import com.helger.collection.paging.IPagingSpec;
import com.helger.db.api.helper.DBValueHelper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.db.jdbc.mgr.AbstractJDBCEnabledManager;
import com.helger.peppolid.CIdentifier;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.simple.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.peppolid.simple.process.SimpleProcessIdentifier;
import com.helger.phoss.smp.backend.sql.SMPJDBCQueryHelper;
import com.helger.phoss.smp.backend.sql.SMPJDBCQueryHelper.SearchCondition;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;
import com.helger.phoss.smp.domain.accesspoint.SMPAccessPoint;
import com.helger.phoss.smp.domain.serviceinfo.ESMPServiceInformationColumn;
import com.helger.phoss.smp.domain.serviceinfo.EndpointUsageInfo;
import com.helger.phoss.smp.domain.serviceinfo.IEndpointUsageInfo;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationCallback;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpointHelper;
import com.helger.phoss.smp.domain.serviceinfo.SMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformation;
import com.helger.phoss.smp.security.SMPCertificateHelper;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.core.paging.TableColumnHelper;

/**
 * A JDBC based implementation of the {@link ISMPServiceInformationManager} interface.
 *
 * @author Philip Helger
 * @since 5.3.0
 */
public final class SMPServiceInformationManagerJDBC extends AbstractJDBCEnabledManager implements
                                                    ISMPServiceInformationManager
{
  private static final ESMPServiceInformationColumn [] COLUMNS = ESMPServiceInformationColumn.values ();

  @MustImplementEqualsAndHashcode
  private static final class DocTypeAndExtension
  {
    final IDocumentTypeIdentifier m_aDocTypeID;
    final String m_sExt;

    public DocTypeAndExtension (@NonNull final IDocumentTypeIdentifier aDocTypeID, final String sExt)
    {
      m_aDocTypeID = aDocTypeID;
      m_sExt = sExt;
    }

    @Override
    public boolean equals (final Object o)
    {
      if (o == this)
        return true;
      if (o == null || !getClass ().equals (o.getClass ()))
        return false;
      final DocTypeAndExtension rhs = (DocTypeAndExtension) o;
      return m_aDocTypeID.equals (rhs.m_aDocTypeID) && EqualsHelper.equals (m_sExt, rhs.m_sExt);
    }

    @Override
    public int hashCode ()
    {
      return new HashCodeGenerator (this).append (m_aDocTypeID).append (m_sExt).getHashCode ();
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPServiceInformationManagerJDBC.class);

  private final String m_sTableNameSM;
  private final String m_sTableNameP;
  private final String m_sTableNameE;
  private final String m_sTableNameAP;
  private final ISMPAccessPointManager m_aAccessPointMgr;
  private final CallbackList <ISMPServiceInformationCallback> m_aCBs = new CallbackList <> ();

  /**
   * Constructor
   *
   * @param aDBExecSupplier
   *        The supplier for {@link DBExecutor} objects. May not be <code>null</code>.
   * @param sTableNamePrefix
   *        The table name prefix to be used. May not be <code>null</code>.
   * @param aAccessPointMgr
   *        The Access Point manager to use. May not be <code>null</code>.
   */
  public SMPServiceInformationManagerJDBC (@NonNull final Supplier <? extends DBExecutor> aDBExecSupplier,
                                           @NonNull final String sTableNamePrefix,
                                           @NonNull final ISMPAccessPointManager aAccessPointMgr)
  {
    super (aDBExecSupplier);
    ValueEnforcer.notNull (sTableNamePrefix, "TableNamePrefix");
    ValueEnforcer.notNull (aAccessPointMgr, "AccessPointMgr");
    m_sTableNameSM = sTableNamePrefix + "smp_service_metadata";
    m_sTableNameP = sTableNamePrefix + "smp_process";
    m_sTableNameE = sTableNamePrefix + "smp_endpoint";
    m_sTableNameAP = sTableNamePrefix + "smp_access_point";
    m_aAccessPointMgr = aAccessPointMgr;
  }

  @NonNull
  @ReturnsMutableObject
  public CallbackList <ISMPServiceInformationCallback> serviceInformationCallbacks ()
  {
    return m_aCBs;
  }

  /**
   * Create the Access Point object of an endpoint DB row.
   *
   * @param sAccessPointID
   *        The value of the <code>accessPointID</code> column. May be <code>null</code> for
   *        inconsistent data.
   * @param sEndpointReference
   *        The joined endpoint reference URL. May be <code>null</code>.
   * @param sCertificate
   *        The joined certificate. May be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  private static SMPAccessPoint _toAccessPoint (@Nullable final String sAccessPointID,
                                                @Nullable final String sEndpointReference,
                                                @Nullable final String sCertificate)
  {
    if (StringHelper.isEmpty (sAccessPointID))
    {
      // Should never happen - fallback to a detached Access Point
      LOGGER.warn ("Found an endpoint without a resolvable Access Point");
      return SMPAccessPoint.createDetached (sEndpointReference, sCertificate);
    }
    return new SMPAccessPoint (sAccessPointID, sEndpointReference, sCertificate);
  }

  @NonNull
  public ESuccess mergeSMPServiceInformation (@NonNull final ISMPServiceInformation aSMPServiceInformation)
  {
    ValueEnforcer.notNull (aSMPServiceInformation, "ServiceInformation");

    // Resolve and de-duplicate the Access Points of all endpoints
    SMPEndpointHelper.resolveAccessPoints (m_aAccessPointMgr, aSMPServiceInformation);

    final MutableBoolean aUpdated = new MutableBoolean (false);

    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction (() -> {
      // Simply delete the old one
      final EChange eDeleted = _deleteSMPServiceInformationNoCallback (aExecutor, aSMPServiceInformation);
      aUpdated.set (eDeleted.isChanged ());

      // Insert new processes
      final IParticipantIdentifier aPID = aSMPServiceInformation.getServiceGroupParticipantIdentifier ();
      final IDocumentTypeIdentifier aDocTypeID = aSMPServiceInformation.getDocumentTypeIdentifier ();

      aExecutor.insertOrUpdateOrDelete ("INSERT INTO " +
                                        m_sTableNameSM +
                                        " (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, extension) VALUES (?, ?, ?, ?, ?)",
                                        new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                   aPID.getValue (),
                                                                                   aDocTypeID.getScheme (),
                                                                                   aDocTypeID.getValue (),
                                                                                   aSMPServiceInformation.getExtensions ()
                                                                                                         .getExtensionsAsJsonString ()));

      for (final ISMPProcess aProcess : aSMPServiceInformation.getAllProcesses ())
      {
        final IProcessIdentifier aProcessID = aProcess.getProcessIdentifier ();
        aExecutor.insertOrUpdateOrDelete ("INSERT INTO " +
                                          m_sTableNameP +
                                          " (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, processIdentifierType, processIdentifier, extension) VALUES (?, ?, ?, ?, ?, ?, ?)",
                                          new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                     aPID.getValue (),
                                                                                     aDocTypeID.getScheme (),
                                                                                     aDocTypeID.getValue (),
                                                                                     aProcessID.getScheme (),
                                                                                     aProcessID.getValue (),
                                                                                     aProcess.getExtensions ()
                                                                                             .getExtensionsAsJsonString ()));
        // Insert new endpoints
        for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
        {
          aExecutor.insertOrUpdateOrDelete ("INSERT INTO " +
                                            m_sTableNameE +
                                            " (id, businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, processIdentifierType, processIdentifier," +
                                            " accessPointID, minimumAuthenticationLevel, requireBusinessLevelSignature, serviceActivationDate, serviceDescription, serviceExpirationDate, technicalContactUrl, technicalInformationUrl, transportProfile," +
                                            " extension) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                            new ConstantPreparedStatementDataProvider (aEndpoint.getID (),
                                                                                       aPID.getScheme (),
                                                                                       aPID.getValue (),
                                                                                       aDocTypeID.getScheme (),
                                                                                       aDocTypeID.getValue (),
                                                                                       aProcessID.getScheme (),
                                                                                       aProcessID.getValue (),
                                                                                       aEndpoint.getAccessPointID (),
                                                                                       aEndpoint.getMinimumAuthenticationLevel (),
                                                                                       Boolean.valueOf (aEndpoint.isRequireBusinessLevelSignature ()),
                                                                                       DBValueHelper.toTimestamp (aEndpoint.getServiceActivationDateTime ()),
                                                                                       aEndpoint.getServiceDescription (),
                                                                                       DBValueHelper.toTimestamp (aEndpoint.getServiceExpirationDateTime ()),
                                                                                       aEndpoint.getTechnicalContactUrl (),
                                                                                       aEndpoint.getTechnicalInformationUrl (),
                                                                                       aEndpoint.getTransportProfile (),
                                                                                       aEndpoint.getExtensions ()
                                                                                                .getExtensionsAsJsonString ()));
        }
      }
    });
    if (eSuccess.isFailure ())
      return ESuccess.FAILURE;

    // Callback outside of transaction
    if (aUpdated.booleanValue ())
    {
      AuditHelper.onAuditModifySuccess (SMPServiceInformation.OT,
                                        "set-all",
                                        aSMPServiceInformation.getID (),
                                        aSMPServiceInformation.getServiceGroupID (),
                                        aSMPServiceInformation.getDocumentTypeIdentifier ().getURIEncoded (),
                                        aSMPServiceInformation.getAllProcesses (),
                                        aSMPServiceInformation.getExtensions ().getExtensionsAsJsonString ());

      m_aCBs.forEach (x -> x.onSMPServiceInformationUpdated (aSMPServiceInformation));
    }
    else
    {
      AuditHelper.onAuditCreateSuccess (SMPServiceInformation.OT,
                                        aSMPServiceInformation.getID (),
                                        aSMPServiceInformation.getServiceGroupID (),
                                        aSMPServiceInformation.getDocumentTypeIdentifier ().getURIEncoded (),
                                        aSMPServiceInformation.getAllProcesses (),
                                        aSMPServiceInformation.getExtensions ().getExtensionsAsJsonString ());
      m_aCBs.forEach (x -> x.onSMPServiceInformationCreated (aSMPServiceInformation));
    }

    return ESuccess.SUCCESS;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPEndpoint> getAllSMPEndpoints (@Nullable final IParticipantIdentifier aParticipantID,
                                                         @Nullable final IDocumentTypeIdentifier aDocTypeID,
                                                         @Nullable final IProcessIdentifier aProcessID,
                                                         @Nullable final String sTransportProfileID)
  {
    final ICommonsList <ISMPEndpoint> ret = new CommonsArrayList <> ();
    final ISMPServiceInformation aServiceInfo = getSMPServiceInformationOfServiceGroupAndDocumentType (aParticipantID,
                                                                                                       aDocTypeID);
    if (aServiceInfo != null)
    {
      final ISMPProcess aProcess = aServiceInfo.getProcessOfID (aProcessID);
      if (aProcess != null)
      {
        ret.addAll (aProcess.getAllEndpointsOfTransportProfile (sTransportProfileID));
      }
    }
    return ret;
  }

  @NonNull
  private EChange _deleteSMPServiceInformationNoCallback (@NonNull final DBExecutor aExecutor,
                                                          @NonNull final ISMPServiceInformation aSMPServiceInformation)
  {
    final Wrapper <Long> ret = new Wrapper <> (Long.valueOf (-1));
    final ESuccess eSuccess = aExecutor.performInTransaction (() -> {
      final IParticipantIdentifier aPID = aSMPServiceInformation.getServiceGroupParticipantIdentifier ();
      final IDocumentTypeIdentifier aDocTypeID = aSMPServiceInformation.getDocumentTypeIdentifier ();
      final long nCountEP = aExecutor.insertOrUpdateOrDelete ("DELETE FROM " +
                                                              m_sTableNameE +
                                                              " WHERE businessIdentifierScheme=? AND businessIdentifier=? AND documentIdentifierScheme=? AND documentIdentifier=?",
                                                              new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                         aPID.getValue (),
                                                                                                         aDocTypeID.getScheme (),
                                                                                                         aDocTypeID.getValue ()));
      final long nCountProc = aExecutor.insertOrUpdateOrDelete ("DELETE FROM " +
                                                                m_sTableNameP +
                                                                " WHERE businessIdentifierScheme=? AND businessIdentifier=? AND documentIdentifierScheme=? AND documentIdentifier=?",
                                                                new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                           aPID.getValue (),
                                                                                                           aDocTypeID.getScheme (),
                                                                                                           aDocTypeID.getValue ()));
      final long nCountSM = aExecutor.insertOrUpdateOrDelete ("DELETE FROM " +
                                                              m_sTableNameSM +
                                                              " WHERE businessIdentifierScheme=? AND businessIdentifier=? AND documentIdentifierScheme=? AND documentIdentifier=?",
                                                              new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                         aPID.getValue (),
                                                                                                         aDocTypeID.getScheme (),
                                                                                                         aDocTypeID.getValue ()));
      ret.set (Long.valueOf (nCountEP + nCountProc + nCountSM));
    });
    if (eSuccess.isFailure ())
      return EChange.UNCHANGED;
    return EChange.valueOf (ret.get ().longValue () > 0);
  }

  @NonNull
  public EChange deleteSMPServiceInformation (@Nullable final ISMPServiceInformation aSMPServiceInformation)
  {
    if (aSMPServiceInformation == null)
      return EChange.UNCHANGED;

    // Main deletion
    if (_deleteSMPServiceInformationNoCallback (newExecutor (), aSMPServiceInformation).isUnchanged ())
    {
      AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT, "no-such-id", aSMPServiceInformation.getID ());
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditDeleteSuccess (SMPServiceInformation.OT, aSMPServiceInformation.getID ());

    // Callback outside of transaction
    m_aCBs.forEach (x -> x.onSMPServiceInformationDeleted (aSMPServiceInformation));

    return EChange.CHANGED;
  }

  @NonNull
  public EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable final IParticipantIdentifier aParticipantID)
  {
    if (aParticipantID == null)
      return EChange.UNCHANGED;

    final Wrapper <Long> ret = new Wrapper <> (Long.valueOf (0));
    final Wrapper <ICommonsList <ISMPServiceInformation>> aAllDeleted = new Wrapper <> ();
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction (() -> {
      // get the old ones first
      aAllDeleted.set (_getAllSMPServiceInformationOfServiceGroup (aExecutor, aParticipantID));

      final long nCountEP = aExecutor.insertOrUpdateOrDelete ("DELETE FROM " +
                                                              m_sTableNameE +
                                                              " WHERE businessIdentifierScheme=? AND businessIdentifier=?",
                                                              new ConstantPreparedStatementDataProvider (aParticipantID.getScheme (),
                                                                                                         aParticipantID.getValue ()));
      final long nCountProc = aExecutor.insertOrUpdateOrDelete ("DELETE FROM " +
                                                                m_sTableNameP +
                                                                " WHERE businessIdentifierScheme=? AND businessIdentifier=?",
                                                                new ConstantPreparedStatementDataProvider (aParticipantID.getScheme (),
                                                                                                           aParticipantID.getValue ()));
      final long nCountSM = aExecutor.insertOrUpdateOrDelete ("DELETE FROM " +
                                                              m_sTableNameSM +
                                                              " WHERE businessIdentifierScheme=? AND businessIdentifier=?",
                                                              new ConstantPreparedStatementDataProvider (aParticipantID.getScheme (),
                                                                                                         aParticipantID.getValue ()));
      ret.set (Long.valueOf (nCountEP + nCountProc + nCountSM));
    });
    if (eSuccess.isFailure () || ret.get ().longValue () <= 0)
    {
      AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT, "no-such-id", aParticipantID.getURIEncoded ());
      return EChange.UNCHANGED;
    }

    // Callback outside of transaction
    if (aAllDeleted.isSet ())
      for (final ISMPServiceInformation aSMPServiceInformation : aAllDeleted.get ())
      {
        AuditHelper.onAuditDeleteSuccess (SMPServiceInformation.OT, aSMPServiceInformation.getID ());
        m_aCBs.forEach (x -> x.onSMPServiceInformationDeleted (aSMPServiceInformation));
      }

    return EChange.CHANGED;
  }

  @NonNull
  public EChange deleteSMPProcess (@Nullable final ISMPServiceInformation aSMPServiceInformation,
                                   @Nullable final ISMPProcess aProcess)
  {
    if (aSMPServiceInformation == null || aProcess == null)
      return EChange.UNCHANGED;

    final Wrapper <Long> ret = new Wrapper <> (Long.valueOf (0));
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction (() -> {
      final IParticipantIdentifier aPID = aSMPServiceInformation.getServiceGroupParticipantIdentifier ();
      final IDocumentTypeIdentifier aDocTypeID = aSMPServiceInformation.getDocumentTypeIdentifier ();
      final IProcessIdentifier aProcessID = aProcess.getProcessIdentifier ();
      final long nCountEP = aExecutor.insertOrUpdateOrDelete ("DELETE FROM " +
                                                              m_sTableNameE +
                                                              " WHERE businessIdentifierScheme=? AND businessIdentifier=? AND documentIdentifierScheme=? AND documentIdentifier=? AND processIdentifierType=? AND processIdentifier=?",
                                                              new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                         aPID.getValue (),
                                                                                                         aDocTypeID.getScheme (),
                                                                                                         aDocTypeID.getValue (),
                                                                                                         aProcessID.getScheme (),
                                                                                                         aProcessID.getValue ()));
      final long nCountProc = aExecutor.insertOrUpdateOrDelete ("DELETE FROM " +
                                                                m_sTableNameP +
                                                                " WHERE businessIdentifierScheme=? AND businessIdentifier=? AND documentIdentifierScheme=? AND documentIdentifier=? AND processIdentifierType=? AND processIdentifier=?",
                                                                new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                           aPID.getValue (),
                                                                                                           aDocTypeID.getScheme (),
                                                                                                           aDocTypeID.getValue (),
                                                                                                           aProcessID.getScheme (),
                                                                                                           aProcessID.getValue ()));
      ret.set (Long.valueOf (nCountEP + nCountProc));
    });
    if (eSuccess.isFailure ())
      return EChange.UNCHANGED;

    return EChange.valueOf (ret.get ().longValue () > 0);
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformation ()
  {
    final ICommonsList <ISMPServiceInformation> ret = new CommonsArrayList <> ();
    forEachSMPServiceInformation (ret::add);
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  @Override
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformation (@NonNull final IPagingSpec aPagingSpec,
                                                                            @Nullable final String sSearchText)
  {
    final ICommonsList <ISMPServiceInformation> ret = new CommonsArrayList <> ();
    if (aPagingSpec.isEmptyPage ())
      return ret;

    final SearchCondition aSearch = SMPJDBCQueryHelper.createSearchCondition (COLUMNS, sSearchText);
    _forEachSMPServiceInformation ((aSearch.isEmpty () ? "" : " WHERE " + aSearch.getSQL ()) +
                                   SMPJDBCQueryHelper.getOrderByAndPagingClause (COLUMNS, aPagingSpec),
                                   aSearch.getAllParams (),
                                   ret::add);
    // The DB result is grouped in a Map, so the order needs to be restored
    final Comparator <ISMPServiceInformation> aComparator = TableColumnHelper.getComparator (COLUMNS, aPagingSpec);
    if (aComparator != null)
      ret.sort (aComparator);
    return ret;
  }

  @Override
  public long getSMPServiceInformationCount (@Nullable final String sSearchText)
  {
    final SearchCondition aSearch = SMPJDBCQueryHelper.createSearchCondition (COLUMNS, sSearchText);
    if (aSearch.isEmpty ())
      return getSMPServiceInformationCount ();

    return newExecutor ().queryCount ("SELECT COUNT(*) FROM " + m_sTableNameSM + " sm WHERE " + aSearch.getSQL (),
                                      new ConstantPreparedStatementDataProvider (aSearch.getAllParams ()));
  }

  public void forEachSMPServiceInformation (@NonNull final Consumer <? super ISMPServiceInformation> aConsumer)
  {
    _forEachSMPServiceInformation (null, null, aConsumer);
  }

  private void _forEachSMPServiceInformation (@Nullable final String sServiceMetadataSuffix,
                                              @Nullable final ICommonsList <Object> aParams,
                                              @NonNull final Consumer <? super ISMPServiceInformation> aConsumer)
  {
    // If only a single page shall be returned, the paging must happen on the
    // Service Metadata level, because the joins create multiple rows per
    // Service Information object
    final String sServiceMetadataTable = sServiceMetadataSuffix == null ? m_sTableNameSM
                                                                        : "(SELECT businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, extension FROM " +
                                                                          m_sTableNameSM +
                                                                          " sm" +
                                                                          sServiceMetadataSuffix +
                                                                          ")";
    final String sSQL = "SELECT sm.businessIdentifierScheme, sm.businessIdentifier, sm.documentIdentifierScheme, sm.documentIdentifier, sm.extension," +
                        "   sp.processIdentifierType, sp.processIdentifier, sp.extension," +
                        "   se.id, se.transportProfile, ap.endpointReference, se.requireBusinessLevelSignature, se.minimumAuthenticationLevel," +
                        "     se.serviceActivationDate, se.serviceExpirationDate, ap.certificate, se.serviceDescription," +
                        "     se.technicalContactUrl, se.technicalInformationUrl, se.extension, se.accessPointID" +
                        " FROM " +
                        sServiceMetadataTable +
                        " sm" +
                        " INNER JOIN " +
                        m_sTableNameP +
                        " sp" +
                        "   ON sm.businessIdentifierScheme=sp.businessIdentifierScheme AND sm.businessIdentifier=sp.businessIdentifier" +
                        "   AND sm.documentIdentifierScheme=sp.documentIdentifierScheme AND sm.documentIdentifier=sp.documentIdentifier" +
                        " INNER JOIN " +
                        m_sTableNameE +
                        " se" +
                        "   ON sp.businessIdentifierScheme=se.businessIdentifierScheme AND sp.businessIdentifier=se.businessIdentifier" +
                        "   AND sp.documentIdentifierScheme=se.documentIdentifierScheme AND sp.documentIdentifier=se.documentIdentifier" +
                        "   AND sp.processIdentifierType=se.processIdentifierType AND sp.processIdentifier=se.processIdentifier" +
                        " LEFT OUTER JOIN " +
                        m_sTableNameAP +
                        " ap ON se.accessPointID=ap.id";
    final ICommonsList <DBResultRow> aDBResult = aParams == null || aParams.isEmpty () ? newExecutor ().queryAll (sSQL)
                                                                                       : newExecutor ().queryAll (sSQL,
                                                                                                                  new ConstantPreparedStatementDataProvider (aParams));

    final ICommonsMap <IParticipantIdentifier, ICommonsMap <DocTypeAndExtension, ICommonsMap <SMPProcess, ICommonsList <SMPEndpoint>>>> aGrouping = new CommonsHashMap <> ();
    if (aDBResult != null)
      for (final DBResultRow aDBRow : aDBResult)
      {
        // Participant ID
        final IParticipantIdentifier aParticipantID = new SimpleParticipantIdentifier (aDBRow.getAsString (0),
                                                                                       aDBRow.getAsString (1));
        // Document type ID and extension
        final IDocumentTypeIdentifier aDocTypeID = new SimpleDocumentTypeIdentifier (aDBRow.getAsString (2),
                                                                                     aDBRow.getAsString (3));
        final String sServiceInformationExtension = aDBRow.getAsString (4);
        // Process without endpoints
        final SMPProcess aProcess = new SMPProcess (new SimpleProcessIdentifier (aDBRow.getAsString (5),
                                                                                 aDBRow.getAsString (6)),
                                                    null,
                                                    aDBRow.getAsString (7));
        // Don't add endpoint to process, because that impacts
        // SMPProcess.equals/hashcode
        final SMPEndpoint aEndpoint = new SMPEndpoint (aDBRow.getAsString (8),
                                                       aDBRow.getAsString (9),
                                                       _toAccessPoint (aDBRow.getAsString (20),
                                                                       aDBRow.getAsString (10),
                                                                       aDBRow.getAsString (15)),
                                                       aDBRow.getAsBoolean (11,
                                                                            SMPEndpoint.DEFAULT_REQUIRES_BUSINESS_LEVEL_SIGNATURE),
                                                       aDBRow.getAsString (12),
                                                       aDBRow.getAsXMLOffsetDateTime (13),
                                                       aDBRow.getAsXMLOffsetDateTime (14),
                                                       aDBRow.getAsString (16),
                                                       aDBRow.getAsString (17),
                                                       aDBRow.getAsString (18),
                                                       aDBRow.getAsString (19));
        aGrouping.computeIfAbsent (aParticipantID, k -> new CommonsHashMap <> ())
                 .computeIfAbsent (new DocTypeAndExtension (aDocTypeID, sServiceInformationExtension),
                                   k -> new CommonsHashMap <> ())
                 .computeIfAbsent (aProcess, k -> new CommonsArrayList <> ())
                 .add (aEndpoint);
      }

    // Per participant ID
    for (final Map.Entry <IParticipantIdentifier, ICommonsMap <DocTypeAndExtension, ICommonsMap <SMPProcess, ICommonsList <SMPEndpoint>>>> aEntry : aGrouping.entrySet ())
    {
      final IParticipantIdentifier aParticipantID = aEntry.getKey ();

      // Per document type ID
      for (final Map.Entry <DocTypeAndExtension, ICommonsMap <SMPProcess, ICommonsList <SMPEndpoint>>> aEntry2 : aEntry.getValue ()
                                                                                                                       .entrySet ())
      {
        // Flatten list
        final ICommonsList <SMPProcess> aProcesses = new CommonsArrayList <> ();
        for (final Map.Entry <SMPProcess, ICommonsList <SMPEndpoint>> aEntry3 : aEntry2.getValue ().entrySet ())
        {
          final SMPProcess aProcess = aEntry3.getKey ();
          aProcess.addEndpoints (aEntry3.getValue ());
          aProcesses.add (aProcess);
        }

        final DocTypeAndExtension aDE = aEntry2.getKey ();
        aConsumer.accept (new SMPServiceInformation (aParticipantID, aDE.m_aDocTypeID, aProcesses, aDE.m_sExt));
      }
    }
  }

  @Nonnegative
  public long getSMPServiceInformationCount ()
  {
    return newExecutor ().queryCount ("SELECT COUNT(*) FROM " + m_sTableNameSM);
  }

  @NonNull
  @ReturnsMutableCopy
  private ICommonsList <ISMPServiceInformation> _getAllSMPServiceInformationOfServiceGroup (@NonNull final DBExecutor aExecutor,
                                                                                            @Nullable final IParticipantIdentifier aParticipantID)
  {
    final ICommonsList <ISMPServiceInformation> ret = new CommonsArrayList <> ();
    if (aParticipantID != null)
    {
      final ICommonsList <DBResultRow> aDBResult = aExecutor.queryAll ("SELECT sm.documentIdentifierScheme, sm.documentIdentifier, sm.extension," +
                                                                       "   sp.processIdentifierType, sp.processIdentifier, sp.extension," +
                                                                       "   se.id, se.transportProfile, ap.endpointReference, se.requireBusinessLevelSignature, se.minimumAuthenticationLevel," +
                                                                       "     se.serviceActivationDate, se.serviceExpirationDate, ap.certificate, se.serviceDescription," +
                                                                       "     se.technicalContactUrl, se.technicalInformationUrl, se.extension, se.accessPointID" +
                                                                       " FROM " +
                                                                       m_sTableNameSM +
                                                                       " sm" +
                                                                       " INNER JOIN " +
                                                                       m_sTableNameP +
                                                                       " sp" +
                                                                       "   ON sm.businessIdentifierScheme=sp.businessIdentifierScheme AND sm.businessIdentifier=sp.businessIdentifier" +
                                                                       "   AND sm.documentIdentifierScheme=sp.documentIdentifierScheme AND sm.documentIdentifier=sp.documentIdentifier" +
                                                                       " INNER JOIN " +
                                                                       m_sTableNameE +
                                                                       " se" +
                                                                       "   ON sp.businessIdentifierScheme=se.businessIdentifierScheme AND sp.businessIdentifier=se.businessIdentifier" +
                                                                       "   AND sp.documentIdentifierScheme=se.documentIdentifierScheme AND sp.documentIdentifier=se.documentIdentifier" +
                                                                       "   AND sp.processIdentifierType=se.processIdentifierType AND sp.processIdentifier=se.processIdentifier" +
                                                                       " LEFT OUTER JOIN " +
                                                                       m_sTableNameAP +
                                                                       " ap ON se.accessPointID=ap.id" +
                                                                       " WHERE sm.businessIdentifierScheme=? AND sm.businessIdentifier=?",
                                                                       new ConstantPreparedStatementDataProvider (aParticipantID.getScheme (),
                                                                                                                  aParticipantID.getValue ()));
      if (aDBResult != null)
      {
        final ICommonsMap <DocTypeAndExtension, ICommonsMap <SMPProcess, ICommonsList <SMPEndpoint>>> aGrouping = new CommonsHashMap <> ();
        for (final DBResultRow aDBRow : aDBResult)
        {
          // Document type ID and extension
          final IDocumentTypeIdentifier aDocTypeID = new SimpleDocumentTypeIdentifier (aDBRow.getAsString (0),
                                                                                       aDBRow.getAsString (1));
          final String sServiceInformationExtension = aDBRow.getAsString (2);
          // Process without endpoints
          final SMPProcess aProcess = new SMPProcess (new SimpleProcessIdentifier (aDBRow.getAsString (3),
                                                                                   aDBRow.getAsString (4)),
                                                      null,
                                                      aDBRow.getAsString (5));
          // Don't add endpoint to process, because that impacts
          // SMPProcess.equals/hashcode
          final SMPEndpoint aEndpoint = new SMPEndpoint (aDBRow.getAsString (6),
                                                         aDBRow.getAsString (7),
                                                         _toAccessPoint (aDBRow.getAsString (18),
                                                                         aDBRow.getAsString (8),
                                                                         aDBRow.getAsString (13)),
                                                         aDBRow.getAsBoolean (9,
                                                                              SMPEndpoint.DEFAULT_REQUIRES_BUSINESS_LEVEL_SIGNATURE),
                                                         aDBRow.getAsString (10),
                                                         aDBRow.getAsXMLOffsetDateTime (11),
                                                         aDBRow.getAsXMLOffsetDateTime (12),
                                                         aDBRow.getAsString (14),
                                                         aDBRow.getAsString (15),
                                                         aDBRow.getAsString (16),
                                                         aDBRow.getAsString (17));
          aGrouping.computeIfAbsent (new DocTypeAndExtension (aDocTypeID, sServiceInformationExtension),
                                     k -> new CommonsHashMap <> ())
                   .computeIfAbsent (aProcess, k -> new CommonsArrayList <> ())
                   .add (aEndpoint);
        }

        for (final Map.Entry <DocTypeAndExtension, ICommonsMap <SMPProcess, ICommonsList <SMPEndpoint>>> aEntry : aGrouping.entrySet ())
        {
          // Flatten list
          final ICommonsList <SMPProcess> aProcesses = new CommonsArrayList <> ();
          for (final Map.Entry <SMPProcess, ICommonsList <SMPEndpoint>> aEntry2 : aEntry.getValue ().entrySet ())
          {
            final SMPProcess aProcess = aEntry2.getKey ();
            aProcess.addEndpoints (aEntry2.getValue ());
            aProcesses.add (aProcess);
          }

          final DocTypeAndExtension aDE = aEntry.getKey ();
          ret.add (new SMPServiceInformation (aParticipantID, aDE.m_aDocTypeID, aProcesses, aDE.m_sExt));
        }
      }
    }
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformationOfServiceGroup (@Nullable final IParticipantIdentifier aParticipantID)
  {
    return _getAllSMPServiceInformationOfServiceGroup (newExecutor (), aParticipantID);
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <IDocumentTypeIdentifier> getAllSMPDocumentTypesOfServiceGroup (@Nullable final IParticipantIdentifier aParticipantID)
  {
    final ICommonsList <IDocumentTypeIdentifier> ret = new CommonsArrayList <> ();
    if (aParticipantID != null)
    {
      final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT sm.documentIdentifierScheme, sm.documentIdentifier" +
                                                                            " FROM " +
                                                                            m_sTableNameSM +
                                                                            " sm" +
                                                                            " WHERE sm.businessIdentifierScheme=? AND sm.businessIdentifier=?",
                                                                            new ConstantPreparedStatementDataProvider (aParticipantID.getScheme (),
                                                                                                                       aParticipantID.getValue ()));
      if (aDBResult != null)
        for (final DBResultRow aRow : aDBResult)
          ret.add (new SimpleDocumentTypeIdentifier (aRow.getAsString (0), aRow.getAsString (1)));
    }
    return ret;
  }

  @Nullable
  public ISMPServiceInformation getSMPServiceInformationOfServiceGroupAndDocumentType (@Nullable final IParticipantIdentifier aParticipantID,
                                                                                       @Nullable final IDocumentTypeIdentifier aDocTypeID)
  {
    if (aParticipantID == null)
      return null;
    if (aDocTypeID == null)
      return null;

    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT sm.extension," +
                                                                          "   sp.processIdentifierType, sp.processIdentifier, sp.extension," +
                                                                          "   se.id, se.transportProfile, ap.endpointReference, se.requireBusinessLevelSignature, se.minimumAuthenticationLevel," +
                                                                          "     se.serviceActivationDate, se.serviceExpirationDate, ap.certificate, se.serviceDescription," +
                                                                          "     se.technicalContactUrl, se.technicalInformationUrl, se.extension, se.accessPointID" +
                                                                          " FROM " +
                                                                          m_sTableNameSM +
                                                                          " sm" +
                                                                          " INNER JOIN " +
                                                                          m_sTableNameP +
                                                                          " sp" +
                                                                          "   ON sm.businessIdentifierScheme=sp.businessIdentifierScheme AND sm.businessIdentifier=sp.businessIdentifier" +
                                                                          "   AND sm.documentIdentifierScheme=sp.documentIdentifierScheme AND sm.documentIdentifier=sp.documentIdentifier" +
                                                                          " INNER JOIN " +
                                                                          m_sTableNameE +
                                                                          " se" +
                                                                          "   ON sp.businessIdentifierScheme=se.businessIdentifierScheme AND sp.businessIdentifier=se.businessIdentifier" +
                                                                          "   AND sp.documentIdentifierScheme=se.documentIdentifierScheme AND sp.documentIdentifier=se.documentIdentifier" +
                                                                          "   AND sp.processIdentifierType=se.processIdentifierType AND sp.processIdentifier=se.processIdentifier" +
                                                                          " LEFT OUTER JOIN " +
                                                                          m_sTableNameAP +
                                                                          " ap ON se.accessPointID=ap.id" +
                                                                          " WHERE sm.businessIdentifierScheme=? AND sm.businessIdentifier=? AND sm.documentIdentifierScheme=? AND sm.documentIdentifier=?",
                                                                          new ConstantPreparedStatementDataProvider (aParticipantID.getScheme (),
                                                                                                                     aParticipantID.getValue (),
                                                                                                                     aDocTypeID.getScheme (),
                                                                                                                     aDocTypeID.getValue ()));
    if (aDBResult != null && aDBResult.isNotEmpty ())
    {
      final String sServiceInformationExtension = aDBResult.getFirstOrNull ().getAsString (0);

      final ICommonsMap <SMPProcess, ICommonsList <SMPEndpoint>> aEndpoints = new CommonsHashMap <> ();
      for (final DBResultRow aDBRow : aDBResult)
      {
        // Process without endpoints as key
        final SMPProcess aProcess = new SMPProcess (new SimpleProcessIdentifier (aDBRow.getAsString (1),
                                                                                 aDBRow.getAsString (2)),
                                                    null,
                                                    aDBRow.getAsString (3));
        final SMPEndpoint aEndpoint = new SMPEndpoint (aDBRow.getAsString (4),
                                                       aDBRow.getAsString (5),
                                                       _toAccessPoint (aDBRow.getAsString (16),
                                                                       aDBRow.getAsString (6),
                                                                       aDBRow.getAsString (11)),
                                                       aDBRow.getAsBoolean (7,
                                                                            SMPEndpoint.DEFAULT_REQUIRES_BUSINESS_LEVEL_SIGNATURE),
                                                       aDBRow.getAsString (8),
                                                       aDBRow.getAsXMLOffsetDateTime (9),
                                                       aDBRow.getAsXMLOffsetDateTime (10),
                                                       aDBRow.getAsString (12),
                                                       aDBRow.getAsString (13),
                                                       aDBRow.getAsString (14),
                                                       aDBRow.getAsString (15));
        aEndpoints.computeIfAbsent (aProcess, k -> new CommonsArrayList <> ()).add (aEndpoint);
      }

      // Flatten list
      final ICommonsList <SMPProcess> aProcesses = new CommonsArrayList <> ();
      for (final Map.Entry <SMPProcess, ICommonsList <SMPEndpoint>> aEntry : aEndpoints.entrySet ())
      {
        final SMPProcess aProcess = aEntry.getKey ();
        aProcess.addEndpoints (aEntry.getValue ());
        aProcesses.add (aProcess);
      }
      return new SMPServiceInformation (aParticipantID, aDocTypeID, aProcesses, sServiceInformationExtension);
    }
    return null;
  }

  public boolean containsAnyEndpointWithTransportProfile (@Nullable final String sTransportProfileID)
  {
    if (StringHelper.isEmpty (sTransportProfileID))
      return false;

    final long nCount = newExecutor ().queryCount ("SELECT COUNT(*) FROM " +
                                                   m_sTableNameE +
                                                   " WHERE transportProfile=?",
                                                   new ConstantPreparedStatementDataProvider (sTransportProfileID));
    return nCount > 0;
  }

  @Nonnegative
  public long getEndpointCount ()
  {
    return newExecutor ().queryCount ("SELECT COUNT(*) FROM " + m_sTableNameE);
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsMap <String, IEndpointUsageInfo> getEndpointURLUsageMap ()
  {
    final ICommonsMap <String, IEndpointUsageInfo> ret = new CommonsHashMap <> ();
    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT ap.endpointReference, se.businessIdentifierScheme, se.businessIdentifier FROM " +
                                                                          m_sTableNameE +
                                                                          " se INNER JOIN " +
                                                                          m_sTableNameAP +
                                                                          " ap ON se.accessPointID=ap.id" +
                                                                          " WHERE ap.endpointReference IS NOT NULL");
    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
      {
        final String sURL = aRow.getAsString (0);
        if (StringHelper.isNotEmpty (sURL))
        {
          final String sServiceGroupID = CIdentifier.getURIEncoded (aRow.getAsString (1), aRow.getAsString (2));

          final IEndpointUsageInfo aInfo = ret.computeIfAbsent (sURL, k -> new EndpointUsageInfo ());
          ((EndpointUsageInfo) aInfo).incrementForServiceGroupID (sServiceGroupID);
        }
      }
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsMap <String, IEndpointUsageInfo> getEndpointCertificateUsageMap ()
  {
    final ICommonsMap <String, IEndpointUsageInfo> ret = new CommonsHashMap <> ();
    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT ap.certificate, se.businessIdentifierScheme, se.businessIdentifier FROM " +
                                                                          m_sTableNameE +
                                                                          " se INNER JOIN " +
                                                                          m_sTableNameAP +
                                                                          " ap ON se.accessPointID=ap.id");
    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
      {
        final String sNormalizedCert = SMPCertificateHelper.getNormalizedCert (aRow.getAsString (0));
        final String sServiceGroupID = CIdentifier.getURIEncoded (aRow.getAsString (1), aRow.getAsString (2));

        final IEndpointUsageInfo aInfo = ret.computeIfAbsent (sNormalizedCert, k -> new EndpointUsageInfo ());
        ((EndpointUsageInfo) aInfo).incrementForServiceGroupID (sServiceGroupID);
      }
    return ret;
  }

  /** Maximum number of elements in a single SQL "IN" clause (Oracle limit is 1000) */
  private static final int MAX_IN_CLAUSE_ELEMENTS = 500;

  /**
   * Count the endpoints referencing one of the provided Access Point IDs.
   *
   * @param aAccessPointIDs
   *        The Access Point IDs to look for. May not be <code>null</code>.
   * @param aServiceGroupID
   *        Optional service group filter. May be <code>null</code>.
   * @return The number of matching endpoints.
   */
  @Nonnegative
  private long _countEndpointsUsingAccessPoints (@NonNull final ICommonsSet <String> aAccessPointIDs,
                                                 @Nullable final IParticipantIdentifier aServiceGroupID)
  {
    if (aAccessPointIDs.isEmpty ())
      return 0;

    // Chunk the IDs, because some databases (e.g. Oracle) limit the number of elements of a single
    // "IN" clause
    final ICommonsList <String> aAllIDs = new CommonsArrayList <> (aAccessPointIDs);
    long ret = 0;
    for (int nStart = 0; nStart < aAllIDs.size (); nStart += MAX_IN_CLAUSE_ELEMENTS)
    {
      final List <String> aChunk = aAllIDs.subList (nStart,
                                                    Math.min (nStart + MAX_IN_CLAUSE_ELEMENTS, aAllIDs.size ()));
      final ICommonsList <Object> aParams = new CommonsArrayList <> (aChunk);
      final StringBuilder aSQL = new StringBuilder ("SELECT COUNT(*) FROM ").append (m_sTableNameE)
                                                                            .append (" WHERE accessPointID IN (")
                                                                            .append (StringHelper.getRepeated ("?,",
                                                                                                               aChunk.size () -
                                                                                                                     1))
                                                                            .append ("?)");
      if (aServiceGroupID != null)
      {
        aSQL.append (" AND businessIdentifierScheme=? AND businessIdentifier=?");
        aParams.add (aServiceGroupID.getScheme ());
        aParams.add (aServiceGroupID.getValue ());
      }
      ret += newExecutor ().queryCount (aSQL.toString (), new ConstantPreparedStatementDataProvider (aParams));
    }
    return ret;
  }

  @Nonnegative
  public long updateAllEndpointURLs (@Nullable final IParticipantIdentifier aServiceGroupID,
                                     @NonNull final String sOldURL,
                                     @NonNull final String sNewURL)
  {
    ValueEnforcer.notNull (sOldURL, "OldURL");
    ValueEnforcer.notNull (sNewURL, "NewURL");

    if (sOldURL.equals (sNewURL))
      return 0;

    final ISMPAccessPoint aOldAP = m_aAccessPointMgr.findAccessPoint (sOldURL);
    if (aOldAP == null)
      return 0;

    final long nEndpointsChanged = _countEndpointsUsingAccessPoints (new CommonsHashSet <> (aOldAP.getID ()),
                                                                     aServiceGroupID);
    if (nEndpointsChanged == 0)
      return 0;

    final ISMPAccessPoint aTargetAP = m_aAccessPointMgr.findAccessPoint (sNewURL);
    if (aServiceGroupID == null && aTargetAP == null)
    {
      // Fast path: all endpoints of this Access Point are affected and the new URL is not yet in
      // use, so simply rename the Access Point. No endpoint row needs to be touched at all.
      m_aAccessPointMgr.updateAccessPointEndpointReference (aOldAP.getID (), sNewURL);
      return nEndpointsChanged;
    }

    // Only a part of the endpoints is affected and/or the new URL already exists, so the affected
    // endpoints must be re-pointed to the Access Point of the new URL
    final ISMPAccessPoint aNewAP;
    if (aTargetAP != null)
    {
      if (!aTargetAP.hasSameCertificate (aOldAP.getCertificate ()))
        LOGGER.warn ("The Access Point '" +
                     sNewURL +
                     "' already exists with a different certificate. The affected endpoints now use the certificate of '" +
                     sNewURL +
                     "'.");
      aNewAP = aTargetAP;
    }
    else
      aNewAP = m_aAccessPointMgr.getOrCreateAccessPoint (sNewURL, aOldAP.getCertificate ());

    final long nChanged;
    if (aServiceGroupID != null)
      nChanged = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                        m_sTableNameE +
                                                        " SET accessPointID=? WHERE accessPointID=? AND businessIdentifierScheme=? AND businessIdentifier=?",
                                                        new ConstantPreparedStatementDataProvider (aNewAP.getID (),
                                                                                                   aOldAP.getID (),
                                                                                                   aServiceGroupID.getScheme (),
                                                                                                   aServiceGroupID.getValue ()));
    else
      nChanged = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                        m_sTableNameE +
                                                        " SET accessPointID=? WHERE accessPointID=?",
                                                        new ConstantPreparedStatementDataProvider (aNewAP.getID (),
                                                                                                   aOldAP.getID ()));
    if (nChanged > 0)
      _deleteAllUnusedAccessPoints ();
    return Math.max (nChanged, 0);
  }

  @Nonnegative
  public long updateAllEndpointCertificates (@NonNull final String sOldCert, @NonNull final String sNewCert)
  {
    ValueEnforcer.notNull (sOldCert, "OldCert");
    ValueEnforcer.notNull (sNewCert, "NewCert");

    final String sOldCertNormalized = SMPCertificateHelper.getNormalizedCert (sOldCert);

    // The certificate is an attribute of the Access Point, so only the (few) Access Point rows need
    // to be updated - no endpoint row is touched at all
    final ICommonsSet <String> aAPIDs = m_aAccessPointMgr.getAllAccessPointIDsWithCertificate (sOldCertNormalized);
    if (aAPIDs.isEmpty ())
      return 0;

    // Determine the number of affected endpoints for the caller, before the change is applied
    final long nEndpointsChanged = _countEndpointsUsingAccessPoints (aAPIDs, null);
    m_aAccessPointMgr.updateAllAccessPointCertificates (sOldCertNormalized, sNewCert);
    return nEndpointsChanged;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsSet <String> getAllUsedAccessPointIDs ()
  {
    final ICommonsSet <String> ret = new CommonsHashSet <> ();
    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT DISTINCT accessPointID FROM " +
                                                                          m_sTableNameE +
                                                                          " WHERE accessPointID IS NOT NULL");
    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
        ret.add (aRow.getAsString (0));
    return ret;
  }

  private void _deleteAllUnusedAccessPoints ()
  {
    m_aAccessPointMgr.deleteAllUnusedAccessPoints (getAllUsedAccessPointIDs ());
  }
}
