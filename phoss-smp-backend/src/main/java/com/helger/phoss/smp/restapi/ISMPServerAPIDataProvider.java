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
package com.helger.phoss.smp.restapi;

import java.net.URI;

import org.jspecify.annotations.NonNull;

import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * This interface must be implemented by all real SMP implementations, so that
 * the REST API can easily be used. It provides information only available in
 * the web application.
 *
 * @author Philip Helger
 */
public interface ISMPServerAPIDataProvider
{
  /**
   * @return The URI of the current request. May not be <code>null</code>.
   */
  @NonNull
  URI getCurrentURI ();

  /**
   * Get the service group HREF for the passed service group ID. Since this
   * depends on the web address of the server it must be implemented in this
   * interface.
   *
   * @param aServiceGroupID
   *        The service group ID. Never <code>null</code>.
   * @return The HREF to show the service group.
   */
  @NonNull
  String getServiceGroupHref (@NonNull IParticipantIdentifier aServiceGroupID);

  /**
   * Get the service metadata HREF for the passed service group and document
   * type ID. Since this depends on the web address of the server it must be
   * implemented in this interface.
   *
   * @param aServiceGroupID
   *        The service group ID. Never <code>null</code>.
   * @param aDocTypeID
   *        The document type ID of the participant to query.
   * @return The HREF to the service metadata.
   */
  @NonNull
  String getServiceMetadataReferenceHref (@NonNull IParticipantIdentifier aServiceGroupID,
                                          @NonNull IDocumentTypeIdentifier aDocTypeID);
}
