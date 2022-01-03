/*
 * Copyright (C) 2015-2022 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.extension;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;

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
  ICommonsList <com.helger.xsds.bdxr.smp1.ExtensionType> extensions ();

  /**
   * @return The string representation of all extension elements together (like
   *         a CLOB). May be <code>null</code>. If an extension is present it
   *         must be well-formed JSON content.
   */
  @Nullable
  String getExtensionsAsString ();

  /**
   * @return The XML content of the first extension or <code>null</code> if no
   *         extension is present.
   */
  @Nullable
  String getFirstExtensionXML ();
}
