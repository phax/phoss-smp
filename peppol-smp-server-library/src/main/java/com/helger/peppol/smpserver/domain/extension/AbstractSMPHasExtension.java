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
import javax.annotation.concurrent.NotThreadSafe;

import org.w3c.dom.Element;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.peppol.bdxr.BDXRExtensionConverter;
import com.helger.peppol.bdxr.ExtensionType;
import com.helger.xml.serialize.write.XMLWriter;

/**
 * Abstract implementation class for {@link ISMPHasExtension}. All extensions
 * are internally stored as instances of
 * {@link com.helger.peppol.bdxr.ExtensionType} since this the biggest data type
 * which can be used for PEPPOL SMP and BDXR SMP.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public abstract class AbstractSMPHasExtension implements ISMPHasExtension
{
  private final ICommonsList <com.helger.peppol.bdxr.ExtensionType> m_aExtensions = new CommonsArrayList<> ();

  protected AbstractSMPHasExtension ()
  {}

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <com.helger.peppol.bdxr.ExtensionType> getAllExtensions ()
  {
    return m_aExtensions.getClone ();
  }

  @Nullable
  public String getExtensionAsString ()
  {
    if (m_aExtensions.isEmpty ())
      return null;
    return BDXRExtensionConverter.convertToString (m_aExtensions);
  }

  @Nullable
  public String getFirstExtensionXML ()
  {
    if (m_aExtensions.isEmpty ())
      return null;

    // Use only the XML element of the first extension
    final Element aAny = (Element) m_aExtensions.getFirst ().getAny ();
    return XMLWriter.getNodeAsString (aAny);
  }

  @Nonnull
  public EChange setExtensionAsString (@Nullable final String sExtension)
  {
    ICommonsList <ExtensionType> aNewExt = null;
    if (StringHelper.hasText (sExtension))
    {
      // Soft migration :)
      if (sExtension.charAt (0) == '<')
        aNewExt = BDXRExtensionConverter.convertXMLToSingleExtension (sExtension);
      else
        aNewExt = BDXRExtensionConverter.convert (sExtension);
    }
    if (m_aExtensions.equals (aNewExt))
      return EChange.UNCHANGED;
    m_aExtensions.setAll (aNewExt);
    return EChange.CHANGED;
  }

  @Nullable
  @ReturnsMutableCopy
  public com.helger.peppol.smp.ExtensionType getAsPeppolExtension ()
  {
    if (m_aExtensions.isEmpty ())
      return null;

    // Use only the XML element of the first extension
    final com.helger.peppol.smp.ExtensionType ret = new com.helger.peppol.smp.ExtensionType ();
    ret.setAny ((Element) m_aExtensions.getFirst ().getAny ());
    return ret;
  }

  @Nullable
  @ReturnsMutableCopy
  public ICommonsList <com.helger.peppol.bdxr.ExtensionType> getAsBDXRExtension ()
  {
    if (m_aExtensions.isEmpty ())
      return null;

    return m_aExtensions.getClone ();
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
