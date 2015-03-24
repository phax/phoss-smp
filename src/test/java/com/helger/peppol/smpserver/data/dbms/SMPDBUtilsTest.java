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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.Test;

import com.helger.peppol.smpserver.data.dbms.SMPDBUtils;

/**
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public class SMPDBUtilsTest
{
  @Test
  public void testGetRFC1421CompliantString ()
  {
    assertNull (SMPDBUtils.getRFC1421CompliantString (null));
    assertEquals ("", SMPDBUtils.getRFC1421CompliantString (""));

    // for up to 64 chars it makes no difference
    for (int i = 0; i <= 64; ++i)
    {
      final char [] aChars = new char [i];
      Arrays.fill (aChars, 'a');
      final String sText = new String (aChars);
      assertEquals (sText, SMPDBUtils.getRFC1421CompliantString (sText));
    }

    final String sLong = "123456789012345678901234567890123456789012345678901234567890abcd"
                         + "123456789012345678901234567890123456789012345678901234567890abcd"
                         + "xyz";
    final String sFormatted = SMPDBUtils.getRFC1421CompliantString (sLong);
    assertEquals ("123456789012345678901234567890123456789012345678901234567890abcd\r\n"
                  + "123456789012345678901234567890123456789012345678901234567890abcd\r\n"
                  + "xyz", sFormatted);
  }
}
