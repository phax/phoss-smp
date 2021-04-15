/**
 * Copyright (C) 2019-2021 Philip Helger and contributors
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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ELockType;
import com.helger.commons.annotation.IsLocked;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectCallback;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.redirect.SMPRedirect;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
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
  private final ISMPServiceGroupManager m_aServiceGroupMgr;
  private final CallbackList <ISMPRedirectCallback> m_aCallbacks = new CallbackList <> ();

  public SMPRedirectManagerMongoDB (@Nonnull final IIdentifierFactory aIdentifierFactory,
                                    @Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    super ("smp-redirect");
    m_aIdentifierFactory = aIdentifierFactory;
    m_aServiceGroupMgr = aServiceGroupMgr;
    getCollection ().createIndex (Indexes.ascending (BSON_ID));
  }

  @Nonnull
  @ReturnsMutableObject
  public CallbackList <ISMPRedirectCallback> redirectCallbacks ()
  {
    return m_aCallbacks;
  }

  @Nonnull
  @ReturnsMutableCopy
  public static Document toBson (@Nonnull final ISMPRedirect aValue)
  {
    final Document ret = new Document ().append (BSON_ID, aValue.getID ())
                                        .append (BSON_SERVICE_GROUP_ID, aValue.getServiceGroupID ())
                                        .append (BSON_DOCTYPE_ID, toBson (aValue.getDocumentTypeIdentifier ()))
                                        .append (BSON_TARGET_HREF, aValue.getTargetHref ())
                                        .append (BSON_TARGET_SUBJECT_CN, aValue.getSubjectUniqueIdentifier ());
    if (aValue.hasCertificate ())
      ret.append (BSON_TARGET_CERTIFICATE, CertificateHelper.getPEMEncodedCertificate (aValue.getCertificate ()));
    if (aValue.extensions ().isNotEmpty ())
      ret.append (BSON_EXTENSIONS, aValue.getExtensionsAsString ());
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public static SMPRedirect toDomain (@Nonnull final IIdentifierFactory aIdentifierFactory,
                                      @Nonnull final ISMPServiceGroupManager aServiceGroupMgr,
                                      @Nonnull final Document aDoc)
  {
    // The ID itself is derived from ServiceGroupID and DocTypeID
    final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aIdentifierFactory.parseParticipantIdentifier (aDoc.getString (BSON_SERVICE_GROUP_ID)));
    final IDocumentTypeIdentifier aDocTypeID = toDocumentTypeID (aDoc.get (BSON_DOCTYPE_ID, Document.class));
    final X509Certificate aCert = CertificateHelper.convertStringToCertficateOrNull (aDoc.getString (BSON_TARGET_CERTIFICATE));
    return new SMPRedirect (aServiceGroup,
                            aDocTypeID,
                            aDoc.getString (BSON_TARGET_HREF),
                            aDoc.getString (BSON_TARGET_SUBJECT_CN),
                            aCert,
                            aDoc.getString (BSON_EXTENSIONS));
  }

  @Nonnull
  @ReturnsMutableCopy
  public SMPRedirect toDomain (@Nonnull final Document aDoc)
  {
    return toDomain (m_aIdentifierFactory, m_aServiceGroupMgr, aDoc);
  }

  @Nonnull
  @IsLocked (ELockType.WRITE)
  private ISMPRedirect _createSMPRedirect (@Nonnull final SMPRedirect aSMPRedirect)
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
                                      aSMPRedirect.getExtensionsAsString ());
    return aSMPRedirect;
  }

  @IsLocked (ELockType.WRITE)
  private void _updateSMPRedirect (@Nonnull final SMPRedirect aSMPRedirect)
  {
    // ServiceGroup and DocType are never changed -> therefore the ID is never
    // changed
    final Document aOldDoc = getCollection ().findOneAndReplace (new Document (BSON_ID, aSMPRedirect.getID ()), toBson (aSMPRedirect));
    if (aOldDoc != null)
      AuditHelper.onAuditModifySuccess (SMPRedirect.OT,
                                        aSMPRedirect.getID (),
                                        aSMPRedirect.getServiceGroupID (),
                                        aSMPRedirect.getDocumentTypeIdentifier ().getURIEncoded (),
                                        aSMPRedirect.getTargetHref (),
                                        aSMPRedirect.getSubjectUniqueIdentifier (),
                                        aSMPRedirect.getCertificate (),
                                        aSMPRedirect.getExtensionsAsString ());
  }

  @Nonnull
  public ISMPRedirect createOrUpdateSMPRedirect (@Nonnull final ISMPServiceGroup aServiceGroup,
                                                 @Nonnull final IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                                 @Nonnull @Nonempty final String sTargetHref,
                                                 @Nonnull @Nonempty final String sSubjectUniqueIdentifier,
                                                 @Nullable final X509Certificate aCertificate,
                                                 @Nullable final String sExtension)
  {
    ValueEnforcer.notNull (aServiceGroup, "ServiceGroup");
    ValueEnforcer.notNull (aDocumentTypeIdentifier, "DocumentTypeIdentifier");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createOrUpdateSMPRedirect (" +
                    aServiceGroup +
                    ", " +
                    aDocumentTypeIdentifier +
                    ", " +
                    sTargetHref +
                    ", " +
                    sSubjectUniqueIdentifier +
                    ", " +
                    aCertificate +
                    ", " +
                    (StringHelper.hasText (sExtension) ? "with extension" : "without extension") +
                    ")");

    final ISMPRedirect aOldRedirect = getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup, aDocumentTypeIdentifier);
    final SMPRedirect aNewRedirect = new SMPRedirect (aServiceGroup,
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

  @Nonnull
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
      AuditHelper.onAuditDeleteFailure (SMPRedirect.OT, "no-such-id", aSMPRedirect.getID ());
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

  @Nonnull
  public EChange deleteAllSMPRedirectsOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    if (aServiceGroup == null)
      return EChange.UNCHANGED;

    EChange eChange = EChange.UNCHANGED;
    for (final ISMPRedirect aRedirect : getAllSMPRedirectsOfServiceGroup (aServiceGroup.getID ()))
      eChange = eChange.or (deleteSMPRedirect (aRedirect));
    return eChange;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPRedirect> getAllSMPRedirects ()
  {
    final ICommonsList <ISMPRedirect> ret = new CommonsArrayList <> ();
    getCollection ().find ().forEach (x -> ret.add (toDomain (x)));
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPRedirect> getAllSMPRedirectsOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    return getAllSMPRedirectsOfServiceGroup (aServiceGroup == null ? null : aServiceGroup.getID ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPRedirect> getAllSMPRedirectsOfServiceGroup (@Nullable final String sServiceGroupID)
  {
    final ICommonsList <ISMPRedirect> ret = new CommonsArrayList <> ();
    if (StringHelper.hasText (sServiceGroupID))
      getCollection ().find (new Document (BSON_SERVICE_GROUP_ID, sServiceGroupID)).forEach (x -> ret.add (toDomain (x)));
    return ret;
  }

  @Nonnegative
  public long getSMPRedirectCount ()
  {
    return getCollection ().countDocuments ();
  }

  @Nullable
  public ISMPRedirect getSMPRedirectOfServiceGroupAndDocumentType (@Nullable final ISMPServiceGroup aServiceGroup,
                                                                   @Nullable final IDocumentTypeIdentifier aDocTypeID)
  {
    if (aServiceGroup == null)
      return null;
    if (aDocTypeID == null)
      return null;

    final Document aMatch = getCollection ().find (Filters.and (new Document (BSON_SERVICE_GROUP_ID, aServiceGroup.getID ()),
                                                                new Document (BSON_DOCTYPE_ID, toBson (aDocTypeID))))
                                            .first ();
    if (aMatch == null)
      return null;
    return toDomain (aMatch);
  }
}
