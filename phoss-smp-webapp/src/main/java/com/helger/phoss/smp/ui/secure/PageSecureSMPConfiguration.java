/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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

import java.time.LocalDateTime;
import java.util.Locale;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.state.ESuccess;
import com.helger.config.IConfig;
import com.helger.config.source.IConfigurationSource;
import com.helger.config.source.resource.IConfigurationSourceResource;
import com.helger.config.value.IConfigurationValueProvider;
import com.helger.config.value.IConfigurationValueProviderWithPriorityCallback;
import com.helger.datetime.format.PDTToString;
import com.helger.datetime.helper.PDTFactory;
import com.helger.html.hc.html.grouping.HCLI;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.html.textlevel.HCStrong;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.client.PDClientConfiguration;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.config.SMPConfigProvider;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.photon.bootstrap4.badge.BootstrapBadge;
import com.helger.photon.bootstrap4.badge.EBootstrapBadgeType;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;

public final class PageSecureSMPConfiguration extends AbstractSMPWebPage
{
  private static final class Reload implements IConfigurationValueProviderWithPriorityCallback
  {
    private final HCOL m_aOL;

    public Reload (@NonNull final HCOL aOL)
    {
      m_aOL = aOL;
    }

    public void onConfigurationValueProvider (@NonNull final IConfigurationValueProvider aCVP, final int nPriority)
    {
      if (aCVP instanceof final IConfigurationSourceResource aSrcRes)
      {
        final ESuccess eSuccess = aSrcRes.reload ();
        final HCLI aLI = m_aOL.addItem ()
                              .addChild (new HCCode ().addChild (aSrcRes.getResource ().getPath ()))
                              .addChild (" ");
        if (eSuccess.isSuccess ())
          aLI.addChild (new BootstrapBadge (EBootstrapBadgeType.SUCCESS).addChild ("Successfully reloaded"));
        else
          aLI.addChild (new BootstrapBadge (EBootstrapBadgeType.DANGER).addChild ("Failed to reload"));
      }
    }
  }

  private static final class List implements IConfigurationValueProviderWithPriorityCallback
  {
    private final HCOL m_aOL;

    public List (@NonNull final HCOL aOL)
    {
      m_aOL = aOL;
    }

    public void onConfigurationValueProvider (@NonNull final IConfigurationValueProvider aCVP, final int nPriority)
    {
      if (aCVP instanceof final IConfigurationSourceResource aSrcRes)
      {
        m_aOL.addItem ()
             .addChild (new HCCode ().addChild (aSrcRes.getResource ().getPath ()))
             .addChild (" (priority " + nPriority + ")")
             .addChild (new HCStrong ().addChild (" (reloadable)"));
      }
      else
        if (aCVP instanceof final IConfigurationSource aSrc)
        {
          // use getDisplayText instead of name in ph-commons 12.1.4+
          m_aOL.addItem (aSrc.getSourceType ().name () + " (priority " + nPriority + ")");
        }
    }
  }

  public static final String ACTION_RELOAD_CONFIG = "reloadconfig";

  private LocalDateTime m_aLastReload = null;

  public PageSecureSMPConfiguration (@NonNull @Nonempty final String sID)
  {
    super (sID, "SMP Configuration");
  }

  @Override
  protected void fillContent (@NonNull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final IConfig aSMLConfig = SMPConfigProvider.getConfig ();
    final IConfig aPDConfig = PDClientConfiguration.getConfig ();
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final String sDirectoryName = SMPWebAppConfiguration.getDirectoryName ();

    {

      final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
      aToolbar.addChild (new BootstrapButton ().addChild ("Reload Configuration Sources")
                                               .setDisabled ((aSMLConfig.getResourceBasedConfigurationValueProviderCount () +
                                                              aPDConfig.getResourceBasedConfigurationValueProviderCount ()) ==
                                                             0)
                                               .setOnClick (aWPEC.getSelfHref ()
                                                                 .add (CPageParam.PARAM_ACTION, ACTION_RELOAD_CONFIG))
                                               .setIcon (EDefaultIcon.REFRESH));
      aNodeList.addChild (aToolbar);
    }

    if (ACTION_RELOAD_CONFIG.equals (aWPEC.getAction ()))
      m_aLastReload = PDTFactory.getCurrentLocalDateTime ();

    // Just informational
    if (m_aLastReload != null)
      aNodeList.addChild (info ("Last configuration reload: " +
                                PDTToString.getAsString (m_aLastReload, aDisplayLocale)));

    // Show SMP main configuration
    aNodeList.addChild (h2 ("Current configuration sources registered"));
    {
      aNodeList.addChild (h3 ("SMP Server Configuration"));
      final HCOL aOL = new HCOL ();
      // This is a recursive iteration
      aSMLConfig.forEachConfigurationValueProvider (new List (aOL));
      if (aOL.hasNoChildren ())
        aOL.addItem ("No configuration source present");
      aNodeList.addChild (aOL);
    }

    if (ACTION_RELOAD_CONFIG.equals (aWPEC.getAction ()))
    {
      aNodeList.addChild (h4 ("Reloading Configuration Sources"));
      {
        final HCOL aOL = new HCOL ();
        aSMLConfig.forEachConfigurationValueProvider (new Reload (aOL));
        if (aOL.hasNoChildren ())
          aOL.addItem ("No reloadable configuration source present");
        aNodeList.addChild (aOL);
      }
    }

    if (aSettings.isDirectoryIntegrationEnabled ())
    {
      {
        aNodeList.addChild (h3 (sDirectoryName + " Client Configuration"));
        final HCOL aOL = new HCOL ();
        // This is a recursive iteration
        aPDConfig.forEachConfigurationValueProvider (new List (aOL));
        if (aOL.hasNoChildren ())
          aOL.addItem ("No configuration source present");
        aNodeList.addChild (aOL);
      }

      if (ACTION_RELOAD_CONFIG.equals (aWPEC.getAction ()))
      {
        aNodeList.addChild (h4 ("Reloading Configuration Sources"));
        {
          final HCOL aOL = new HCOL ();
          aPDConfig.forEachConfigurationValueProvider (new Reload (aOL));
          if (aOL.hasNoChildren ())
            aOL.addItem ("No reloadable configuration source present");
          aNodeList.addChild (aOL);
        }
      }
    }
  }
}
