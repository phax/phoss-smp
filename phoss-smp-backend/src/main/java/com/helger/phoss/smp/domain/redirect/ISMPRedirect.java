/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.redirect;

import java.security.cert.X509Certificate;

import com.helger.annotation.Nonempty;
import com.helger.base.compare.IComparator;
import com.helger.base.id.IHasID;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.extension.ISMPHasExtension;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * This interface represents a single SMP redirect for a certain document type
 * identifier ({@link IDocumentTypeIdentifier}).
 *
 * @author Philip Helger
 */
public interface ISMPRedirect extends IHasID <String>, ISMPHasExtension
{
  /**
   * The ID of an SMP redirect is usually the combination of service group ID
   * and document type ID. So this is NOT the same as the service group ID.
   */
  @Nonnull
  @Nonempty
  String getID ();

  /**
   * @return The participant ID of the service group to which this service
   *         information belongs. Never <code>null</code>.
   */
  @Nonnull
  IParticipantIdentifier getServiceGroupParticipantIdentifier ();

  /**
   * @return The ID of the service group to which this redirect belongs. Never
   *         <code>null</code>.
   */
  @Nonnull
  @Nonempty
  String getServiceGroupID ();

  /**
   * @return The document type identifier of this redirect. Never
   *         <code>null</code>.
   */
  @Nonnull
  IDocumentTypeIdentifier getDocumentTypeIdentifier ();

  /**
   * @return The destination href of the new SMP. Never <code>null</code>.
   */
  @Nonnull
  @Nonempty
  String getTargetHref ();

  /**
   * @return The subject unique identifier of the target SMPs certificate used
   *         to sign its resources.
   */
  @Nonnull
  @Nonempty
  String getSubjectUniqueIdentifier ();

  /**
   * @return The X509 public certificate of the new SMP where the redirect
   *         points to. This is needed since OASIS BDXR SMP v2. May be
   *         <code>null</code>.
   * @since 5.2.0
   */
  @Nullable
  X509Certificate getCertificate ();

  /**
   * @return <code>true</code> if a redirect certificate is present,
   *         <code>false</code> if not.
   * @since 5.2.0
   */
  default boolean hasCertificate ()
  {
    return getCertificate () != null;
  }

  /**
   * @return This redirect object as a Peppol SMP JAXB object for the REST
   *         interface. Never <code>null</code>.
   */
  @Nonnull
  com.helger.xsds.peppol.smp1.ServiceMetadataType getAsJAXBObjectPeppol ();

  /**
   * @return This redirect object as a BDXR SMP v1 JAXB object for the REST
   *         interface. Never <code>null</code>.
   */
  @Nonnull
  com.helger.xsds.bdxr.smp1.ServiceMetadataType getAsJAXBObjectBDXR1 ();

  /**
   * @return This redirect object as a BDXR SMP v2 JAXB object for the REST
   *         interface. Never <code>null</code>.
   */
  @Nonnull
  com.helger.xsds.bdxr.smp2.ServiceMetadataType getAsJAXBObjectBDXR2 ();

  @Nonnull
  static IComparator <ISMPRedirect> comparator ()
  {
    return (aElement1, aElement2) -> {
      int ret = aElement1.getServiceGroupID ().compareTo (aElement2.getServiceGroupID ());
      if (ret == 0)
        ret = aElement1.getDocumentTypeIdentifier ().compareTo (aElement2.getDocumentTypeIdentifier ());
      return ret;
    };
  }
}
