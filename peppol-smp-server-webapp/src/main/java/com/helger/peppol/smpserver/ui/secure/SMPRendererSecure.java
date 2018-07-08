/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
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

import java.util.Locale;

import javax.annotation.Nonnull;

import com.helger.commons.url.ISimpleURL;
import com.helger.css.property.CCSSProperties;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.html.hc.html.textlevel.HCStrong;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.app.CApp;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.security.SMPKeyManager;
import com.helger.peppol.smpserver.settings.ISMPSettings;
import com.helger.peppol.smpserver.ui.pub.SMPRendererPublic;
import com.helger.photon.bootstrap3.CBootstrapCSS;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap3.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap3.base.BootstrapContainer;
import com.helger.photon.bootstrap3.breadcrumbs.BootstrapBreadcrumbs;
import com.helger.photon.bootstrap3.breadcrumbs.BootstrapBreadcrumbsProvider;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.grid.BootstrapRow;
import com.helger.photon.bootstrap3.nav.BootstrapNav;
import com.helger.photon.bootstrap3.navbar.BootstrapNavbar;
import com.helger.photon.bootstrap3.navbar.EBootstrapNavbarPosition;
import com.helger.photon.bootstrap3.navbar.EBootstrapNavbarType;
import com.helger.photon.bootstrap3.uictrls.ext.BootstrapMenuItemRenderer;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.app.context.ILayoutExecutionContext;
import com.helger.photon.core.app.context.LayoutExecutionContext;
import com.helger.photon.core.app.layout.CLayout;
import com.helger.photon.core.servlet.AbstractPublicApplicationServlet;
import com.helger.photon.core.servlet.LogoutServlet;
import com.helger.photon.core.url.LinkHelper;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.util.SecurityHelper;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * The viewport renderer (menu + content area)
 *
 * @author Philip Helger
 */
public final class SMPRendererSecure
{
  private SMPRendererSecure ()
  {}

  @Nonnull
  private static IHCNode _getNavbar (@Nonnull final ILayoutExecutionContext aLEC)
  {
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();

    final ISimpleURL aLinkToStartPage = aLEC.getLinkToMenuItem (aLEC.getMenuTree ().getDefaultMenuItemID ());

    final BootstrapNavbar aNavBar = new BootstrapNavbar (EBootstrapNavbarType.STATIC_TOP, true, aDisplayLocale);
    aNavBar.getContainer ().setFluid (true);
    aNavBar.addBrand (SMPRendererPublic.createLogo (aLEC), aLinkToStartPage);
    aNavBar.addBrand (new HCSpan ().addChild (CApp.getApplicationSuffix () + " Administration"), aLinkToStartPage);
    aNavBar.addText (EBootstrapNavbarPosition.COLLAPSIBLE_LEFT, " [" + SMPServerConfiguration.getSMLSMPID () + "]");
    aNavBar.addButton (EBootstrapNavbarPosition.COLLAPSIBLE_LEFT,
                       new BootstrapButton ().setOnClick (LinkHelper.getURLWithContext (AbstractPublicApplicationServlet.SERVLET_DEFAULT_PATH +
                                                                                        "/"))
                                             .addChild ("Goto public view")
                                             .addStyle (CCSSProperties.MARGIN_LEFT.newValue ("8px"))
                                             .addStyle (CCSSProperties.MARGIN_RIGHT.newValue ("8px")));

    {
      final BootstrapNav aNav = new BootstrapNav ();
      final IUser aUser = LoggedInUserManager.getInstance ().getCurrentUser ();
      aNav.addText (new HCSpan ().addChild ("Logged in as ")
                                 .addChild (new HCStrong ().addChild (SecurityHelper.getUserDisplayName (aUser,
                                                                                                         aDisplayLocale))));
      aNav.addButton (new BootstrapButton ().setOnClick (LinkHelper.getURLWithContext (aRequestScope,
                                                                                       LogoutServlet.SERVLET_DEFAULT_PATH))
                                            .addChild (EPhotonCoreText.LOGIN_LOGOUT.getDisplayText (aDisplayLocale))
                                            .addStyle (CCSSProperties.MARGIN_LEFT.newValue ("8px"))
                                            .addStyle (CCSSProperties.MARGIN_RIGHT.newValue ("8px")));

      aNavBar.addNav (EBootstrapNavbarPosition.COLLAPSIBLE_RIGHT, aNav);
    }
    return aNavBar;
  }

  @Nonnull
  public static IHCNode getMenuContent (@Nonnull final LayoutExecutionContext aLEC)
  {
    final HCNodeList ret = new HCNodeList ();
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();

    ret.addChild (BootstrapMenuItemRenderer.createSideBarMenu (aLEC));

    // Information on SML usage
    if (aSettings.isSMLActive ())
      ret.addChild (new BootstrapInfoBox ().addChild ("SML connection active!"));
    else
    {
      // Warn only if SML is needed
      if (SMPMetaManager.getSettings ().isSMLNeeded ())
        ret.addChild (new BootstrapWarnBox ().addChild ("SML connection NOT active!"));
    }

    if (SMPServerConfiguration.getRESTType ().isPEPPOL ())
    {
      if (aSettings.isPEPPOLDirectoryIntegrationEnabled ())
        ret.addChild (new BootstrapSuccessBox ().addChild ("Directory support is enabled!"));
      else
        ret.addChild (new BootstrapWarnBox ().addChild ("Directory support is disabled!"));
    }

    // Information on certificate
    if (!SMPKeyManager.isCertificateValid ())
      ret.addChild (new BootstrapErrorBox ().addChild ("Certificate configuration is invalid. REST queries will not work!"));

    return ret;
  }

  @Nonnull
  public static IHCNode getContent (@Nonnull final LayoutExecutionContext aLEC)
  {
    final HCNodeList ret = new HCNodeList ();

    final BootstrapContainer aOuterContainer = ret.addAndReturnChild (new BootstrapContainer ().setFluid (true));

    // Header
    aOuterContainer.addChild (_getNavbar (aLEC));

    // Breadcrumbs
    if (false)
    {
      final BootstrapBreadcrumbs aBreadcrumbs = BootstrapBreadcrumbsProvider.createBreadcrumbs (aLEC);
      aBreadcrumbs.addClass (CBootstrapCSS.HIDDEN_XS);
      aOuterContainer.addChild (aBreadcrumbs);
    }

    // Content
    {
      final BootstrapRow aRow = aOuterContainer.addAndReturnChild (new BootstrapRow ());
      final HCDiv aCol1 = aRow.createColumn (12, 4, 3, 2);
      final HCDiv aCol2 = aRow.createColumn (12, 8, 9, 10);

      // left
      // We need a wrapper span for easy AJAX content replacement
      aCol1.addChild (new HCSpan ().setID (CLayout.LAYOUT_AREAID_MENU)
                                   .addClass (CBootstrapCSS.HIDDEN_PRINT)
                                   .addChild (getMenuContent (aLEC)));
      aCol1.addChild (new HCDiv ().setID (CLayout.LAYOUT_AREAID_SPECIAL));

      // content - determine is exactly same as for view
      aCol2.addChild (SMPRendererPublic.getPageContent (aLEC));
    }

    aOuterContainer.addChild (SMPRendererPublic.createDefaultFooter ());

    return ret;
  }
}
