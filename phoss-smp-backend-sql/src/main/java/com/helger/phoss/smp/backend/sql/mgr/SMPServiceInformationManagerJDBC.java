/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.backend.sql.mgr;

import java.util.Map;
import java.util.Optional;

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
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.simple.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.peppolid.simple.process.SimpleProcessIdentifier;
import com.helger.phoss.smp.backend.sql.AbstractJDBCEnabledManager;
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

/**
 * A JDBC based implementation of the {@link ISMPServiceInformationManager}
 * interface.
 *
 * @author Philip Helger
 * @since 5.3.0
 */
public final class SMPServiceInformationManagerJDBC extends AbstractJDBCEnabledManager implements ISMPServiceInformationManager
{
  @MustImplementEqualsAndHashcode
  private static final class DocTypeAndExtension
  {
    final IDocumentTypeIdentifier m_aDocTypeID;
    final String m_sExt;

    public DocTypeAndExtension (final IDocumentTypeIdentifier aDocTypeID, final String sExt)
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

  public SMPServiceInformationManagerJDBC (@Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
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

    final ESuccess eSuccess = executor ().performInTransaction ( () -> {
      // Simply delete the old one
      final EChange eDeleted = _deleteSMPServiceInformationNoCallback (aSMPServiceInformation);
      aUpdated.set (eDeleted.isChanged ());

      // Insert new processes
      final IParticipantIdentifier aPID = aSMPServiceInformation.getServiceGroup ().getParticpantIdentifier ();
      final IDocumentTypeIdentifier aDocTypeID = aSMPServiceInformation.getDocumentTypeIdentifier ();

      for (final ISMPProcess aProcess : aSMPServiceInformation.getAllProcesses ())
      {
        final IProcessIdentifier aProcessID = aProcess.getProcessIdentifier ();
        executor ().insertOrUpdateOrDelete ("INSERT INTO smp_process (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, processIdentifierType, processIdentifier, extension) VALUES (?, ?, ?, ?, ?, ?, ?)",
                                            new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                       aPID.getValue (),
                                                                                       aDocTypeID.getScheme (),
                                                                                       aDocTypeID.getValue (),
                                                                                       aProcessID.getScheme (),
                                                                                       aProcessID.getValue (),
                                                                                       aProcess.getExtensionsAsString ()));
        // Insert new endpoints
        for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
        {
          executor ().insertOrUpdateOrDelete ("INSERT INTO smp_endpoint (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, processIdentifierType, processIdentifier," +
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
                                                                                         toTimestamp (aEndpoint.getServiceActivationDateTime ()),
                                                                                         aEndpoint.getServiceDescription (),
                                                                                         toTimestamp (aEndpoint.getServiceExpirationDateTime ()),
                                                                                         aEndpoint.getTechnicalContactUrl (),
                                                                                         aEndpoint.getTechnicalInformationUrl (),
                                                                                         aEndpoint.getTransportProfile (),
                                                                                         aEndpoint.getExtensionsAsString ()));
        }
      }
    });
    if (eSuccess.isFailure ())
      return ESuccess.FAILURE;

    // Callback outside of transaction
    if (aUpdated.booleanValue ())
      m_aCBs.forEach (x -> x.onSMPServiceInformationUpdated (aSMPServiceInformation));
    else
      m_aCBs.forEach (x -> x.onSMPServiceInformationCreated (aSMPServiceInformation));

    return ESuccess.SUCCESS;
  }

  @Nullable
  public ISMPServiceInformation findServiceInformation (@Nullable final ISMPServiceGroup aServiceGroup,
                                                        @Nullable final IDocumentTypeIdentifier aDocTypeID,
                                                        @Nullable final IProcessIdentifier aProcessID,
                                                        @Nullable final ISMPTransportProfile aTransportProfile)
  {
    final ISMPServiceInformation aServiceInfo = getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDocTypeID);
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
    final Wrapper <EChange> ret = new Wrapper <> (EChange.UNCHANGED);
    final ESuccess eSuccess = executor ().performInTransaction ( () -> {
      final IParticipantIdentifier aPID = aSMPServiceInformation.getServiceGroup ().getParticpantIdentifier ();
      final IDocumentTypeIdentifier aDocTypeID = aSMPServiceInformation.getDocumentTypeIdentifier ();
      final long nCountEP = executor ().insertOrUpdateOrDelete ("DELETE FROM smp_endpoint AS se" +
                                                                " WHERE se.businessIdentifierScheme=? AND se.businessIdentifier=? AND se.documentIdentifierScheme=? AND se.documentIdentifier=?",
                                                                new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                           aPID.getValue (),
                                                                                                           aDocTypeID.getScheme (),
                                                                                                           aDocTypeID.getValue ()));
      final long nCountProc = executor ().insertOrUpdateOrDelete ("DELETE FROM smp_process AS sp" +
                                                                  " WHERE sp.businessIdentifierScheme=? AND sp.businessIdentifier=? AND sp.documentIdentifierScheme=? AND sp.documentIdentifier=?",
                                                                  new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                             aPID.getValue (),
                                                                                                             aDocTypeID.getScheme (),
                                                                                                             aDocTypeID.getValue ()));
      ret.set (EChange.valueOf (nCountEP > 0 || nCountProc > 0));
    });
    if (eSuccess.isFailure ())
      return EChange.UNCHANGED;
    return ret.get ();
  }

  @Nonnull
  public EChange deleteSMPServiceInformation (@Nullable final ISMPServiceInformation aSMPServiceInformation)
  {
    if (aSMPServiceInformation == null)
      return EChange.UNCHANGED;

    // Main deletion
    if (_deleteSMPServiceInformationNoCallback (aSMPServiceInformation).isUnchanged ())
      return EChange.UNCHANGED;

    // Callback outside of transaction
    m_aCBs.forEach (x -> x.onSMPServiceInformationDeleted (aSMPServiceInformation));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    if (aServiceGroup == null)
      return EChange.UNCHANGED;

    final Wrapper <EChange> ret = new Wrapper <> (EChange.UNCHANGED);
    final Wrapper <ICommonsList <ISMPServiceInformation>> aAllDeleted = new Wrapper <> ();
    final ESuccess eSuccess = executor ().performInTransaction ( () -> {
      // get the old ones first
      aAllDeleted.set (getAllSMPServiceInformationOfServiceGroup (aServiceGroup));

      final IParticipantIdentifier aPID = aServiceGroup.getParticpantIdentifier ();
      final long nCountEP = executor ().insertOrUpdateOrDelete ("DELETE FROM smp_endpoint AS se" +
                                                                " WHERE se.businessIdentifierScheme=? AND se.businessIdentifier=?",
                                                                new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                           aPID.getValue ()));
      final long nCountProc = executor ().insertOrUpdateOrDelete ("DELETE FROM smp_process AS sp" +
                                                                  " WHERE sp.businessIdentifierScheme=? AND sp.businessIdentifier=?",
                                                                  new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                             aPID.getValue ()));
      final long nCountSM = executor ().insertOrUpdateOrDelete ("DELETE FROM smp_service_metadata AS sm" +
                                                                " WHERE sm.businessIdentifierScheme=? AND sm.businessIdentifier=?",
                                                                new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                           aPID.getValue ()));
      ret.set (EChange.valueOf (nCountEP > 0 || nCountProc > 0 || nCountSM > 0));
    });
    if (eSuccess.isFailure () || ret.get ().isUnchanged ())
      return EChange.UNCHANGED;

    // Callback outside of transaction
    if (aAllDeleted.isSet ())
      for (final ISMPServiceInformation aSMPServiceInformation : aAllDeleted.get ())
        m_aCBs.forEach (x -> x.onSMPServiceInformationDeleted (aSMPServiceInformation));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteSMPProcess (@Nullable final ISMPServiceInformation aSMPServiceInformation, @Nullable final ISMPProcess aProcess)
  {
    if (aSMPServiceInformation == null || aProcess == null)
      return EChange.UNCHANGED;

    final Wrapper <EChange> ret = new Wrapper <> (EChange.UNCHANGED);
    final ESuccess eSuccess = executor ().performInTransaction ( () -> {
      final IParticipantIdentifier aPID = aSMPServiceInformation.getServiceGroup ().getParticpantIdentifier ();
      final IDocumentTypeIdentifier aDocTypeID = aSMPServiceInformation.getDocumentTypeIdentifier ();
      final IProcessIdentifier aProcessID = aProcess.getProcessIdentifier ();
      final long nCountEP = executor ().insertOrUpdateOrDelete ("DELETE FROM smp_endpoint AS se" +
                                                                " WHERE se.businessIdentifierScheme=? AND se.businessIdentifier=? AND se.documentIdentifierScheme=? AND se.documentIdentifier=? AND se.processIdentifierType=? AND se.processIdentifier=?",
                                                                new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                           aPID.getValue (),
                                                                                                           aDocTypeID.getScheme (),
                                                                                                           aDocTypeID.getValue (),
                                                                                                           aProcessID.getScheme (),
                                                                                                           aProcessID.getValue ()));
      final long nCountProc = executor ().insertOrUpdateOrDelete ("DELETE FROM smp_process AS sp" +
                                                                  " WHERE sp.businessIdentifierScheme=? AND sp.businessIdentifier=? AND sp.documentIdentifierScheme=? AND sp.documentIdentifier=? AND sp.processIdentifierType=? AND sp.processIdentifier=?",
                                                                  new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                             aPID.getValue (),
                                                                                                             aDocTypeID.getScheme (),
                                                                                                             aDocTypeID.getValue (),
                                                                                                             aProcessID.getScheme (),
                                                                                                             aProcessID.getValue ()));
      ret.set (EChange.valueOf (nCountEP > 0 || nCountProc > 0));
    });
    if (eSuccess.isFailure ())
      return EChange.UNCHANGED;

    return ret.get ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformation ()
  {
    final ICommonsList <ISMPServiceInformation> ret = new CommonsArrayList <> ();
    final Optional <ICommonsList <DBResultRow>> aDBResult = executor ().queryAll ("SELECT sm.businessIdentifierScheme, sm.businessIdentifier, sm.documentIdentifierScheme, sm.documentIdentifier, sm.extension," +
                                                                                  "   sp.processIdentifierType, sp.processIdentifier, sp.extension," +
                                                                                  "   se.transportProfile, se.endpointReference, se.requireBusinessLevelSignature, se.minimumAuthenticationLevel," +
                                                                                  "     se.serviceActivationDate, se.serviceExpirationDate, se.certificate, se.serviceDescription," +
                                                                                  "     se.technicalContactUrl, se.technicalInformationUrl, se.extension" +
                                                                                  " FROM smp_service_metadata AS sm" +
                                                                                  " LEFT JOIN smp_process AS sp" +
                                                                                  "   ON sm.businessIdentifierScheme=sp.businessIdentifierScheme AND sm.businessIdentifier=sp.businessIdentifier" +
                                                                                  "   AND sm.documentIdentifierScheme=sp.documentIdentifierScheme AND sm.documentIdentifier=sp.documentIdentifier" +
                                                                                  " LEFT JOIN smp_endpoint AS se" +
                                                                                  "   ON sp.businessIdentifierScheme=se.businessIdentifierScheme AND sp.businessIdentifier=se.businessIdentifier" +
                                                                                  "   AND sp.documentIdentifierScheme=se.documentIdentifierScheme AND sp.documentIdentifier=se.documentIdentifier" +
                                                                                  "   AND sp.processIdentifierType=se.processIdentifierType AND sp.processIdentifier=se.processIdentifier");

    final ICommonsMap <IParticipantIdentifier, ICommonsMap <DocTypeAndExtension, ICommonsMap <SMPProcess, ICommonsList <SMPEndpoint>>>> aGrouping = new CommonsHashMap <> ();
    for (final DBResultRow aDBRow : aDBResult.get ())
    {
      // Participant ID
      final IParticipantIdentifier aParticipantID = new SimpleParticipantIdentifier (aDBRow.getAsString (0), aDBRow.getAsString (1));
      // Document type ID and extension
      final IDocumentTypeIdentifier aDocTypeID = new SimpleDocumentTypeIdentifier (aDBRow.getAsString (2), aDBRow.getAsString (3));
      final String sServiceInformationExtension = aDBRow.getAsString (4);
      // Process without endpoints
      final SMPProcess aProcess = new SMPProcess (new SimpleProcessIdentifier (aDBRow.getAsString (5), aDBRow.getAsString (6)),
                                                  null,
                                                  aDBRow.getAsString (7));
      // Don't add endpoint to process, because that impacts
      // SMPProcess.equals/hashcode
      final SMPEndpoint aEndpoint = new SMPEndpoint (aDBRow.getAsString (8),
                                                     aDBRow.getAsString (9),
                                                     aDBRow.getAsBoolean (10),
                                                     aDBRow.getAsString (11),
                                                     aDBRow.getAsLocalDateTime (12),
                                                     aDBRow.getAsLocalDateTime (13),
                                                     aDBRow.getAsString (14),
                                                     aDBRow.getAsString (15),
                                                     aDBRow.getAsString (16),
                                                     aDBRow.getAsString (17),
                                                     aDBRow.getAsString (18));
      aGrouping.computeIfAbsent (aParticipantID, k -> new CommonsHashMap <> ())
               .computeIfAbsent (new DocTypeAndExtension (aDocTypeID, sServiceInformationExtension), k -> new CommonsHashMap <> ())
               .computeIfAbsent (aProcess, k -> new CommonsArrayList <> ())
               .add (aEndpoint);
    }

    // Per participant ID
    for (final Map.Entry <IParticipantIdentifier, ICommonsMap <DocTypeAndExtension, ICommonsMap <SMPProcess, ICommonsList <SMPEndpoint>>>> aEntry : aGrouping.entrySet ())
    {
      final ISMPServiceGroup aServiceGroup = m_aServiceGroupMgr.getSMPServiceGroupOfID (aEntry.getKey ());
      if (aServiceGroup == null)
        throw new IllegalStateException ("Failed to resolve service group for particpant ID '" + aEntry.getKey ().getURIEncoded () + "'");

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
        ret.add (new SMPServiceInformation (aServiceGroup, aDE.m_aDocTypeID, aProcesses, aDE.m_sExt));
      }
    }

    return ret;
  }

  @Nonnegative
  public long getSMPServiceInformationCount ()
  {
    return executor ().queryCount ("SELECT COUNT(*) FROM smp_service_metadata");
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceInformation> getAllSMPServiceInformationOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    final ICommonsList <ISMPServiceInformation> ret = new CommonsArrayList <> ();
    if (aServiceGroup != null)
    {
      final IParticipantIdentifier aPID = aServiceGroup.getParticpantIdentifier ();
      final Optional <ICommonsList <DBResultRow>> aDBResult = executor ().queryAll ("SELECT sm.documentIdentifierScheme, sm.documentIdentifier, sm.extension," +
                                                                                    "   sp.processIdentifierType, sp.processIdentifier, sp.extension," +
                                                                                    "   se.transportProfile, se.endpointReference, se.requireBusinessLevelSignature, se.minimumAuthenticationLevel," +
                                                                                    "     se.serviceActivationDate, se.serviceExpirationDate, se.certificate, se.serviceDescription," +
                                                                                    "     se.technicalContactUrl, se.technicalInformationUrl, se.extension" +
                                                                                    " FROM smp_service_metadata AS sm" +
                                                                                    " LEFT JOIN smp_process AS sp" +
                                                                                    "   ON sm.businessIdentifierScheme=sp.businessIdentifierScheme AND sm.businessIdentifier=sp.businessIdentifier" +
                                                                                    "   AND sm.documentIdentifierScheme=sp.documentIdentifierScheme AND sm.documentIdentifier=sp.documentIdentifier" +
                                                                                    " LEFT JOIN smp_endpoint AS se" +
                                                                                    "   ON sp.businessIdentifierScheme=se.businessIdentifierScheme AND sp.businessIdentifier=se.businessIdentifier" +
                                                                                    "   AND sp.documentIdentifierScheme=se.documentIdentifierScheme AND sp.documentIdentifier=se.documentIdentifier" +
                                                                                    "   AND sp.processIdentifierType=se.processIdentifierType AND sp.processIdentifier=se.processIdentifier" +
                                                                                    " WHERE sm.businessIdentifierScheme=? AND sm.businessIdentifier=?",
                                                                                    new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                                               aPID.getValue ()));
      if (aDBResult.isPresent ())
      {
        final ICommonsMap <DocTypeAndExtension, ICommonsMap <SMPProcess, ICommonsList <SMPEndpoint>>> aGrouping = new CommonsHashMap <> ();
        for (final DBResultRow aDBRow : aDBResult.get ())
        {
          // Document type ID and extension
          final IDocumentTypeIdentifier aDocTypeID = new SimpleDocumentTypeIdentifier (aDBRow.getAsString (0), aDBRow.getAsString (1));
          final String sServiceInformationExtension = aDBRow.getAsString (2);
          // Process without endpoints
          final SMPProcess aProcess = new SMPProcess (new SimpleProcessIdentifier (aDBRow.getAsString (3), aDBRow.getAsString (4)),
                                                      null,
                                                      aDBRow.getAsString (5));
          // Don't add endpoint to process, because that impacts
          // SMPProcess.equals/hashcode
          final SMPEndpoint aEndpoint = new SMPEndpoint (aDBRow.getAsString (6),
                                                         aDBRow.getAsString (7),
                                                         aDBRow.getAsBoolean (8),
                                                         aDBRow.getAsString (9),
                                                         aDBRow.getAsLocalDateTime (10),
                                                         aDBRow.getAsLocalDateTime (11),
                                                         aDBRow.getAsString (12),
                                                         aDBRow.getAsString (13),
                                                         aDBRow.getAsString (14),
                                                         aDBRow.getAsString (15),
                                                         aDBRow.getAsString (16));
          aGrouping.computeIfAbsent (new DocTypeAndExtension (aDocTypeID, sServiceInformationExtension), k -> new CommonsHashMap <> ())
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
      final Optional <ICommonsList <DBResultRow>> aDBResult = executor ().queryAll ("SELECT sm.documentIdentifierScheme, sm.documentIdentifier" +
                                                                                    " FROM smp_service_metadata AS sm" +
                                                                                    " WHERE sm.businessIdentifierScheme=? AND sm.businessIdentifier=?");
      if (aDBResult.isPresent ())
      {
        final ICommonsList <DBResultRow> aRows = aDBResult.get ();
        for (final DBResultRow aRow : aRows)
          ret.add (new SimpleDocumentTypeIdentifier (aRow.getAsString (0), aRow.getAsString (1)));
      }
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

    final IParticipantIdentifier aPID = aServiceGroup.getParticpantIdentifier ();
    final Optional <ICommonsList <DBResultRow>> aDBResult = executor ().queryAll ("SELECT sm.extension," +
                                                                                  "   sp.processIdentifierType, sp.processIdentifier, sp.extension," +
                                                                                  "   se.transportProfile, se.endpointReference, se.requireBusinessLevelSignature, se.minimumAuthenticationLevel," +
                                                                                  "     se.serviceActivationDate, se.serviceExpirationDate, se.certificate, se.serviceDescription," +
                                                                                  "     se.technicalContactUrl, se.technicalInformationUrl, se.extension" +
                                                                                  " FROM smp_service_metadata AS sm" +
                                                                                  " LEFT JOIN smp_process AS sp" +
                                                                                  "   ON sm.businessIdentifierScheme=sp.businessIdentifierScheme AND sm.businessIdentifier=sp.businessIdentifier" +
                                                                                  "   AND sm.documentIdentifierScheme=sp.documentIdentifierScheme AND sm.documentIdentifier=sp.documentIdentifier" +
                                                                                  " LEFT JOIN smp_endpoint AS se" +
                                                                                  "   ON sp.businessIdentifierScheme=se.businessIdentifierScheme AND sp.businessIdentifier=se.businessIdentifier" +
                                                                                  "   AND sp.documentIdentifierScheme=se.documentIdentifierScheme AND sp.documentIdentifier=se.documentIdentifier" +
                                                                                  "   AND sp.processIdentifierType=se.processIdentifierType AND sp.processIdentifier=se.processIdentifier" +
                                                                                  " WHERE sm.businessIdentifierScheme=? AND sm.businessIdentifier=? AND sm.documentIdentifierScheme=? AND sm.documentIdentifier=?",
                                                                                  new ConstantPreparedStatementDataProvider (aPID.getScheme (),
                                                                                                                             aPID.getValue (),
                                                                                                                             aDocTypeID.getScheme (),
                                                                                                                             aDocTypeID.getValue ()));
    if (aDBResult.isPresent ())
    {
      final ICommonsList <DBResultRow> aRows = aDBResult.get ();
      if (aRows.isNotEmpty ())
      {
        final String sServiceInformationExtension = aRows.getFirst ().getAsString (0);

        final ICommonsMap <SMPProcess, ICommonsList <SMPEndpoint>> aEndpoints = new CommonsHashMap <> ();
        for (final DBResultRow aDBRow : aRows)
        {
          // Process without endpoints as key
          final SMPProcess aProcess = new SMPProcess (new SimpleProcessIdentifier (aDBRow.getAsString (1), aDBRow.getAsString (2)),
                                                      null,
                                                      aDBRow.getAsString (3));
          final SMPEndpoint aEndpoint = new SMPEndpoint (aDBRow.getAsString (4),
                                                         aDBRow.getAsString (5),
                                                         aDBRow.getAsBoolean (6),
                                                         aDBRow.getAsString (7),
                                                         aDBRow.getAsLocalDateTime (8),
                                                         aDBRow.getAsLocalDateTime (9),
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
    }
    return null;
  }

  public boolean containsAnyEndpointWithTransportProfile (@Nullable final String sTransportProfileID)
  {
    if (StringHelper.hasNoText (sTransportProfileID))
      return false;

    final long nCount = executor ().queryCount ("SELECT COUNT(*) FROM smp_endpoint WHERE transportProfile=?",
                                                new ConstantPreparedStatementDataProvider (sTransportProfileID));
    return nCount > 0;
  }
}