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
package com.helger.phoss.smp.ui.pub;

import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.string.StringHelper;
import com.helger.base.url.URLHelper;
import com.helger.cache.regex.RegExHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.css.property.CCSSProperties;
import com.helger.css.propertyvalue.CCSSValue;
import com.helger.css.utils.CSSDataURLHelper;
import com.helger.html.css.DefaultCSSClassProvider;
import com.helger.html.css.ICSSClassProvider;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.ext.HCA_MailTo;
import com.helger.html.hc.html.HC_Target;
import com.helger.html.hc.html.IHCElementWithChildren;
import com.helger.html.hc.html.embedded.HCImg;
import com.helger.html.hc.html.grouping.HCP;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.html.hc.html.textlevel.HCStrong;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.servlet.SMPLogoutServlet;
import com.helger.photon.app.url.LinkHelper;
import com.helger.photon.bootstrap4.CBootstrapCSS;
import com.helger.photon.bootstrap4.breadcrumb.BootstrapBreadcrumb;
import com.helger.photon.bootstrap4.breadcrumb.BootstrapBreadcrumbProvider;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.layout.BootstrapContainer;
import com.helger.photon.bootstrap4.navbar.BootstrapNavbar;
import com.helger.photon.bootstrap4.navbar.BootstrapNavbarToggleable;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapMenuItemRenderer;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapMenuItemRendererHorz;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapPageRenderer;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.appid.CApplicationID;
import com.helger.photon.core.appid.PhotonGlobalState;
import com.helger.photon.core.execcontext.ILayoutExecutionContext;
import com.helger.photon.core.execcontext.ISimpleWebExecutionContext;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.photon.core.html.CLayout;
import com.helger.photon.core.menu.IMenuItemExternal;
import com.helger.photon.core.menu.IMenuItemPage;
import com.helger.photon.core.menu.IMenuObject;
import com.helger.photon.core.menu.IMenuSeparator;
import com.helger.photon.core.menu.IMenuTree;
import com.helger.photon.core.menu.MenuItemDeterminatorCallback;
import com.helger.photon.core.servlet.AbstractSecureApplicationServlet;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.util.SecurityHelper;
import com.helger.url.ISimpleURL;
import com.helger.url.SimpleURL;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * The public application viewport renderer (menu + content area)
 *
 * @author Philip Helger
 */
public final class SMPRendererPublic
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPRendererPublic.class);
  private static final ICSSClassProvider CSS_CLASS_FOOTER_LINKS = DefaultCSSClassProvider.create ("footer-links");
  private static final ICommonsList <IMenuObject> FOOTER_OBJECTS = new CommonsArrayList <> ();

  static
  {
    PhotonGlobalState.state (CApplicationID.APP_ID_PUBLIC).getMenuTree ().iterateAllMenuObjects (aCurrentObject -> {
      if (aCurrentObject.attrs ().containsKey (CMenuPublic.FLAG_FOOTER))
        FOOTER_OBJECTS.add (aCurrentObject);
    });
  }

  private SMPRendererPublic ()
  {}

  private static void _addNavbarLoginLogout (@NonNull final ILayoutExecutionContext aLEC,
                                             @NonNull final BootstrapNavbar aNavbar)
  {
    final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();
    final IUser aUser = aLEC.getLoggedInUser ();

    final BootstrapNavbarToggleable aToggleable = aNavbar.addAndReturnToggleable ();
    if (aUser != null)
    {
      final Locale aDisplayLocale = aLEC.getDisplayLocale ();
      aToggleable.addChild (new BootstrapButton ().addClass (CBootstrapCSS.ML_AUTO)
                                                  .addClass (CBootstrapCSS.MR_2)
                                                  .addChild ("Goto Administration")
                                                  .setOnClick (LinkHelper.getURLWithContext (AbstractSecureApplicationServlet.SERVLET_DEFAULT_PATH +
                                                                                             "/")));
      aToggleable.addAndReturnText ()
                 .addClass (CBootstrapCSS.MX_2)
                 .addChild ("Logged in as ")
                 .addChild (new HCStrong ().addChild (SecurityHelper.getUserDisplayName (aUser, aDisplayLocale)));

      aToggleable.addChild (new BootstrapButton ().addClass (CBootstrapCSS.MX_2)
                                                  .setOnClick (LinkHelper.getURLWithContext (aRequestScope,
                                                                                             SMPLogoutServlet.SERVLET_DEFAULT_PATH))
                                                  .addChild (EPhotonCoreText.LOGIN_LOGOUT.getDisplayText (aDisplayLocale)));
    }
  }

  // Create the logo URL only once
  private static final ISimpleURL CUSTOM_LOGO_URL_CACHE;
  static
  {
    final String sInlineURL = SMPWebAppConfiguration.getPublicLogoInline ();
    if (StringHelper.isNotEmpty (sInlineURL) && CSSDataURLHelper.isDataURL (sInlineURL))
    {
      // Use custom inline data URL
      CUSTOM_LOGO_URL_CACHE = new SimpleURL (sInlineURL);
      LOGGER.info ("Using a custom inline data URL as logo");
    }
    else
    {
      final String sExternalURL = SMPWebAppConfiguration.getPublicLogoExternalUrl ();
      if (StringHelper.isNotEmpty (sExternalURL) && URLHelper.getAsURL (sExternalURL) != null)
      {
        // Use custom external URL
        CUSTOM_LOGO_URL_CACHE = new SimpleURL (sExternalURL);
        LOGGER.info ("Using a custom external URL as logo: '" + sExternalURL + "'");
      }
      else
      {
        final String sInternalURL = SMPWebAppConfiguration.getPublicLogoInternalUrl ();
        if (StringHelper.isNotEmpty (sInternalURL))
        {
          // Use custom internal URL
          CUSTOM_LOGO_URL_CACHE = new SimpleURL (sInternalURL);
          LOGGER.info ("Using a custom internal URL as logo: '" + sInternalURL + "'");
        }
        else
        {
          // Use the default logo
          CUSTOM_LOGO_URL_CACHE = null;
        }
      }
    }
  }

  /**
   * Create the logo node. This applies to public and secure mode.
   *
   * @param aSWEC
   *        The current web execution context
   * @return The HC Node. Never <code>null</code>.
   */
  @NonNull
  public static IHCNode createLogo (@NonNull final ISimpleWebExecutionContext aSWEC)
  {
    final IRequestWebScopeWithoutResponse aRequestScope = aSWEC.getRequestScope ();

    final ISimpleURL aLogoHref;
    if (CUSTOM_LOGO_URL_CACHE != null)
      aLogoHref = CUSTOM_LOGO_URL_CACHE;
    else
    {
      // The default logo
      aLogoHref = LinkHelper.getStreamURL (aRequestScope, "/image/phoss-smp-136-50.png");
    }
    return new HCImg ().setSrc (aLogoHref)
                       .addStyle (CCSSProperties.MARGIN.newValue ("-15px"))
                       .addStyle (CCSSProperties.VERTICAL_ALIGN.newValue (CCSSValue.TOP))
                       .addStyle (CCSSProperties.PADDING.newValue ("0 6px"));
  }

  @NonNull
  public static IHCNode createLogoBig (@NonNull final ISimpleWebExecutionContext aSWEC)
  {
    final IRequestWebScopeWithoutResponse aRequestScope = aSWEC.getRequestScope ();

    final ISimpleURL aLogoHref;
    if (CUSTOM_LOGO_URL_CACHE != null)
      aLogoHref = CUSTOM_LOGO_URL_CACHE;
    else
    {
      // The default logo
      aLogoHref = LinkHelper.getStreamURL (aRequestScope, "/image/phoss-smp-272-100.png");
    }
    return new HCImg ().setSrc (aLogoHref);
  }

  @NonNull
  private static BootstrapNavbar _getNavbar (@NonNull final ILayoutExecutionContext aLEC)
  {
    final ISimpleURL aLinkToStartPage = aLEC.getLinkToMenuItem (aLEC.getMenuTree ().getDefaultMenuItemID ());

    final BootstrapNavbar aNavBar = new BootstrapNavbar ();
    aNavBar.addBrand (createLogo (aLEC), aLinkToStartPage);
    aNavBar.addBrand (new HCSpan ().addChild (CSMP.getApplicationTitle ()), aLinkToStartPage);

    _addNavbarLoginLogout (aLEC, aNavBar);
    return aNavBar;
  }

  @NonNull
  public static IHCNode getMenuContent (@NonNull final LayoutExecutionContext aLEC)
  {
    // Main menu
    final IMenuTree aMenuTree = aLEC.getMenuTree ();
    final MenuItemDeterminatorCallback aCallback = new MenuItemDeterminatorCallback (aMenuTree,
                                                                                     aLEC.getSelectedMenuItemID ())
    {
      @Override
      protected boolean isMenuItemValidToBeDisplayed (@NonNull final IMenuObject aMenuObj)
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

  /**
   * @param bShowApplicationName
   *        <code>true</code> to show the application name and version, <code>false</code> to hide
   *        it.
   * @param bShowSource
   *        <code>true</code> to show the link to the source, <code>false</code> to hide it.
   * @param bShowAuthor
   *        <code>true</code> to show the author, <code>false</code> to hide it.
   * @return The footer to be used for /public and /secure. Never <code>null</code> but maybe empty.
   */
  @NonNull
  public static BootstrapContainer createDefaultFooter (final boolean bShowApplicationName,
                                                        final boolean bShowSource,
                                                        final boolean bShowAuthor)
  {
    final BootstrapContainer aContainer = new BootstrapContainer ().setID (CLayout.LAYOUT_AREAID_FOOTER)
                                                                   .addClass (CBootstrapCSS.BG_LIGHT)
                                                                   .setFluid (true);
    if (bShowApplicationName)
    {
      aContainer.addChild (new HCP ().addChild (CSMP.getApplicationTitleAndVersion () +
                                                " with " +
                                                SMPServerConfiguration.getRESTType ().getDisplayName () +
                                                " API"));
    }
    // By
    {
      final HCP aBy = new HCP ();

      // Author
      if (bShowAuthor)
        aBy.addChild ("Created by ").addChild (HCA_MailTo.createLinkedEmail ("philip@helger.com", "Philip Helger"));

      // Source
      if (bShowSource)
      {
        if (aBy.hasChildren ())
          aBy.addChild (" - ");
        aBy.addChild (new HCA (new SimpleURL ("https://github.com/phax/phoss-smp")).setTargetBlank ()
                                                                                   .addChild (CSMP.APPLICATION_TITLE +
                                                                                              " on GitHub"));
      }
      if (aBy.hasChildren ())
        aContainer.addChild (aBy);
    }
    // Imprint
    if (SMPWebAppConfiguration.isImprintEnabled ())
    {
      final String sImprintText = SMPWebAppConfiguration.getImprintText ();
      if (StringHelper.isNotEmpty (sImprintText))
      {
        final ISimpleURL aImprintHref = SMPWebAppConfiguration.getImprintHref ();
        final IHCElementWithChildren <?> aNode;
        if (aImprintHref != null)
        {
          // Link and text
          final String sImprintTarget = SMPWebAppConfiguration.getImprintTarget ();
          final HC_Target aTarget = StringHelper.isNotEmpty (sImprintTarget) ? new HC_Target (sImprintTarget) : null;
          aNode = new HCA (aImprintHref).addChild (sImprintText).setTarget (aTarget);
        }
        else
        {
          // Text only
          aNode = new HCSpan ().addChild (sImprintText);
        }
        // Already trimmed
        final String sImprintCSSClasses = SMPWebAppConfiguration.getImprintCSSClasses ();
        if (StringHelper.isNotEmpty (sImprintCSSClasses))
        {
          final ICommonsSet <String> aUniqueNames = new CommonsHashSet <> (RegExHelper.getSplitToList (sImprintCSSClasses,
                                                                                                       "\\s+"));
          for (final String sCSSClass : aUniqueNames)
            aNode.addClass (DefaultCSSClassProvider.create (sCSSClass));
        }
        aContainer.addChild (new HCP ().addChild ("Imprint ").addChild (aNode));
      }
    }
    return aContainer;
  }

  @NonNull
  public static IHCNode getContent (@NonNull final LayoutExecutionContext aLEC)
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
    aOuterContainer.addChild (BootstrapPageRenderer.getPageContent (aLEC));

    // Footer
    {
      final BootstrapContainer aDiv = createDefaultFooter (SMPWebAppConfiguration.isPublicShowApplicationName (),
                                                           SMPWebAppConfiguration.isPublicShowSource (),
                                                           SMPWebAppConfiguration.isPublicShowAuthor ());
      {
        final BootstrapMenuItemRendererHorz aRenderer = new BootstrapMenuItemRendererHorz (aDisplayLocale);
        final HCUL aUL = new HCUL ().addClass (CSS_CLASS_FOOTER_LINKS);
        for (final IMenuObject aMenuObj : FOOTER_OBJECTS)
        {
          if (aMenuObj instanceof IMenuSeparator)
            aUL.addItem (aRenderer.renderSeparator (aLEC, (IMenuSeparator) aMenuObj));
          else
            if (aMenuObj instanceof IMenuItemPage)
              aUL.addItem (aRenderer.renderMenuItemPage (aLEC, (IMenuItemPage) aMenuObj, false, false, false));
            else
              if (aMenuObj instanceof IMenuItemExternal)
                aUL.addItem (aRenderer.renderMenuItemExternal (aLEC,
                                                               (IMenuItemExternal) aMenuObj,
                                                               false,
                                                               false,
                                                               false));
              else
                throw new IllegalStateException ("Unsupported menu object type!");
        }
        if (aUL.hasChildren ())
          aDiv.addChild (aUL);
      }
      if (aDiv.hasChildren ())
        aOuterContainer.addChild (aDiv);
    }
    return ret;
  }
}
