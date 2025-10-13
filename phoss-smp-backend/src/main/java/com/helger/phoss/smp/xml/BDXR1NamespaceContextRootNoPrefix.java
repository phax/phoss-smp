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
package com.helger.phoss.smp.xml;

import com.helger.smpclient.bdxr1.marshal.BDXR1NamespaceContext;
import com.helger.xsds.bdxr.smp1.CBDXRSMP1;

import jakarta.annotation.Nonnull;

/**
 * Special version of {@link BDXR1NamespaceContext} where the root element uses the default prefix.
 * 
 * @author Philip Helger
 * @since 8.0.1
 */
public class BDXR1NamespaceContextRootNoPrefix extends BDXR1NamespaceContext
{
  private static final class SingletonHolder
  {
    static final BDXR1NamespaceContextRootNoPrefix INSTANCE = new BDXR1NamespaceContextRootNoPrefix ();
  }

  /**
   * Deprecated constructor.
   *
   * @deprecated Use {@link BDXR1NamespaceContextRootNoPrefix#getInstance()} instead.
   */
  @Deprecated (forRemoval = false)
  public BDXR1NamespaceContextRootNoPrefix ()
  {
    removeMapping (CBDXRSMP1.DEFAULT_PREFIX);
    addDefaultNamespaceURI (CBDXRSMP1.NAMESPACE_URI);
  }

  @Nonnull
  public static BDXR1NamespaceContextRootNoPrefix getInstance ()
  {
    return SingletonHolder.INSTANCE;
  }
}
