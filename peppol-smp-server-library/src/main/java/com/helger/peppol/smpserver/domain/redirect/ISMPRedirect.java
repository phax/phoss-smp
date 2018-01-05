/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.domain.redirect;

import java.io.Serializable;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.compare.IComparator;
import com.helger.commons.id.IHasID;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.smpserver.domain.extension.ISMPHasExtension;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;

/**
 * This interface represents a single SMP redirect for a certain document type
 * identifier ({@link IDocumentTypeIdentifier}).
 *
 * @author Philip Helger
 */
public interface ISMPRedirect extends IHasID <String>, Serializable, ISMPHasExtension
{
  /**
   * @return The service group which this redirect should handle.
   */
  @Nonnull
  ISMPServiceGroup getServiceGroup ();

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
   * @return This redirect object as a PEPPOL SMP JAXB object for the REST
   *         interface. Never <code>null</code>.
   */
  @Nonnull
  com.helger.peppol.smp.ServiceMetadataType getAsJAXBObjectPeppol ();

  /**
   * @return This redirect object as a BDXR SMP JAXB object for the REST
   *         interface. Never <code>null</code>.
   */
  @Nonnull
  com.helger.peppol.bdxr.ServiceMetadataType getAsJAXBObjectBDXR ();

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
