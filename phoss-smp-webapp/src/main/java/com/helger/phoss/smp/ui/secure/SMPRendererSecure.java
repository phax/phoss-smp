/*
 * Copyright (C) 2014-2022 Philip Helger and contributors
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

import java.util.Locale;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.html.hc.html.textlevel.HCStrong;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.config.SMPHttpConfiguration;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.ui.ajax.CAjax;
import com.helger.phoss.smp.ui.pub.SMPRendererPublic;
import com.helger.photon.app.url.LinkHelper;
import com.helger.photon.bootstrap4.CBootstrapCSS;
import com.helger.photon.bootstrap4.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap4.alert.EBootstrapAlertType;
import com.helger.photon.bootstrap4.breadcrumb.BootstrapBreadcrumb;
import com.helger.photon.bootstrap4.breadcrumb.BootstrapBreadcrumbProvider;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.layout.BootstrapContainer;
import com.helger.photon.bootstrap4.navbar.BootstrapNavbar;
import com.helger.photon.bootstrap4.navbar.BootstrapNavbarToggleable;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapMenuItemRenderer;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapPageRenderer;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.execcontext.ILayoutExecutionContext;
import com.helger.photon.core.html.CLayout;
import com.helger.photon.core.servlet.AbstractPublicApplicationServlet;
import com.helger.photon.core.servlet.LogoutServlet;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.util.SecurityHelper;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.scope.singleton.AbstractSessionSingleton;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * The viewport renderer (menu + content area)
 *
 * @author Philip Helger
 */
public final class SMPRendererSecure
{
  /**
   * A helper class that checks once per session if proxy information are
   * configured or not. Usually this information does not change, it it is not
   * worth the effort to query that in every request.
   *
   * @author Philip Helger
   */
  public static final class MenuSessionState extends AbstractSessionSingleton
  {
    private final boolean m_bHttpProxyEnabled;
    private final boolean m_bHttpsProxyEnabled;

    @Deprecated
    @UsedViaReflection
    public MenuSessionState ()
    {
      m_bHttpProxyEnabled = SMPHttpConfiguration.getAsHttpProxySettings () != null;
      m_bHttpsProxyEnabled = SMPHttpConfiguration.getAsHttpsProxySettings () != null;
    }

    @Nonnull
    public static MenuSessionState getInstance ()
    {
      return getSessionSingleton (MenuSessionState.class);
    }

    public boolean isHttpProxyEnabled ()
    {
      return m_bHttpProxyEnabled;
    }

    public boolean isHttpsProxyEnabled ()
    {
      return m_bHttpsProxyEnabled;
    }
  }

  private SMPRendererSecure ()
  {}

  @Nonnull
  private static IHCNode _getNavbar (@Nonnull final ILayoutExecutionContext aLEC)
  {
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();

    final ISimpleURL aLinkToStartPage = aLEC.getLinkToMenuItem (aLEC.getMenuTree ().getDefaultMenuItemID ());

    final BootstrapNavbar aNavbar = new BootstrapNavbar ();
    aNavbar.addBrand (SMPRendererPublic.createLogo (aLEC), aLinkToStartPage);
    aNavbar.addBrand (new HCSpan ().addChild (CSMP.getApplicationSuffix () + " Administration"), aLinkToStartPage);
    aNavbar.addAndReturnText ().addChild (" [" + SMPServerConfiguration.getSMLSMPID () + "]");

    final BootstrapNavbarToggleable aToggleable = aNavbar.addAndReturnToggleable ();

    {
      aToggleable.addChild (new BootstrapButton ().addClass (CBootstrapCSS.ML_AUTO)
                                                  .addClass (CBootstrapCSS.MR_2)
                                                  .setOnClick (LinkHelper.getURLWithContext (AbstractPublicApplicationServlet.SERVLET_DEFAULT_PATH +
                                                                                             "/"))
                                                  .addChild ("Goto public view"));

      final IUser aUser = aLEC.getLoggedInUser ();
      aToggleable.addAndReturnText ()
                 .addClass (CBootstrapCSS.MX_2)
                 .addChild ("Logged in as ")
                 .addChild (new HCStrong ().addChild (SecurityHelper.getUserDisplayName (aUser, aDisplayLocale)));
      aToggleable.addChild (new BootstrapButton ().addClass (CBootstrapCSS.MX_2)
                                                  .setOnClick (LinkHelper.getURLWithContext (aRequestScope,
                                                                                             LogoutServlet.SERVLET_DEFAULT_PATH))
                                                  .addChild (EPhotonCoreText.LOGIN_LOGOUT.getDisplayText (aDisplayLocale)));
    }
    return aNavbar;
  }

  @Nonnull
  public static IHCNode getMenuContent (@Nonnull final ILayoutExecutionContext aLEC)
  {
    final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();
    final HCNodeList ret = new HCNodeList ();
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();

    // Main menu in the left
    ret.addChild (BootstrapMenuItemRenderer.createSideBarMenu (aLEC));

    // Small box with general information
    final BootstrapSuccessBox aBox = new BootstrapSuccessBox ().addClass (CBootstrapCSS.MT_2);

    if (SMPMetaManager.getInstance ().getBackendConnectionState ().isFalse ())
    {
      aBox.addChild (new HCDiv ().addChild (EDefaultIcon.NO.getAsNode ())
                                 .addChild (" No database connection: ")
                                 .addChild (new HCA (CAjax.FUNCTION_BACKEND_CONNECTION_RESET.getInvocationURL (aRequestScope)).addChild ("Retry")));
      aBox.setType (EBootstrapAlertType.DANGER);
    }

    // Information on SML usage
    if (aSettings.isSMLEnabled ())
    {
      aBox.addChild (new HCDiv ().addChild (EDefaultIcon.YES.getAsNode ()).addChild (" SML connection is configured."));
      if (aSettings.getSMLInfo () == null)
      {
        aBox.addChild (new HCDiv ().addChild (EDefaultIcon.NO.getAsNode ())
                                   .addChild (" No SML is selected. ")
                                   .addChild (new HCA (aLEC.getLinkToMenuItem (CMenuSecure.MENU_SMP_SETTINGS)).addChild ("Fix me")));
        aBox.setType (EBootstrapAlertType.DANGER);
      }
    }
    else
    {
      // Warn only if SML is needed
      if (aSettings.isSMLRequired ())
      {
        aBox.addChild (new HCDiv ().addChild (EDefaultIcon.NO.getAsNode ())
                                   .addChild (" SML connection is NOT configured. ")
                                   .addChild (new HCA (aLEC.getLinkToMenuItem (CMenuSecure.MENU_SMP_SETTINGS)).addChild ("Fix me")));
        aBox.setTypeIfWorse (EBootstrapAlertType.WARNING);
      }
    }

    if (aSettings.isDirectoryIntegrationRequired ())
    {
      if (aSettings.isDirectoryIntegrationEnabled ())
      {
        aBox.addChild (new HCDiv ().addChild (EDefaultIcon.YES.getAsNode ())
                                   .addChild (" Directory support is configured."));
        if (StringHelper.hasNoText (aSettings.getDirectoryHostName ()))
        {
          aBox.addChild (new HCDiv ().addChild (EDefaultIcon.NO.getAsNode ())
                                     .addChild (" No Directory host is provided. ")
                                     .addChild (new HCA (aLEC.getLinkToMenuItem (CMenuSecure.MENU_SMP_SETTINGS)).addChild ("Fix me")));
          aBox.setType (EBootstrapAlertType.DANGER);
        }
      }
      else
      {
        // Warn only if Directory is needed
        aBox.addChild (new HCDiv ().addChild (EDefaultIcon.NO.getAsNode ())
                                   .addChild (" Directory support is NOT configured. ")
                                   .addChild (new HCA (aLEC.getLinkToMenuItem (CMenuSecure.MENU_SMP_SETTINGS)).addChild ("Fix me")));
        aBox.setTypeIfWorse (EBootstrapAlertType.WARNING);
      }
    }

    // Information on certificate
    if (!SMPKeyManager.isKeyStoreValid ())
    {
      aBox.addChild (new HCDiv ().addChild (EDefaultIcon.NO.getAsNode ())
                                 .addChild (" Certificate configuration is invalid"));
      aBox.setType (EBootstrapAlertType.DANGER);
    }

    // Info, mainly for support purposes
    if (MenuSessionState.getInstance ().isHttpProxyEnabled ())
      aBox.addChild (new HCDiv ().addChild (EDefaultIcon.INFO.getAsNode ()).addChild (" HTTP proxy is enabled"));
    if (MenuSessionState.getInstance ().isHttpsProxyEnabled ())
      aBox.addChild (new HCDiv ().addChild (EDefaultIcon.INFO.getAsNode ()).addChild (" HTTPS proxy is enabled"));

    ret.addChild (aBox);

    return ret;
  }

  @Nonnull
  public static IHCNode getContent (@Nonnull final ILayoutExecutionContext aLEC)
  {
    final HCNodeList ret = new HCNodeList ();

    final BootstrapContainer aOuterContainer = ret.addAndReturnChild (new BootstrapContainer ().setFluid (true));

    // Header
    aOuterContainer.addChild (_getNavbar (aLEC));

    // Breadcrumbs
    if (false)
    {
      final BootstrapBreadcrumb aBreadcrumbs = BootstrapBreadcrumbProvider.createBreadcrumb (aLEC);
      aBreadcrumbs.addClasses (CBootstrapCSS.D_NONE, CBootstrapCSS.D_SM_BLOCK);
      aOuterContainer.addChild (aBreadcrumbs);
    }

    // Content
    {
      final HCDiv aRow = aOuterContainer.addAndReturnChild (new HCDiv ().addClass (CBootstrapCSS.D_MD_FLEX)
                                                                        .addClass (CBootstrapCSS.MT_2));
      final HCDiv aCol1 = aRow.addAndReturnChild (new HCDiv ().addClass (CBootstrapCSS.D_MD_FLEX)
                                                              .addClass (CBootstrapCSS.MR_2));
      final HCDiv aCol2 = aRow.addAndReturnChild (new HCDiv ().addClass (CBootstrapCSS.FLEX_FILL));

      // left

      // We need a wrapper span for easy AJAX content replacement
      aCol1.addClass (CBootstrapCSS.D_PRINT_NONE)
           .addChild (new HCSpan ().setID (CLayout.LAYOUT_AREAID_MENU).addChild (getMenuContent (aLEC)));
      aCol1.addChild (new HCDiv ().setID (CLayout.LAYOUT_AREAID_SPECIAL));

      // content - determine is exactly same as for view
      aCol2.addChild (BootstrapPageRenderer.getPageContent (aLEC));
    }

    aOuterContainer.addChild (SMPRendererPublic.createDefaultFooter (true, true, true));

    return ret;
  }
}
