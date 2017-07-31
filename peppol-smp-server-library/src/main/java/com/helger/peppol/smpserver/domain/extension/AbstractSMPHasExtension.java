/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
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
