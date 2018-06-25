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
package com.helger.peppol.smpserver.domain.extension;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.string.StringHelper;

/**
 * Base interface for objects having an extension (service group, redirect,
 * endpoint, process and service metadata)
 *
 * @author Philip Helger
 */
public interface ISMPHasExtension
{
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <com.helger.peppol.bdxr.ExtensionType> getAllExtensions ();

  /**
   * @return The string representation of the extension element. May be
   *         <code>null</code>. If an extension is present it must be
   *         well-formed XML content.
   * @see #hasExtension()
   */
  @Nullable
  String getExtensionAsString ();

  /**
   * @return The XML content of the first extension or <code>null</code> if no
   *         extension is present.
   */
  @Nullable
  String getFirstExtensionXML ();

  /**
   * @return <code>true</code> if an extension is present, <code>false</code>
   *         otherwise.
   */
  default boolean hasExtension ()
  {
    return StringHelper.hasText (getExtensionAsString ());
  }
}
