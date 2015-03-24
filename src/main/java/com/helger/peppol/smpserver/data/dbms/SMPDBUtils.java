/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.data.dbms;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.busdox.servicemetadata.publishing._1.ExtensionType;

import com.helger.commons.string.StringHelper;
import com.helger.peppol.utils.ExtensionConverter;

/**
 * This class is used inside the DB component and contains several utility
 * methods.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Immutable
public final class SMPDBUtils
{
  private SMPDBUtils ()
  {}

  @Nullable
  public static ExtensionType getAsExtensionSafe (@Nullable final String sXML)
  {
    try
    {
      return ExtensionConverter.convert (sXML);
    }
    catch (final IllegalArgumentException ex)
    {
      // Invalid XML passed
      return null;
    }
  }

  /**
   * The certificate string needs to be emitted in portions of 64 characters. If
   * characters are left, than &lt;CR>&lt;LF> ("\r\n") must be added to the
   * string so that the next characters start on a new line. After the last
   * part, no &lt;CR>&lt;LF> is needed. Respective RFC parts are 1421 4.3.2.2
   * and 4.3.2.4
   *
   * @param sCertificate
   *        Original certificate string as stored in the DB
   * @return The RFC 1421 compliant string
   */
  @Nullable
  public static String getRFC1421CompliantString (@Nullable final String sCertificate)
  {
    if (StringHelper.hasNoText (sCertificate))
      return sCertificate;

    // Remove all existing whitespace characters
    String sPlainString = StringHelper.getWithoutAnySpaces (sCertificate);

    // Start building the result
    final int nMaxLineLength = 64;
    final String sCRLF = "\r\n";
    final StringBuilder aSB = new StringBuilder ();
    while (sPlainString.length () > nMaxLineLength)
    {
      // Append line + CRLF
      aSB.append (sPlainString, 0, nMaxLineLength).append (sCRLF);

      // Remove the start of the string
      sPlainString = sPlainString.substring (nMaxLineLength);
    }

    // Append the rest
    aSB.append (sPlainString);

    return aSB.toString ();
  }
}
