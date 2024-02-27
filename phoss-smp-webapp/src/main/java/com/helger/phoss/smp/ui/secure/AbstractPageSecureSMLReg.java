/*
 * Copyright (C) 2014-2024 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.smp.ui.secure;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLSocketFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smlclient.ManageServiceMetadataServiceCaller;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.phoss.smp.security.SMPTrustManager;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.photon.uicore.page.WebPageExecutionContext;

public abstract class AbstractPageSecureSMLReg extends AbstractSMPWebPage
{
  protected static final String HELPTEXT_SMP_ID = "This is the unique ID your SMP will have inside the SML. All continuing operations must use this ID. This ID is taken from the configuration file. All uppercase names are appreciated!";
  protected static final String HELPTEXT_LOGICAL_ADDRESS = "This must be the public fully qualified domain name of your SMP. This can be either a domain name like ''http://smp.example.org'' or an IP address like ''http://1.1.1.1''! The hostname of localhost is ''{0}''.";
  protected static final String DEFAULT_PHYSICAL_ADDRESS = "1.1.1.1";

  protected AbstractPageSecureSMLReg (@Nonnull @Nonempty final String sID, @Nonnull final String sName)
  {
    super (sID, sName);
  }

  protected final boolean canShowPage (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    // No truststore is okay - that can be handled
    if (false)
      if (!SMPTrustManager.isTrustStoreValid ())
      {
        aNodeList.addChild (error ("No valid truststore is provided, so no connection with the SML can be established!"));
        return false;
      }
    if (!SMPKeyManager.isKeyStoreValid ())
    {
      aNodeList.addChild (error ("No valid keystore/certificate is provided, so no connection with the SML can be established!"));
      return false;
    }
    return true;
  }

  @Nonnull
  protected static ManageServiceMetadataServiceCaller createSMLCaller (@Nonnull final ISMLInfo aSML,
                                                                       @Nonnull final SSLSocketFactory aSocketFactory)
  {
    final ManageServiceMetadataServiceCaller ret = new ManageServiceMetadataServiceCaller (aSML);
    ret.setSSLSocketFactory (aSocketFactory);
    return ret;
  }
}
