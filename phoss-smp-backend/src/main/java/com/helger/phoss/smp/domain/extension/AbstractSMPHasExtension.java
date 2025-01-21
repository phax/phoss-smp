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
package com.helger.phoss.smp.domain.extension;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.smpclient.extension.SMPExtensionList;

/**
 * Abstract implementation class for {@link ISMPHasExtension}. All extensions
 * are internally stored as instances of
 * {@link com.helger.xsds.bdxr.smp1.ExtensionType} since this the biggest data
 * type which can be used for Peppol SMP and BDXR SMP.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public abstract class AbstractSMPHasExtension implements ISMPHasExtension
{
  private final SMPExtensionList m_aExtensions = new SMPExtensionList ();

  protected AbstractSMPHasExtension ()
  {}

  @Nonnull
  @ReturnsMutableObject
  public final SMPExtensionList getExtensions ()
  {
    return m_aExtensions;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;

    final AbstractSMPHasExtension rhs = (AbstractSMPHasExtension) o;
    return m_aExtensions.equals (rhs.m_aExtensions);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aExtensions).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Extensions", m_aExtensions).getToString ();
  }
}
