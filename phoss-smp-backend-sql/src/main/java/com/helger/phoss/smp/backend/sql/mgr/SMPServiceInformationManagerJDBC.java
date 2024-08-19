/*
 * Copyright (C) 2019-2024 Philip Helger and contributors
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

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.mutable.MutableBoolean;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.wrapper.Wrapper;
import com.helger.db.api.helper.DBValueHelper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.db.jdbc.mgr.AbstractJDBCEnabledManager;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.simple.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.peppolid.simple.process.SimpleProcessIdentifier;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationCallback;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.SMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformation;
import com.helger.photon.audit.AuditHelper;

/**
 * A JDBC based implementation of the {@link ISMPServiceInformationManager}
 * interface.
 *
 * @author Philip Helger
 * @since 5.3.0
 */
public final class SMPServiceInformationManagerJDBC extends AbstractJDBCEnabledManager implements
                                                    ISMPServiceInformationManager
{
  @MustImplementEqualsAndHashcode
  private static final class DocTypeAndExtension
  {
    final IDocumentTypeIdentifier m_aDocTypeID;
    final String m_sExt;

    public DocTypeAndExtension (@Nonnull final IDocumentTypeIdentifier aDocTypeID, final String sExt)
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

  private final ISMPServiceGroupManager m_aServiceGroupMgr;
  private final CallbackList <ISMPServiceInformationCallback> m_aCBs = new CallbackList <> ();

  /**
   * Constructor
   *
   * @param aDBExecSupplier
   *        The supplier for {@link DBExecutor} objects. May not be
   *        <code>null</code>.
   * @param aServiceGroupMgr
   *        The service group manager to use. May not be <code>null</code>.
   */
  public SMPServiceInformationManagerJDBC (@Nonnull final Supplier <? extends DBExecutor> aDBExecSupplier,
                                           @Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    super (aDBExecSupplier);
    m_aServiceGroupMgr = aServiceGroupMgr;
  }

  @Nonnull
  @ReturnsMutableObject
  public CallbackList <ISMPServiceInformationCallback> serviceInformationCallbacks ()
  {
    return m_aCBs;
  }

  @Nonnull
  public ESuccess mergeSMPServiceInformation (@Nonnull final ISMPServiceInformation aSMPServiceInformation)
  {
    ValueEnforcer.notNull (aSMPServiceInformation, "ServiceInformation");

    final MutableBoolean aUpdated = new MutableBoolean (false);

    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Simply delete the old one
      final EChange eDeleted = _deleteSMPServiceInformationNoCallback (aSMPServiceInformation);
      aUpdated.set (eDeleted.isChanged ());

      // Insert new processes
      final IParticipantIdentifier aPID = aSMPServiceInformation.getServiceGroup ().getParticipantIdentifier ();
      final IDocumentTypeIdentifier aDocTypeID = aSMPServiceInformation.getDocumentTypeIdentifier ();

      aExecutor.insertOrUpdateOrDelete ("INSERT INTO smp_service_metadata (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, extension) VALUES (?, ?, ?, ?, ?)",
                                        new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                   aPID.getValue (),
                                                                                   aDocTypeID.getScheme (),
                                                                                   aDocTypeID.getValue (),
                                                                                   aSMPServiceInformation.getExtensions ()
                                                                                                         .getExtensionsAsJsonString ()));

      for (final ISMPProcess aProcess : aSMPServiceInformation.getAllProcesses ())
      {
        final IProcessIdentifier aProcessID = aProcess.getProcessIdentifier ();
        aExecutor.insertOrUpdateOrDelete ("INSERT INTO smp_process (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, processIdentifierType, processIdentifier, extension) VALUES (?, ?, ?, ?, ?, ?, ?)",
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
          aExecutor.insertOrUpdateOrDelete ("INSERT INTO smp_endpoint (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, processIdentifierType, processIdentifier," +
                                            " certificate, endpointReference, minimumAuthenticationLevel, requireBusinessLevelSignature, serviceActivationDate, serviceDescription, serviceExpirationDate, technicalContactUrl, technicalInformationUrl, transportProfile," +
                                            " extension) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                            new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                       aPID.getValue (),
                                                                                       aDocTypeID.getScheme (),
                                                                                       aDocTypeID.getValue (),
                                                                                       aProcessID.getScheme (),
                                                                                       aProcessID.getValue (),
                                                                                       aEndpoint.getCertificate (),
                                                                                       aEndpoint.getEndpointReference (),
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

  @Nullable
  public ISMPServiceInformation findServiceInformation (@Nullable final ISMPServiceGroup aServiceGroup,
                                                        @Nullable final IDocumentTypeIdentifier aDocTypeID,
                                                        @Nullable final IProcessIdentifier aProcessID,
                                                        @Nullable final ISMPTransportProfile aTransportProfile)
  {
    final ISMPServiceInformation aServiceInfo = getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                       aDocTypeID);
    if (aServiceInfo != null)
    {
      final ISMPProcess aProcess = aServiceInfo.getProcessOfID (aProcessID);
      if (aProcess != null)
      {
        final ISMPEndpoint aEndpoint = aProcess.getEndpointOfTransportProfile (aTransportProfile);
        if (aEndpoint != null)
          return aServiceInfo;
      }
    }
    return null;
  }

  @Nonnull
  private EChange _deleteSMPServiceInformationNoCallback (@Nonnull final ISMPServiceInformation aSMPServiceInformation)
  {
    final Wrapper <Long> ret = new Wrapper <> (Long.valueOf (-1));
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      final IParticipantIdentifier aPID = aSMPServiceInformation.getServiceGroup ().getParticipantIdentifier ();
      final IDocumentTypeIdentifier aDocTypeID = aSMPServiceInformation.getDocumentTypeIdentifier ();
      final long nCountEP = aExecutor.insertOrUpdateOrDelete ("DELETE FROM smp_endpoint" +
                                                              " WHERE businessIdentifierScheme=? AND businessIdentifier=? AND documentIdentifierScheme=? AND documentIdentifier=?",
                                                              new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                         aPID.getValue (),
                                                                                                         aDocTypeID.getScheme (),
                                                                                                         aDocTypeID.getValue ()));
      final long nCountProc = aExecutor.insertOrUpdateOrDelete ("DELETE FROM smp_process" +
                                                                " WHERE businessIdentifierScheme=? AND businessIdentifier=? AND documentIdentifierScheme=? AND documentIdentifier=?",
                                                                new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                           aPID.getValue (),
                                                                                                           aDocTypeID.getScheme (),
                                                                                                           aDocTypeID.getValue ()));
      final long nCountSM = aExecutor.insertOrUpdateOrDelete ("DELETE FROM smp_service_metadata" +
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

  @Nonnull
  public EChange deleteSMPServiceInformation (@Nullable final ISMPServiceInformation aSMPServiceInformation)
  {
    if (aSMPServiceInformation == null)
      return EChange.UNCHANGED;

    // Main deletion
    if (_deleteSMPServiceInformationNoCallback (aSMPServiceInformation).isUnchanged ())
    {
      AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT, "no-such-id", aSMPServiceInformation.getID ());
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditDeleteSuccess (SMPServiceInformation.OT, aSMPServiceInformation.getID ());

    // Callback outside of transaction
    m_aCBs.forEach (x -> x.onSMPServiceInformationDeleted (aSMPServiceInformation));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    if (aServiceGroup == null)
      return EChange.UNCHANGED;

    final Wrapper <Long> ret = new Wrapper <> (Long.valueOf (0));
    final Wrapper <ICommonsList <ISMPServiceInformation>> aAllDeleted = new Wrapper <> ();
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // get the old ones first
      aAllDeleted.set (getAllSMPServiceInformationOfServiceGroup (aServiceGroup));

      final IParticipantIdentifier aPID = aServiceGroup.getParticipantIdentifier ();
      final long nCountEP = aExecutor.insertOrUpdateOrDelete ("DELETE FROM smp_endpoint" +
                                                              " WHERE businessIdentifierScheme=? AND businessIdentifier=?",
                                                              new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                         aPID.getValue ()));
      final long nCountProc = aExecutor.insertOrUpdateOrDelete ("DELETE FROM smp_process" +
                                                                " WHERE businessIdentifierScheme=? AND businessIdentifier=?",
                                                                new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                           aPID.getValue ()));
      final long nCountSM = aExecutor.insertOrUpdateOrDelete ("DELETE FROM smp_service_metadata" +
                                                              " WHERE businessIdentifierScheme=? AND businessIdentifier=?",
                                                              new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                         aPID.getValue ()));
      ret.set (Long.valueOf (nCountEP + nCountProc + nCountSM));
    });
    if (eSuccess.isFailure () || ret.get ().longValue () <= 0)
    {
      AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT, "no-such-id", aServiceGroup.getID ());
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

  @Nonnull
  public EChange deleteSMPProcess (@Nullable final ISMPServiceInformation aSMPServiceInformation,
                                   @Nullable final ISMPProcess aProcess)
  {
    if (aSMPServiceInformation == null || aProcess == null)
      return EChange.UNCHANGED;

    final Wrapper <Long> ret = new Wrapper <> (Long.valueOf (0));
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      final IParticipantIdentifier aPID = aSMPServiceInformation.getServiceGroup ().getParticipantIdentifier ();
      final IDocumentTypeIdentifier aDocTypeID = aSMPServiceInformation.getDocumentTypeIdentifier ();
      final IProcessIdentifier aProcessID = aProcess.getProcessIdentifier ();
      final long nCountEP = aExecutor.insertOrUpdateOrDelete ("DELETE FROM smp_endpoint" +
                                                              " WHERE businessIdentifierScheme=? AND businessIdentifier=? AND documentIdentifierScheme=? AND documentIdentifier=? AND processIdentifierType=? AND processIdentifier=?",
                                                              new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                         aPID.getValue (),
                                                                                                         aDocTypeID.getScheme (),
                                                                                                         aDocTypeID.getValue (),
                                                                                                         aProcessID.getScheme (),
                                                                                                         aProcessID.getValue ()));
      final long nCountProc = aExecutor.insertOrUpdateOrDelete ("DELETE FROM smp_process" +
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

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformation ()
  {
    final ICommonsList <ISMPServiceInformation> ret = new CommonsArrayList <> ();
    forEachSMPServiceInformation (ret::add);
    return ret;
  }

  public void forEachSMPServiceInformation (@Nonnull final Consumer <? super ISMPServiceInformation> aConsumer)
  {
    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT sm.businessIdentifierScheme, sm.businessIdentifier, sm.documentIdentifierScheme, sm.documentIdentifier, sm.extension," +
                                                                          "   sp.processIdentifierType, sp.processIdentifier, sp.extension," +
                                                                          "   se.transportProfile, se.endpointReference, se.requireBusinessLevelSignature, se.minimumAuthenticationLevel," +
                                                                          "     se.serviceActivationDate, se.serviceExpirationDate, se.certificate, se.serviceDescription," +
                                                                          "     se.technicalContactUrl, se.technicalInformationUrl, se.extension" +
                                                                          " FROM smp_service_metadata sm" +
                                                                          " INNER JOIN smp_process sp" +
                                                                          "   ON sm.businessIdentifierScheme=sp.businessIdentifierScheme AND sm.businessIdentifier=sp.businessIdentifier" +
                                                                          "   AND sm.documentIdentifierScheme=sp.documentIdentifierScheme AND sm.documentIdentifier=sp.documentIdentifier" +
                                                                          " INNER JOIN smp_endpoint se" +
                                                                          "   ON sp.businessIdentifierScheme=se.businessIdentifierScheme AND sp.businessIdentifier=se.businessIdentifier" +
                                                                          "   AND sp.documentIdentifierScheme=se.documentIdentifierScheme AND sp.documentIdentifier=se.documentIdentifier" +
                                                                          "   AND sp.processIdentifierType=se.processIdentifierType AND sp.processIdentifier=se.processIdentifier");

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
                                                       aDBRow.getAsBoolean (10,
                                                                            SMPEndpoint.DEFAULT_REQUIRES_BUSINESS_LEVEL_SIGNATURE),
                                                       aDBRow.getAsString (11),
                                                       aDBRow.getAsXMLOffsetDateTime (12),
                                                       aDBRow.getAsXMLOffsetDateTime (13),
                                                       aDBRow.getAsString (14),
                                                       aDBRow.getAsString (15),
                                                       aDBRow.getAsString (16),
                                                       aDBRow.getAsString (17),
                                                       aDBRow.getAsString (18));
        aGrouping.computeIfAbsent (aParticipantID, k -> new CommonsHashMap <> ())
                 .computeIfAbsent (new DocTypeAndExtension (aDocTypeID, sServiceInformationExtension),
                                   k -> new CommonsHashMap <> ())
                 .computeIfAbsent (aProcess, k -> new CommonsArrayList <> ())
                 .add (aEndpoint);
      }

    // Per participant ID
    for (final Map.Entry <IParticipantIdentifier, ICommonsMap <DocTypeAndExtension, ICommonsMap <SMPProcess, ICommonsList <SMPEndpoint>>>> aEntry : aGrouping.entrySet ())
    {
      final ISMPServiceGroup aServiceGroup = m_aServiceGroupMgr.getSMPServiceGroupOfID (aEntry.getKey ());
      if (aServiceGroup == null)
        throw new IllegalStateException ("Failed to resolve service group for participant ID '" +
                                         aEntry.getKey ().getURIEncoded () +
                                         "'");

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
        aConsumer.accept (new SMPServiceInformation (aServiceGroup, aDE.m_aDocTypeID, aProcesses, aDE.m_sExt));
      }
    }
  }

  @Nonnegative
  public long getSMPServiceInformationCount ()
  {
    return newExecutor ().queryCount ("SELECT COUNT(*) FROM smp_service_metadata");
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformationOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    final ICommonsList <ISMPServiceInformation> ret = new CommonsArrayList <> ();
    if (aServiceGroup != null)
    {
      final IParticipantIdentifier aPID = aServiceGroup.getParticipantIdentifier ();
      final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT sm.documentIdentifierScheme, sm.documentIdentifier, sm.extension," +
                                                                            "   sp.processIdentifierType, sp.processIdentifier, sp.extension," +
                                                                            "   se.transportProfile, se.endpointReference, se.requireBusinessLevelSignature, se.minimumAuthenticationLevel," +
                                                                            "     se.serviceActivationDate, se.serviceExpirationDate, se.certificate, se.serviceDescription," +
                                                                            "     se.technicalContactUrl, se.technicalInformationUrl, se.extension" +
                                                                            " FROM smp_service_metadata sm" +
                                                                            " INNER JOIN smp_process sp" +
                                                                            "   ON sm.businessIdentifierScheme=sp.businessIdentifierScheme AND sm.businessIdentifier=sp.businessIdentifier" +
                                                                            "   AND sm.documentIdentifierScheme=sp.documentIdentifierScheme AND sm.documentIdentifier=sp.documentIdentifier" +
                                                                            " INNER JOIN smp_endpoint se" +
                                                                            "   ON sp.businessIdentifierScheme=se.businessIdentifierScheme AND sp.businessIdentifier=se.businessIdentifier" +
                                                                            "   AND sp.documentIdentifierScheme=se.documentIdentifierScheme AND sp.documentIdentifier=se.documentIdentifier" +
                                                                            "   AND sp.processIdentifierType=se.processIdentifierType AND sp.processIdentifier=se.processIdentifier" +
                                                                            " WHERE sm.businessIdentifierScheme=? AND sm.businessIdentifier=?",
                                                                            new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                                       aPID.getValue ()));
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
                                                         aDBRow.getAsBoolean (8,
                                                                              SMPEndpoint.DEFAULT_REQUIRES_BUSINESS_LEVEL_SIGNATURE),
                                                         aDBRow.getAsString (9),
                                                         aDBRow.getAsXMLOffsetDateTime (10),
                                                         aDBRow.getAsXMLOffsetDateTime (11),
                                                         aDBRow.getAsString (12),
                                                         aDBRow.getAsString (13),
                                                         aDBRow.getAsString (14),
                                                         aDBRow.getAsString (15),
                                                         aDBRow.getAsString (16));
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
          ret.add (new SMPServiceInformation (aServiceGroup, aDE.m_aDocTypeID, aProcesses, aDE.m_sExt));
        }
      }
    }
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IDocumentTypeIdentifier> getAllSMPDocumentTypesOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    final ICommonsList <IDocumentTypeIdentifier> ret = new CommonsArrayList <> ();
    if (aServiceGroup != null)
    {
      final IParticipantIdentifier aPID = aServiceGroup.getParticipantIdentifier ();
      final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT sm.documentIdentifierScheme, sm.documentIdentifier" +
                                                                            " FROM smp_service_metadata sm" +
                                                                            " WHERE sm.businessIdentifierScheme=? AND sm.businessIdentifier=?",
                                                                            new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                                       aPID.getValue ()));
      if (aDBResult != null)
        for (final DBResultRow aRow : aDBResult)
          ret.add (new SimpleDocumentTypeIdentifier (aRow.getAsString (0), aRow.getAsString (1)));
    }
    return ret;
  }

  @Nullable
  public ISMPServiceInformation getSMPServiceInformationOfServiceGroupAndDocumentType (@Nullable final ISMPServiceGroup aServiceGroup,
                                                                                       @Nullable final IDocumentTypeIdentifier aDocTypeID)
  {
    if (aServiceGroup == null)
      return null;
    if (aDocTypeID == null)
      return null;

    final IParticipantIdentifier aPID = aServiceGroup.getParticipantIdentifier ();
    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT sm.extension," +
                                                                          "   sp.processIdentifierType, sp.processIdentifier, sp.extension," +
                                                                          "   se.transportProfile, se.endpointReference, se.requireBusinessLevelSignature, se.minimumAuthenticationLevel," +
                                                                          "     se.serviceActivationDate, se.serviceExpirationDate, se.certificate, se.serviceDescription," +
                                                                          "     se.technicalContactUrl, se.technicalInformationUrl, se.extension" +
                                                                          " FROM smp_service_metadata sm" +
                                                                          " INNER JOIN smp_process sp" +
                                                                          "   ON sm.businessIdentifierScheme=sp.businessIdentifierScheme AND sm.businessIdentifier=sp.businessIdentifier" +
                                                                          "   AND sm.documentIdentifierScheme=sp.documentIdentifierScheme AND sm.documentIdentifier=sp.documentIdentifier" +
                                                                          " INNER JOIN smp_endpoint se" +
                                                                          "   ON sp.businessIdentifierScheme=se.businessIdentifierScheme AND sp.businessIdentifier=se.businessIdentifier" +
                                                                          "   AND sp.documentIdentifierScheme=se.documentIdentifierScheme AND sp.documentIdentifier=se.documentIdentifier" +
                                                                          "   AND sp.processIdentifierType=se.processIdentifierType AND sp.processIdentifier=se.processIdentifier" +
                                                                          " WHERE sm.businessIdentifierScheme=? AND sm.businessIdentifier=? AND sm.documentIdentifierScheme=? AND sm.documentIdentifier=?",
                                                                          new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                                     aPID.getValue (),
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
                                                       aDBRow.getAsBoolean (6,
                                                                            SMPEndpoint.DEFAULT_REQUIRES_BUSINESS_LEVEL_SIGNATURE),
                                                       aDBRow.getAsString (7),
                                                       aDBRow.getAsXMLOffsetDateTime (8),
                                                       aDBRow.getAsXMLOffsetDateTime (9),
                                                       aDBRow.getAsString (10),
                                                       aDBRow.getAsString (11),
                                                       aDBRow.getAsString (12),
                                                       aDBRow.getAsString (13),
                                                       aDBRow.getAsString (14));
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
      return new SMPServiceInformation (aServiceGroup, aDocTypeID, aProcesses, sServiceInformationExtension);
    }
    return null;
  }

  public boolean containsAnyEndpointWithTransportProfile (@Nullable final String sTransportProfileID)
  {
    if (StringHelper.hasNoText (sTransportProfileID))
      return false;

    final long nCount = newExecutor ().queryCount ("SELECT COUNT(*) FROM smp_endpoint WHERE transportProfile=?",
                                                   new ConstantPreparedStatementDataProvider (sTransportProfileID));
    return nCount > 0;
  }
}
