/*
 * Copyright (C) 2019-2025 Philip Helger and contributors
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

import java.security.cert.X509Certificate;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.numeric.mutable.MutableBoolean;
import com.helger.base.state.EChange;
import com.helger.base.state.ESuccess;
import com.helger.base.wrapper.Wrapper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.db.jdbc.mgr.AbstractJDBCEnabledManager;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.simple.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectCallback;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.redirect.SMPRedirect;
import com.helger.photon.audit.AuditHelper;
import com.helger.security.certificate.CertificateDecodeHelper;
import com.helger.security.certificate.CertificateHelper;

/**
 * A JDBC based implementation of the {@link ISMPRedirectManager} interface.
 *
 * @author Philip Helger
 * @since 9.2.4
 */
public final class SMPRedirectManagerJDBC extends AbstractJDBCEnabledManager implements ISMPRedirectManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPRedirectManagerJDBC.class);

  private final String m_sTableName;
  private final CallbackList <ISMPRedirectCallback> m_aCallbacks = new CallbackList <> ();

  /**
   * Constructor
   *
   * @param aDBExecSupplier
   *        The supplier for {@link DBExecutor} objects. May not be <code>null</code>.
   * @param sTableNamePrefix
   *        The table name prefix to be used. May not be <code>null</code>.
   */
  public SMPRedirectManagerJDBC (@NonNull final Supplier <? extends DBExecutor> aDBExecSupplier,
                                 @NonNull final String sTableNamePrefix)
  {
    super (aDBExecSupplier);
    ValueEnforcer.notNull (sTableNamePrefix, "TableNamePrefix");
    m_sTableName = sTableNamePrefix + "smp_service_metadata_red";
  }

  @NonNull
  @ReturnsMutableObject
  public CallbackList <ISMPRedirectCallback> redirectCallbacks ()
  {
    return m_aCallbacks;
  }

  @Nullable
  public ISMPRedirect createOrUpdateSMPRedirect (@NonNull final IParticipantIdentifier aParticipantIdentifier,
                                                 @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                                 @NonNull @Nonempty final String sRedirectUrl,
                                                 @NonNull @Nonempty final String sSubjectUniqueIdentifier,
                                                 @Nullable final X509Certificate aCertificate,
                                                 @Nullable final String sExtension)
  {
    ValueEnforcer.notNull (aParticipantIdentifier, "ParticipantIdentifier");
    ValueEnforcer.notNull (aDocTypeID, "DocumentTypeIdentifier");

    final MutableBoolean aCreatedNew = new MutableBoolean (true);

    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      final ISMPRedirect aDBRedirect = getSMPRedirectOfServiceGroupAndDocumentType (aParticipantIdentifier, aDocTypeID);

      final String sCertificate = aCertificate == null ? null : CertificateHelper.getPEMEncodedCertificate (
                                                                                                            aCertificate);

      if (aDBRedirect == null)
      {
        // Create new
        final long nCreated = aExecutor.insertOrUpdateOrDelete ("INSERT INTO " +
                                                                m_sTableName +
                                                                " (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, redirectionUrl, certificateUID, certificate, extension) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                                                                new ConstantPreparedStatementDataProvider (aParticipantIdentifier.getScheme (),
                                                                                                           aParticipantIdentifier.getValue (),
                                                                                                           aDocTypeID.getScheme (),
                                                                                                           aDocTypeID.getValue (),
                                                                                                           sRedirectUrl,
                                                                                                           sSubjectUniqueIdentifier,
                                                                                                           sCertificate,
                                                                                                           sExtension));
        if (nCreated != 1)
          throw new IllegalStateException ("Failed to create new DB entry (" + nCreated + ")");
        aCreatedNew.set (true);
      }
      else
      {
        // Update existing
        final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE " +
                                                                m_sTableName +
                                                                " SET redirectionUrl=?, certificateUID=?, certificate=?, extension=?" +
                                                                " WHERE businessIdentifierScheme=? AND businessIdentifier=? AND documentIdentifierScheme=? AND documentIdentifier=?",
                                                                new ConstantPreparedStatementDataProvider (sRedirectUrl,
                                                                                                           sSubjectUniqueIdentifier,
                                                                                                           sCertificate,
                                                                                                           sExtension,
                                                                                                           aParticipantIdentifier.getScheme (),
                                                                                                           aParticipantIdentifier.getValue (),
                                                                                                           aDocTypeID.getScheme (),
                                                                                                           aDocTypeID.getValue ()));
        if (nUpdated != 1)
          throw new IllegalStateException ("Failed to update existing DB entry (" + nUpdated + ")");
        aCreatedNew.set (false);
      }
    });

    if (eSuccess.isFailure ())
    {
      return null;
    }

    final SMPRedirect aSMPRedirect = new SMPRedirect (aParticipantIdentifier,
                                                      aDocTypeID,
                                                      sRedirectUrl,
                                                      sSubjectUniqueIdentifier,
                                                      aCertificate,
                                                      sExtension);

    if (aCreatedNew.booleanValue ())
    {
      AuditHelper.onAuditCreateSuccess (SMPRedirect.OT,
                                        aSMPRedirect.getID (),
                                        aSMPRedirect.getServiceGroupID (),
                                        aSMPRedirect.getDocumentTypeIdentifier ().getURIEncoded (),
                                        aSMPRedirect.getTargetHref (),
                                        aSMPRedirect.getSubjectUniqueIdentifier (),
                                        aSMPRedirect.getCertificate (),
                                        aSMPRedirect.getExtensions ().getExtensionsAsJsonString ());

      m_aCallbacks.forEach (x -> x.onSMPRedirectCreated (aSMPRedirect));
    }
    else
    {
      AuditHelper.onAuditModifySuccess (SMPRedirect.OT,
                                        "set-all",
                                        aSMPRedirect.getID (),
                                        aSMPRedirect.getServiceGroupID (),
                                        aSMPRedirect.getDocumentTypeIdentifier ().getURIEncoded (),
                                        aSMPRedirect.getTargetHref (),
                                        aSMPRedirect.getSubjectUniqueIdentifier (),
                                        aSMPRedirect.getCertificate (),
                                        aSMPRedirect.getExtensions ().getExtensionsAsJsonString ());

      m_aCallbacks.forEach (x -> x.onSMPRedirectUpdated (aSMPRedirect));
    }
    return aSMPRedirect;
  }

  @NonNull
  public EChange deleteSMPRedirect (@Nullable final ISMPRedirect aSMPRedirect)
  {
    if (aSMPRedirect == null)
      return EChange.UNCHANGED;

    final IParticipantIdentifier aParticipantID = aSMPRedirect.getServiceGroupParticipantIdentifier ();
    final IDocumentTypeIdentifier aDocTypeID = aSMPRedirect.getDocumentTypeIdentifier ();
    final long nDeleted = newExecutor ().insertOrUpdateOrDelete ("DELETE FROM " +
                                                                 m_sTableName +
                                                                 " WHERE businessIdentifierScheme=? AND businessIdentifier=? AND documentIdentifierScheme=? and documentIdentifier=?",
                                                                 new ConstantPreparedStatementDataProvider (aParticipantID.getScheme (),
                                                                                                            aParticipantID.getValue (),
                                                                                                            aDocTypeID.getScheme (),
                                                                                                            aDocTypeID.getValue ()));
    if (nDeleted == 0)
    {
      AuditHelper.onAuditDeleteFailure (SMPRedirect.OT, aSMPRedirect.getID (), "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditDeleteSuccess (SMPRedirect.OT,
                                      aSMPRedirect.getID (),
                                      aSMPRedirect.getServiceGroupID (),
                                      aSMPRedirect.getDocumentTypeIdentifier ().getURIEncoded ());
    m_aCallbacks.forEach (x -> x.onSMPRedirectDeleted (aSMPRedirect));
    return EChange.CHANGED;
  }

  @NonNull
  public EChange deleteAllSMPRedirectsOfServiceGroup (@Nullable final IParticipantIdentifier aParticipantID)
  {
    if (aParticipantID == null)
      return EChange.UNCHANGED;

    // Remember all existing
    final ICommonsList <ISMPRedirect> aDeletedRedirects = getAllSMPRedirectsOfServiceGroup (aParticipantID);

    // Now delete
    final long nDeleted = newExecutor ().insertOrUpdateOrDelete ("DELETE FROM " +
                                                                 m_sTableName +
                                                                 " WHERE businessIdentifierScheme=? AND businessIdentifier=?",
                                                                 new ConstantPreparedStatementDataProvider (aParticipantID.getScheme (),
                                                                                                            aParticipantID.getValue ()));
    if (nDeleted == 0)
    {
      return EChange.UNCHANGED;
    }

    // Callback only, if all were deleted
    if (nDeleted == aDeletedRedirects.size ())
    {
      for (final ISMPRedirect aSMPRedirect : aDeletedRedirects)
      {
        AuditHelper.onAuditDeleteSuccess (SMPRedirect.OT,
                                          aSMPRedirect.getID (),
                                          aSMPRedirect.getServiceGroupID (),
                                          aSMPRedirect.getDocumentTypeIdentifier ().getURIEncoded ());
        m_aCallbacks.forEach (x -> x.onSMPRedirectDeleted (aSMPRedirect));
      }
    }
    else
    {
      final String sMsg = nDeleted +
                          " SMP redirects were deleted, but " +
                          aDeletedRedirects.size () +
                          " were found previously. Because of this inconsistency, no callbacks are triggered";
      AuditHelper.onAuditDeleteFailure (SMPRedirect.OT, sMsg);
      LOGGER.warn (sMsg);
    }
    return EChange.CHANGED;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPRedirect> getAllSMPRedirects ()
  {
    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, redirectionUrl, certificateUID, certificate, extension" +
                                                                          " FROM " +
                                                                          m_sTableName);
    final ICommonsList <ISMPRedirect> ret = new CommonsArrayList <> ();
    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
      {
        final IParticipantIdentifier aParticipantID = new SimpleParticipantIdentifier (aRow.getAsString (0),
                                                                                       aRow.getAsString (1));
        final X509Certificate aCertificate = new CertificateDecodeHelper ().source (aRow.getAsString (6))
                                                                           .pemEncoded (true)
                                                                           .getDecodedOrNull ();
        ret.add (new SMPRedirect (aParticipantID,
                                  new SimpleDocumentTypeIdentifier (aRow.getAsString (2), aRow.getAsString (3)),
                                  aRow.getAsString (4),
                                  aRow.getAsString (5),
                                  aCertificate,
                                  aRow.getAsString (7)));
      }
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPRedirect> getAllSMPRedirectsOfServiceGroup (@Nullable final IParticipantIdentifier aParticipantID)
  {
    final ICommonsList <ISMPRedirect> ret = new CommonsArrayList <> ();
    if (aParticipantID != null)
    {
      final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT documentIdentifierScheme, documentIdentifier, redirectionUrl, certificateUID, certificate, extension" +
                                                                            " FROM " +
                                                                            m_sTableName +
                                                                            " WHERE businessIdentifierScheme=? AND businessIdentifier=?",
                                                                            new ConstantPreparedStatementDataProvider (aParticipantID.getScheme (),
                                                                                                                       aParticipantID.getValue ()));
      if (aDBResult != null)
        for (final DBResultRow aRow : aDBResult)
        {
          final X509Certificate aCertificate = new CertificateDecodeHelper ().source (aRow.getAsString (4))
                                                                             .pemEncoded (true)
                                                                             .getDecodedOrNull ();
          ret.add (new SMPRedirect (aParticipantID,
                                    new SimpleDocumentTypeIdentifier (aRow.getAsString (0), aRow.getAsString (1)),
                                    aRow.getAsString (2),
                                    aRow.getAsString (3),
                                    aCertificate,
                                    aRow.getAsString (5)));
        }
    }
    return ret;
  }

  @Nonnegative
  public long getSMPRedirectCount ()
  {
    return newExecutor ().queryCount ("SELECT COUNT(*) FROM " + m_sTableName);
  }

  @Nullable
  public ISMPRedirect getSMPRedirectOfServiceGroupAndDocumentType (@Nullable final IParticipantIdentifier aParticipantID,
                                                                   @Nullable final IDocumentTypeIdentifier aDocTypeID)
  {
    if (aParticipantID == null)
      return null;
    if (aDocTypeID == null)
      return null;

    final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
    newExecutor ().querySingle ("SELECT redirectionUrl, certificateUID, certificate, extension" +
                                " FROM " +
                                m_sTableName +
                                " WHERE businessIdentifierScheme=? AND businessIdentifier=? AND documentIdentifierScheme=? and documentIdentifier=?",
                                new ConstantPreparedStatementDataProvider (aParticipantID.getScheme (),
                                                                           aParticipantID.getValue (),
                                                                           aDocTypeID.getScheme (),
                                                                           aDocTypeID.getValue ()),
                                aDBResult::set);
    if (aDBResult.isNotSet ())
      return null;

    final DBResultRow aRow = aDBResult.get ();
    final X509Certificate aCertificate = new CertificateDecodeHelper ().source (aRow.getAsString (2))
                                                                       .pemEncoded (true)
                                                                       .getDecodedOrNull ();
    return new SMPRedirect (aParticipantID,
                            aDocTypeID,
                            aRow.getAsString (0),
                            aRow.getAsString (1),
                            aCertificate,
                            aRow.getAsString (3));
  }
}
