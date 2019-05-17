/**
 * Copyright (C) 2014-2019 Philip Helger and contributors
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
package com.helger.phoss.smp.ui.pub;

import java.util.Locale;

import javax.annotation.Nonnull;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.SimpleURL;
import com.helger.css.property.CCSSProperties;
import com.helger.css.propertyvalue.CCSSValue;
import com.helger.html.css.DefaultCSSClassProvider;
import com.helger.html.css.ICSSClassProvider;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.ext.HCA_MailTo;
import com.helger.html.hc.html.embedded.HCImg;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCP;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.html.hc.html.textlevel.HCStrong;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.photon.basic.app.appid.CApplicationID;
import com.helger.photon.basic.app.appid.PhotonGlobalState;
import com.helger.photon.basic.app.menu.IMenuItemExternal;
import com.helger.photon.basic.app.menu.IMenuItemPage;
import com.helger.photon.basic.app.menu.IMenuObject;
import com.helger.photon.basic.app.menu.IMenuSeparator;
import com.helger.photon.basic.app.menu.IMenuTree;
import com.helger.photon.basic.app.menu.MenuItemDeterminatorCallback;
import com.helger.photon.bootstrap4.CBootstrapCSS;
import com.helger.photon.bootstrap4.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap4.breadcrumb.BootstrapBreadcrumb;
import com.helger.photon.bootstrap4.breadcrumb.BootstrapBreadcrumbProvider;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.dropdown.BootstrapDropdownMenu;
import com.helger.photon.bootstrap4.ext.BootstrapSystemMessage;
import com.helger.photon.bootstrap4.layout.BootstrapContainer;
import com.helger.photon.bootstrap4.navbar.BootstrapNavbar;
import com.helger.photon.bootstrap4.navbar.BootstrapNavbarNav;
import com.helger.photon.bootstrap4.navbar.BootstrapNavbarToggleable;
import com.helger.photon.bootstrap4.pages.BootstrapWebPageUIHandler;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapMenuItemRenderer;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapMenuItemRendererHorz;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.app.context.ILayoutExecutionContext;
import com.helger.photon.core.app.context.ISimpleWebExecutionContext;
import com.helger.photon.core.app.context.LayoutExecutionContext;
import com.helger.photon.core.app.error.InternalErrorBuilder;
import com.helger.photon.core.app.layout.CLayout;
import com.helger.photon.core.servlet.AbstractSecureApplicationServlet;
import com.helger.photon.core.servlet.LogoutServlet;
import com.helger.photon.core.url.LinkHelper;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.util.SecurityHelper;
import com.helger.photon.uicore.page.IWebPage;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.forcedredirect.ForcedRedirectException;
import com.helger.xservlet.forcedredirect.ForcedRedirectManager;

/**
 * The public application viewport renderer (menu + content area)
 *
 * @author Philip Helger
 */
public final class SMPRendererPublic
{
  private static final ICSSClassProvider CSS_CLASS_FOOTER_LINKS = DefaultCSSClassProvider.create ("footer-links");

  private static final ICommonsList <IMenuObject> s_aFooterObjects = new CommonsArrayList <> ();

  static
  {
    PhotonGlobalState.state (CApplicationID.APP_ID_PUBLIC).getMenuTree ().iterateAllMenuObjects (aCurrentObject -> {
      if (aCurrentObject.attrs ().containsKey (CMenuPublic.FLAG_FOOTER))
        s_aFooterObjects.add (aCurrentObject);
    });
  }

  private SMPRendererPublic ()
  {}

  private static void _addNavbarLoginLogout (@Nonnull final ILayoutExecutionContext aLEC,
                                             @Nonnull final BootstrapNavbar aNavbar)
  {
    final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();
    final IUser aUser = aLEC.getLoggedInUser ();

    final BootstrapNavbarToggleable aToggleable = aNavbar.addAndReturnToggleable ();

    if (aUser != null)
    {
      final Locale aDisplayLocale = aLEC.getDisplayLocale ();
      aToggleable.addChild (new BootstrapButton ().addClass (CBootstrapCSS.ML_AUTO)
                                                  .addClass (CBootstrapCSS.MR_2)
                                                  .addChild ("Goto manager")
                                                  .setOnClick (LinkHelper.getURLWithContext (AbstractSecureApplicationServlet.SERVLET_DEFAULT_PATH +
                                                                                             "/")));
      aToggleable.addAndReturnText ()
                 .addClass (CBootstrapCSS.MX_2)
                 .addChild ("Logged in as ")
                 .addChild (new HCStrong ().addChild (SecurityHelper.getUserDisplayName (aUser, aDisplayLocale)));

      aToggleable.addChild (new BootstrapButton ().addClass (CBootstrapCSS.MX_2)
                                                  .setOnClick (LinkHelper.getURLWithContext (aRequestScope,
                                                                                             LogoutServlet.SERVLET_DEFAULT_PATH))
                                                  .addChild (EPhotonCoreText.LOGIN_LOGOUT.getDisplayText (aDisplayLocale)));
    }
    else
    {
      final BootstrapNavbarNav aNav = aToggleable.addAndReturnNav ();
      final BootstrapDropdownMenu aDropDown = new BootstrapDropdownMenu ();
      {
        final HCDiv aDiv = new HCDiv ().addClass (CBootstrapCSS.P_2)
                                       .addStyle (CCSSProperties.MIN_WIDTH.newValue ("400px"));
        aDiv.addChild (SMPCommonUI.createViewLoginForm (aLEC, null));
        aDropDown.addChild (aDiv);
      }
      aNav.addItem ().addNavDropDown ("Login", aDropDown);
    }
  }

  @Nonnull
  public static IHCNode createLogo (@Nonnull final ISimpleWebExecutionContext aSWEC)
  {
    final IRequestWebScopeWithoutResponse aRequestScope = aSWEC.getRequestScope ();
    return new HCImg ().setSrc (LinkHelper.getStreamURL (aRequestScope, "/image/logo-gradient-223-50-transparent.png"))
                       .addStyle (CCSSProperties.MARGIN.newValue ("-15px"))
                       .addStyle (CCSSProperties.VERTICAL_ALIGN.newValue (CCSSValue.TOP))
                       .addStyle (CCSSProperties.PADDING.newValue ("0 6px"));
  }

  @Nonnull
  public static IHCNode createLogoBig (@Nonnull final ISimpleWebExecutionContext aSWEC)
  {
    final IRequestWebScopeWithoutResponse aRequestScope = aSWEC.getRequestScope ();
    return new HCImg ().setSrc (LinkHelper.getStreamURL (aRequestScope,
                                                         "/image/logo-gradient-446-100-transparent.png"));
  }

  @Nonnull
  private static BootstrapNavbar _getNavbar (@Nonnull final ILayoutExecutionContext aLEC)
  {
    final ISimpleURL aLinkToStartPage = aLEC.getLinkToMenuItem (aLEC.getMenuTree ().getDefaultMenuItemID ());

    final BootstrapNavbar aNavBar = new BootstrapNavbar ();
    aNavBar.addBrand (createLogo (aLEC), aLinkToStartPage);
    aNavBar.addBrand (new HCSpan ().addChild (CSMP.getApplicationTitle ()), aLinkToStartPage);

    _addNavbarLoginLogout (aLEC, aNavBar);
    return aNavBar;
  }

  @Nonnull
  public static IHCNode getMenuContent (@Nonnull final LayoutExecutionContext aLEC)
  {
    // Main menu
    final IMenuTree aMenuTree = aLEC.getMenuTree ();
    final MenuItemDeterminatorCallback aCallback = new MenuItemDeterminatorCallback (aMenuTree,
                                                                                     aLEC.getSelectedMenuItemID ())
    {
      @Override
      protected boolean isMenuItemValidToBeDisplayed (@Nonnull final IMenuObject aMenuObj)
      {
        // Don't show items that belong to the footer
        if (aMenuObj.attrs ().containsKey (CMenuPublic.FLAG_FOOTER))
          return false;

        // Use default code
        return super.isMenuItemValidToBeDisplayed (aMenuObj);
      }
    };
    return BootstrapMenuItemRenderer.createSideBarMenu (aLEC, aCallback);
  }

  @SuppressWarnings ("unchecked")
  @Nonnull
  public static IHCNode getPageContent (@Nonnull final ILayoutExecutionContext aLEC)
  {
    final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();

    // Get the requested menu item
    final IMenuItemPage aSelectedMenuItem = aLEC.getSelectedMenuItem ();

    // Resolve the page of the selected menu item (if found)
    IWebPage <WebPageExecutionContext> aDisplayPage;
    if (aSelectedMenuItem.matchesDisplayFilter ())
    {
      // Only if we have display rights!
      aDisplayPage = (IWebPage <WebPageExecutionContext>) aSelectedMenuItem.getPage ();
    }
    else
    {
      // No rights -> goto start page
      aDisplayPage = (IWebPage <WebPageExecutionContext>) aLEC.getMenuTree ().getDefaultMenuItem ().getPage ();
    }

    final WebPageExecutionContext aWPEC = new WebPageExecutionContext (aLEC, aDisplayPage);

    // Build page content: header + content
    final HCNodeList aPageContainer = new HCNodeList ();

    // First add the system message
    aPageContainer.addChild (BootstrapSystemMessage.createDefault ());

    // Handle 404 case here (see error404.jsp)
    if ("true".equals (aRequestScope.params ().getAsString ("httpError")))
    {
      final String sHttpStatusCode = aRequestScope.params ().getAsString ("httpStatusCode");
      final String sHttpStatusMessage = aRequestScope.params ().getAsString ("httpStatusMessage");
      final String sHttpRequestURI = aRequestScope.params ().getAsString ("httpRequestUri");
      aPageContainer.addChild (new BootstrapErrorBox ().addChild ("HTTP error " +
                                                                  sHttpStatusCode +
                                                                  " (" +
                                                                  sHttpStatusMessage +
                                                                  ")" +
                                                                  (StringHelper.hasText (sHttpRequestURI) ? " for request URI " +
                                                                                                            sHttpRequestURI
                                                                                                          : "")));
    }
    else
    {
      // Add the forced redirect content here
      if (aWPEC.params ().containsKey (ForcedRedirectManager.REQUEST_PARAMETER_PRG_ACTIVE))
        aPageContainer.addChild ((IHCNode) ForcedRedirectManager.getLastForcedRedirectContent (aDisplayPage.getID ()));
    }

    final String sHeaderText = aDisplayPage.getHeaderText (aWPEC);
    aPageContainer.addChild (BootstrapWebPageUIHandler.INSTANCE.createPageHeader (sHeaderText));
    // Main fill content
    try
    {
      aDisplayPage.getContent (aWPEC);
    }
    catch (final ForcedRedirectException ex)
    {
      throw ex;
    }
    catch (final RuntimeException ex)
    {
      new InternalErrorBuilder ().setDisplayLocale (aWPEC.getDisplayLocale ())
                                 .setRequestScope (aRequestScope)
                                 .setThrowable (ex)
                                 .setUIErrorHandlerFor (aWPEC.getNodeList ())
                                 .handle ();
    }
    // Add result
    aPageContainer.addChild (aWPEC.getNodeList ());
    return aPageContainer;
  }

  @Nonnull
  public static BootstrapContainer createDefaultFooter ()
  {
    final BootstrapContainer aDiv = new BootstrapContainer ().setID (CLayout.LAYOUT_AREAID_FOOTER).setFluid (true);
    aDiv.addChild (new HCP ().addChild (CSMP.getApplicationTitleAndVersion () +
                                        " with " +
                                        SMPServerConfiguration.getRESTType ().getDisplayName () +
                                        " API"));
    aDiv.addChild (new HCP ().addChild ("Created by ")
                             .addChild (HCA_MailTo.createLinkedEmail ("philip@helger.com", "Philip Helger"))
                             .addChild (" - Twitter: ")
                             .addChild (new HCA (new SimpleURL ("https://twitter.com/philiphelger")).setTargetBlank ()
                                                                                                    .addChild ("@philiphelger"))
                             .addChild (" - ")
                             .addChild (new HCA (new SimpleURL ("https://github.com/phax/phoss-smp")).setTargetBlank ()
                                                                                                             .addChild (CSMP.APPLICATION_TITLE +
                                                                                                                        " on GitHub")));
    return aDiv;
  }

  @Nonnull
  public static IHCNode getContent (@Nonnull final LayoutExecutionContext aLEC)
  {
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final HCNodeList ret = new HCNodeList ();

    // Header
    ret.addChild (_getNavbar (aLEC));

    final BootstrapContainer aOuterContainer = ret.addAndReturnChild (new BootstrapContainer ().setFluid (true));

    // Breadcrumbs
    if (false)
    {
      final BootstrapBreadcrumb aBreadcrumbs = BootstrapBreadcrumbProvider.createBreadcrumb (aLEC);
      aBreadcrumbs.addClasses (CBootstrapCSS.D_NONE, CBootstrapCSS.D_SM_BLOCK);
      aOuterContainer.addChild (aBreadcrumbs);
    }

    // Content
    try
    {
      aOuterContainer.addChild (getPageContent (aLEC));
    }
    catch (final ForcedRedirectException ex)
    {
      throw ex;
    }
    catch (final RuntimeException ex)
    {
      new InternalErrorBuilder ().setDisplayLocale (aDisplayLocale)
                                 .setRequestScope (aLEC.getRequestScope ())
                                 .setThrowable (ex)
                                 .setUIErrorHandlerFor (aOuterContainer)
                                 .handle ();
    }

    // Footer
    {
      final BootstrapContainer aDiv = createDefaultFooter ();

      final BootstrapMenuItemRendererHorz aRenderer = new BootstrapMenuItemRendererHorz (aDisplayLocale);
      final HCUL aUL = aDiv.addAndReturnChild (new HCUL ().addClass (CSS_CLASS_FOOTER_LINKS));
      for (final IMenuObject aMenuObj : s_aFooterObjects)
      {
        if (aMenuObj instanceof IMenuSeparator)
          aUL.addItem (aRenderer.renderSeparator (aLEC, (IMenuSeparator) aMenuObj));
        else
          if (aMenuObj instanceof IMenuItemPage)
            aUL.addItem (aRenderer.renderMenuItemPage (aLEC, (IMenuItemPage) aMenuObj, false, false, false));
          else
            if (aMenuObj instanceof IMenuItemExternal)
              aUL.addItem (aRenderer.renderMenuItemExternal (aLEC, (IMenuItemExternal) aMenuObj, false, false, false));
            else
              throw new IllegalStateException ("Unsupported menu object type!");
      }
      aOuterContainer.addChild (aDiv);
    }

    return ret;
  }
}
