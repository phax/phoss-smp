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
package com.helger.phoss.smp.restapi.cache;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * The key of a single cached REST API response. Each key belongs to exactly one participant, so
 * that all cached responses of a participant can be invalidated at once.
 *
 * @author Philip Helger
 * @since 8.6.1
 */
@Immutable
public final class SMPRestResponseCacheKey
{
  /**
   * The type of the cached response.
   */
  public enum EResponseType
  {
    /** The response of <code>GET /{ServiceGroupId}</code> */
    SERVICE_GROUP,
    /** The response of <code>GET /{ServiceGroupId}/services/{DocumentTypeId}</code> */
    SERVICE_METADATA
  }

  private final String m_sParticipantID;
  private final EResponseType m_eResponseType;
  private final String m_sDetail;
  private final OffsetDateTime m_aLastModDT;

  private SMPRestResponseCacheKey (@NonNull @Nonempty final String sParticipantID,
                                   @NonNull final EResponseType eResponseType,
                                   @NonNull @Nonempty final String sDetail,
                                   @Nullable final OffsetDateTime aLastModDT)
  {
    ValueEnforcer.notEmpty (sParticipantID, "ParticipantID");
    ValueEnforcer.notNull (eResponseType, "ResponseType");
    ValueEnforcer.notEmpty (sDetail, "Detail");
    m_sParticipantID = sParticipantID;
    m_eResponseType = eResponseType;
    m_sDetail = sDetail;
    // Normalise, so that the same instant is always equal
    m_aLastModDT = aLastModDT == null ? null : aLastModDT.withOffsetSameInstant (ZoneOffset.UTC);
  }

  /**
   * @return The URI encoded participant identifier the response belongs to. Neither
   *         <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getParticipantID ()
  {
    return m_sParticipantID;
  }

  /**
   * @return The type of the cached response. Never <code>null</code>.
   */
  @NonNull
  public EResponseType getResponseType ()
  {
    return m_eResponseType;
  }

  /**
   * @return The response type specific detail that distinguishes multiple responses of the same
   *         participant. For service groups this is the absolute service group HREF, for service
   *         metadata this is the URI encoded document type identifier. Neither <code>null</code>
   *         nor empty.
   */
  @NonNull
  @Nonempty
  public String getDetail ()
  {
    return m_sDetail;
  }

  /**
   * @return The last modification date time of the participant, that was read before the response
   *         was created. It is only part of the key, if the HTTP header <code>Last-Modified</code>
   *         is sent, so that a cached response always matches the sent last modification. May be
   *         <code>null</code>.
   * @since 8.9.0
   */
  @Nullable
  public OffsetDateTime getLastModificationDateTime ()
  {
    return m_aLastModDT;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final SMPRestResponseCacheKey rhs = (SMPRestResponseCacheKey) o;
    return m_sParticipantID.equals (rhs.m_sParticipantID) &&
           m_eResponseType.equals (rhs.m_eResponseType) &&
           m_sDetail.equals (rhs.m_sDetail) &&
           EqualsHelper.equals (m_aLastModDT, rhs.m_aLastModDT);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sParticipantID)
                                       .append (m_eResponseType)
                                       .append (m_sDetail)
                                       .append (m_aLastModDT)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("ParticipantID", m_sParticipantID)
                                       .append ("ResponseType", m_eResponseType)
                                       .append ("Detail", m_sDetail)
                                       .appendIfNotNull ("LastModDT", m_aLastModDT)
                                       .getToString ();
  }

  /**
   * Create a key for the response of <code>GET /{ServiceGroupId}</code>.
   *
   * @param aParticipantID
   *        The participant of the service group. May not be <code>null</code>.
   * @param sServiceGroupHref
   *        The absolute HREF of the service group. It is part of the key, because the response
   *        contains absolute URLs that depend on the public URL of the SMP. May neither be
   *        <code>null</code> nor empty.
   * @return The new key and never <code>null</code>.
   */
  @NonNull
  public static SMPRestResponseCacheKey forServiceGroup (@NonNull final IParticipantIdentifier aParticipantID,
                                                         @NonNull @Nonempty final String sServiceGroupHref)
  {
    return forServiceGroup (aParticipantID, sServiceGroupHref, null);
  }

  /**
   * Create a key for the response of <code>GET /{ServiceGroupId}</code>.
   *
   * @param aParticipantID
   *        The participant of the service group. May not be <code>null</code>.
   * @param sServiceGroupHref
   *        The absolute HREF of the service group. It is part of the key, because the response
   *        contains absolute URLs that depend on the public URL of the SMP. May neither be
   *        <code>null</code> nor empty.
   * @param aLastModDT
   *        The last modification date time of the participant, read before the response is
   *        created. May be <code>null</code>.
   * @return The new key and never <code>null</code>.
   * @since 8.9.0
   */
  @NonNull
  public static SMPRestResponseCacheKey forServiceGroup (@NonNull final IParticipantIdentifier aParticipantID,
                                                         @NonNull @Nonempty final String sServiceGroupHref,
                                                         @Nullable final OffsetDateTime aLastModDT)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    return new SMPRestResponseCacheKey (aParticipantID.getURIEncoded (),
                                        EResponseType.SERVICE_GROUP,
                                        sServiceGroupHref,
                                        aLastModDT);
  }

  /**
   * Create a key for the signed response of
   * <code>GET /{ServiceGroupId}/services/{DocumentTypeId}</code>.
   *
   * @param aParticipantID
   *        The participant of the service group. May not be <code>null</code>.
   * @param aDocTypeID
   *        The document type identifier. May not be <code>null</code>.
   * @return The new key and never <code>null</code>.
   */
  @NonNull
  public static SMPRestResponseCacheKey forServiceMetadata (@NonNull final IParticipantIdentifier aParticipantID,
                                                            @NonNull final IDocumentTypeIdentifier aDocTypeID)
  {
    return forServiceMetadata (aParticipantID, aDocTypeID, null);
  }

  /**
   * Create a key for the signed response of
   * <code>GET /{ServiceGroupId}/services/{DocumentTypeId}</code>.
   *
   * @param aParticipantID
   *        The participant of the service group. May not be <code>null</code>.
   * @param aDocTypeID
   *        The document type identifier. May not be <code>null</code>.
   * @param aLastModDT
   *        The last modification date time of the participant, read before the response is
   *        created. May be <code>null</code>.
   * @return The new key and never <code>null</code>.
   * @since 8.9.0
   */
  @NonNull
  public static SMPRestResponseCacheKey forServiceMetadata (@NonNull final IParticipantIdentifier aParticipantID,
                                                            @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                                            @Nullable final OffsetDateTime aLastModDT)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aDocTypeID, "DocTypeID");
    return new SMPRestResponseCacheKey (aParticipantID.getURIEncoded (),
                                        EResponseType.SERVICE_METADATA,
                                        aDocTypeID.getURIEncoded (),
                                        aLastModDT);
  }
}
