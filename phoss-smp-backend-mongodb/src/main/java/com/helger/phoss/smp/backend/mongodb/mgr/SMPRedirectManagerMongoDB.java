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
package com.helger.phoss.smp.backend.mongodb.mgr;

import java.security.cert.X509Certificate;

import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.ELockType;
import com.helger.annotation.concurrent.IsLocked;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectCallback;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.redirect.SMPRedirect;
import com.helger.photon.audit.AuditHelper;
import com.helger.security.certificate.CertificateHelper;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;

/**
 * Manager for all {@link SMPRedirect} objects.
 *
 * @author Philip Helger
 */
public final class SMPRedirectManagerMongoDB extends AbstractManagerMongoDB implements ISMPRedirectManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPRedirectManagerMongoDB.class);

  private static final String BSON_ID = "id";
  private static final String BSON_SERVICE_GROUP_ID = "sgid";
  private static final String BSON_DOCTYPE_ID = "doctypeid";
  private static final String BSON_TARGET_HREF = "target";
  private static final String BSON_TARGET_SUBJECT_CN = "subjectcn";
  private static final String BSON_TARGET_CERTIFICATE = "certificate";
  private static final String BSON_EXTENSIONS = "extensions";

  private final IIdentifierFactory m_aIdentifierFactory;
  private final CallbackList <ISMPRedirectCallback> m_aCallbacks = new CallbackList <> ();

  public SMPRedirectManagerMongoDB (@NonNull final IIdentifierFactory aIdentifierFactory)
  {
    super ("smp-redirect");
    m_aIdentifierFactory = aIdentifierFactory;
    getCollection ().createIndex (Indexes.ascending (BSON_ID));
  }

  @NonNull
  @ReturnsMutableObject
  public CallbackList <ISMPRedirectCallback> redirectCallbacks ()
  {
    return m_aCallbacks;
  }

  @NonNull
  @ReturnsMutableCopy
  public static Document toBson (@NonNull final ISMPRedirect aValue)
  {
    final Document ret = new Document ().append (BSON_ID, aValue.getID ())
                                        .append (BSON_SERVICE_GROUP_ID, aValue.getServiceGroupID ())
                                        .append (BSON_DOCTYPE_ID, toBson (aValue.getDocumentTypeIdentifier ()))
                                        .append (BSON_TARGET_HREF, aValue.getTargetHref ())
                                        .append (BSON_TARGET_SUBJECT_CN, aValue.getSubjectUniqueIdentifier ());
    if (aValue.hasCertificate ())
      ret.append (BSON_TARGET_CERTIFICATE, CertificateHelper.getPEMEncodedCertificate (aValue.getCertificate ()));
    if (aValue.getExtensions ().extensions ().isNotEmpty ())
      ret.append (BSON_EXTENSIONS, aValue.getExtensions ().getExtensionsAsJsonString ());
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public static SMPRedirect toDomain (@NonNull final IIdentifierFactory aIdentifierFactory,
                                      @NonNull final Document aDoc)
  {
    // The ID itself is derived from ServiceGroupID and DocTypeID
    final IParticipantIdentifier aParticipantIdentifier = aIdentifierFactory.parseParticipantIdentifier (aDoc.getString (BSON_SERVICE_GROUP_ID));
    final IDocumentTypeIdentifier aDocTypeID = toDocumentTypeID (aDoc.get (BSON_DOCTYPE_ID, Document.class));
    final X509Certificate aCert = CertificateHelper.convertStringToCertficateOrNull (aDoc.getString (BSON_TARGET_CERTIFICATE));
    return new SMPRedirect (aParticipantIdentifier,
                            aDocTypeID,
                            aDoc.getString (BSON_TARGET_HREF),
                            aDoc.getString (BSON_TARGET_SUBJECT_CN),
                            aCert,
                            aDoc.getString (BSON_EXTENSIONS));
  }

  @NonNull
  @ReturnsMutableCopy
  public SMPRedirect toDomain (@NonNull final Document aDoc)
  {
    return toDomain (m_aIdentifierFactory, aDoc);
  }

  @NonNull
  @IsLocked (ELockType.WRITE)
  private ISMPRedirect _createSMPRedirect (@NonNull final SMPRedirect aSMPRedirect)
  {
    if (!getCollection ().insertOne (toBson (aSMPRedirect)).wasAcknowledged ())
      throw new IllegalStateException ("Failed to insert into MongoDB Collection");

    AuditHelper.onAuditCreateSuccess (SMPRedirect.OT,
                                      aSMPRedirect.getID (),
                                      aSMPRedirect.getServiceGroupID (),
                                      aSMPRedirect.getDocumentTypeIdentifier ().getURIEncoded (),
                                      aSMPRedirect.getTargetHref (),
                                      aSMPRedirect.getSubjectUniqueIdentifier (),
                                      aSMPRedirect.getCertificate (),
                                      aSMPRedirect.getExtensions ().getExtensionsAsJsonString ());
    return aSMPRedirect;
  }

  @IsLocked (ELockType.WRITE)
  private void _updateSMPRedirect (@NonNull final SMPRedirect aSMPRedirect)
  {
    // ServiceGroup and DocType are never changed -> therefore the ID is never
    // changed
    final Document aOldDoc = getCollection ().findOneAndReplace (new Document (BSON_ID, aSMPRedirect.getID ()),
                                                                 toBson (aSMPRedirect));
    if (aOldDoc != null)
      AuditHelper.onAuditModifySuccess (SMPRedirect.OT,
                                        "set-all",
                                        aSMPRedirect.getID (),
                                        aSMPRedirect.getServiceGroupID (),
                                        aSMPRedirect.getDocumentTypeIdentifier ().getURIEncoded (),
                                        aSMPRedirect.getTargetHref (),
                                        aSMPRedirect.getSubjectUniqueIdentifier (),
                                        aSMPRedirect.getCertificate (),
                                        aSMPRedirect.getExtensions ().getExtensionsAsJsonString ());
  }

  @NonNull
  public ISMPRedirect createOrUpdateSMPRedirect (@NonNull final IParticipantIdentifier aParticipantID,
                                                 @NonNull final IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                                 @NonNull @Nonempty final String sTargetHref,
                                                 @NonNull @Nonempty final String sSubjectUniqueIdentifier,
                                                 @Nullable final X509Certificate aCertificate,
                                                 @Nullable final String sExtension)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aDocumentTypeIdentifier, "DocumentTypeIdentifier");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createOrUpdateSMPRedirect (" +
                    aParticipantID +
                    ", " +
                    aDocumentTypeIdentifier +
                    ", " +
                    sTargetHref +
                    ", " +
                    sSubjectUniqueIdentifier +
                    ", " +
                    aCertificate +
                    ", " +
                    (StringHelper.isNotEmpty (sExtension) ? "with extension" : "without extension") +
                    ")");

    final ISMPRedirect aOldRedirect = getSMPRedirectOfServiceGroupAndDocumentType (aParticipantID,
                                                                                   aDocumentTypeIdentifier);
    final SMPRedirect aNewRedirect = new SMPRedirect (aParticipantID,
                                                      aDocumentTypeIdentifier,
                                                      sTargetHref,
                                                      sSubjectUniqueIdentifier,
                                                      aCertificate,
                                                      sExtension);
    if (aOldRedirect == null)
    {
      // Create new ID
      _createSMPRedirect (aNewRedirect);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("createOrUpdateSMPRedirect - success - created");

      m_aCallbacks.forEach (x -> x.onSMPRedirectCreated (aNewRedirect));
    }
    else
    {
      // Reuse old ID
      _updateSMPRedirect (aNewRedirect);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("createOrUpdateSMPRedirect - success - updated");

      m_aCallbacks.forEach (x -> x.onSMPRedirectUpdated (aNewRedirect));
    }
    return aNewRedirect;
  }

  @NonNull
  public EChange deleteSMPRedirect (@Nullable final ISMPRedirect aSMPRedirect)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPRedirect (" + aSMPRedirect + ")");

    if (aSMPRedirect == null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("deleteSMPRedirect - failure");
      return EChange.UNCHANGED;
    }

    final DeleteResult aDR = getCollection ().deleteOne (new Document (BSON_ID, aSMPRedirect.getID ()));
    if (!aDR.wasAcknowledged () || aDR.getDeletedCount () == 0)
    {
      AuditHelper.onAuditDeleteFailure (SMPRedirect.OT, aSMPRedirect.getID (), "no-such-id");
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("deleteSMPRedirect - failure");
      return EChange.UNCHANGED;
    }

    m_aCallbacks.forEach (x -> x.onSMPRedirectDeleted (aSMPRedirect));

    AuditHelper.onAuditDeleteSuccess (SMPRedirect.OT,
                                      aSMPRedirect.getID (),
                                      aSMPRedirect.getServiceGroupID (),
                                      aSMPRedirect.getDocumentTypeIdentifier ().getURIEncoded ());
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPRedirect - success");
    return EChange.CHANGED;
  }

  @NonNull
  public EChange deleteAllSMPRedirectsOfServiceGroup (@Nullable final IParticipantIdentifier aParticipantID)
  {
    if (aParticipantID == null)
      return EChange.UNCHANGED;

    EChange eChange = EChange.UNCHANGED;
    for (final ISMPRedirect aRedirect : getAllSMPRedirectsOfServiceGroup (aParticipantID))
      eChange = eChange.or (deleteSMPRedirect (aRedirect));
    return eChange;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPRedirect> getAllSMPRedirects ()
  {
    final ICommonsList <ISMPRedirect> ret = new CommonsArrayList <> ();
    getCollection ().find ().forEach (x -> ret.add (toDomain (x)));
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPRedirect> getAllSMPRedirectsOfServiceGroup (@Nullable final IParticipantIdentifier aParticipantID)
  {
    return getAllSMPRedirectsOfServiceGroup (aParticipantID == null ? null : aParticipantID.getURIEncoded ());
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPRedirect> getAllSMPRedirectsOfServiceGroup (@Nullable final String sServiceGroupID)
  {
    final ICommonsList <ISMPRedirect> ret = new CommonsArrayList <> ();
    if (StringHelper.isNotEmpty (sServiceGroupID))
      getCollection ().find (new Document (BSON_SERVICE_GROUP_ID, sServiceGroupID))
                      .forEach (x -> ret.add (toDomain (x)));
    return ret;
  }

  @Nonnegative
  public long getSMPRedirectCount ()
  {
    return getCollection ().countDocuments ();
  }

  @Nullable
  public ISMPRedirect getSMPRedirectOfServiceGroupAndDocumentType (@Nullable final IParticipantIdentifier aParticipantID,
                                                                   @Nullable final IDocumentTypeIdentifier aDocTypeID)
  {
    if (aParticipantID == null)
      return null;
    if (aDocTypeID == null)
      return null;

    final Document aMatch = getCollection ().find (Filters.and (new Document (BSON_SERVICE_GROUP_ID,
                                                                              aParticipantID.getURIEncoded ()),
                                                                new Document (BSON_DOCTYPE_ID, toBson (aDocTypeID))))
                                            .first ();
    if (aMatch == null)
      return null;
    return toDomain (aMatch);
  }
}
