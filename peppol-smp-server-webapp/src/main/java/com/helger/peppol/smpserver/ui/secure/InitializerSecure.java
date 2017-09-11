/**
 * Copyright (C) 2014-2017 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.ui.secure;

import javax.annotation.Nonnull;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.pd.client.PDClientConfiguration;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.app.AppConfiguration;
import com.helger.peppol.smpserver.app.PDClientProvider;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.photon.basic.app.menu.IMenuTree;
import com.helger.photon.bootstrap3.pages.sysinfo.ConfigurationFile;
import com.helger.photon.bootstrap3.pages.sysinfo.ConfigurationFileManager;
import com.helger.photon.core.app.context.LayoutExecutionContext;
import com.helger.photon.core.app.init.IApplicationInitializer;
import com.helger.photon.core.app.layout.CLayout;
import com.helger.photon.core.app.layout.ILayoutManager;
import com.helger.photon.uictrls.prism.EPrismLanguage;

/**
 * Initialize the secure application stuff
 *
 * @author Philip Helger
 */
public final class InitializerSecure implements IApplicationInitializer <LayoutExecutionContext>
{
  public void initLayout (@Nonnull final ILayoutManager <LayoutExecutionContext> aLayoutMgr)
  {
    aLayoutMgr.registerAreaContentProvider (CLayout.LAYOUT_AREAID_VIEWPORT, new SMPRendererSecure ());
  }

  public void initMenu (@Nonnull final IMenuTree aMenuTree)
  {
    MenuSecure.init (aMenuTree);
  }

  public void initRest ()
  {
    final ConfigurationFileManager aCFM = ConfigurationFileManager.getInstance ();
    aCFM.registerConfigurationFile (new ConfigurationFile (new ClassPathResource ("log4j2.xml")).setDescription ("Log4J2 configuration")
                                                                                                .setSyntaxHighlightLanguage (EPrismLanguage.MARKUP));
    aCFM.registerConfigurationFile (new ConfigurationFile (AppConfiguration.getSettingsResource ()).setDescription ("SMP web application configuration")
                                                                                                   .setSyntaxHighlightLanguage (EPrismLanguage.APACHECONF));
    final IReadableResource aConfigRes = SMPServerConfiguration.getConfigFile ().getReadResource ();
    if (aConfigRes != null)
      aCFM.registerConfigurationFile (new ConfigurationFile (aConfigRes).setDescription ("SMP server configuration")
                                                                        .setSyntaxHighlightLanguage (EPrismLanguage.APACHECONF));
    final IReadableResource aPDClientConfig = PDClientConfiguration.getConfigFile ().getReadResource ();
    if (aPDClientConfig != null)
      aCFM.registerConfigurationFile (new ConfigurationFile (aPDClientConfig).setDescription ("PEPPOL Directory client configuration")
                                                                             .setSyntaxHighlightLanguage (EPrismLanguage.APACHECONF));

    // If the SMP settings change, the PD client must be re-created
    SMPMetaManager.getSettingsMgr ().callbacks ().add (x -> PDClientProvider.getInstance ().resetPDClient ());
  }
}
