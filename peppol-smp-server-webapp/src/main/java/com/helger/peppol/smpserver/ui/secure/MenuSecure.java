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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.peppol.smpserver.app.CApp;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.photon.basic.app.menu.IMenuItemPage;
import com.helger.photon.basic.app.menu.IMenuObjectFilter;
import com.helger.photon.basic.app.menu.IMenuTree;
import com.helger.photon.bootstrap3.pages.BootstrapPagesMenuConfigurator;
import com.helger.photon.bootstrap3.pages.security.BasePageSecurityChangePassword;
import com.helger.photon.security.menu.MenuObjectFilterUserAssignedToUserGroup;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uicore.page.system.BasePageShowChildren;

@Immutable
public final class MenuSecure
{
  private MenuSecure ()
  {}

  public static void init (@Nonnull final IMenuTree aMenuTree)
  {
    final MenuObjectFilterUserAssignedToUserGroup aFilterAdministrators = new MenuObjectFilterUserAssignedToUserGroup (CApp.USERGROUP_ADMINISTRATORS_ID);
    final IMenuObjectFilter aFilterPEPPOLDirectory = x -> SMPMetaManager.getSettings ()
                                                                        .isPEPPOLDirectoryIntegrationEnabled () &&
                                                          SMPMetaManager.hasBusinessCardMgr ();
    final IMenuObjectFilter aFilterSMLConnectionActive = x -> SMPMetaManager.getSettings ().isSMLActive ();
    final IMenuObjectFilter aFilterSMLConnectionActiveOrNeeded = x -> SMPMetaManager.getSettings ().isSMLActive () ||
                                                                      SMPMetaManager.getSettings ().isSMLNeeded ();

    if (SMPMetaManager.getUserMgr ().isSpecialUserManagementNeeded ())
    {
      // E.g. SQL version requires separate menu item
      aMenuTree.createRootItem (new PageSecureUser (CMenuSecure.MENU_USERS));
    }

    {
      final IMenuItemPage aServiceGroups = aMenuTree.createRootItem (new PageSecureServiceGroup (CMenuSecure.MENU_SERVICE_GROUPS));
      aMenuTree.createItem (aServiceGroups,
                            new PageSecureServiceGroupExchange (CMenuSecure.MENU_SERVICE_GROUPS_EXCHANGE));
    }
    {
      final IMenuItemPage aEndpoints = aMenuTree.createRootItem (new PageSecureEndpoint (CMenuSecure.MENU_ENDPOINTS));
      aMenuTree.createItem (aEndpoints, new PageSecureEndpointChangeURL (CMenuSecure.MENU_ENDPOINTS_CHANGE_URL));
      aMenuTree.createItem (aEndpoints,
                            new PageSecureEndpointChangeCertificate (CMenuSecure.MENU_ENDPOINTS_CHANGE_CERTIFICATE));
    }
    aMenuTree.createRootItem (new PageSecureRedirect (CMenuSecure.MENU_REDIRECTS));
    aMenuTree.createRootItem (new PageSecureBusinessCard (CMenuSecure.MENU_BUSINESS_CARDS))
             .setDisplayFilter (aFilterPEPPOLDirectory);
    aMenuTree.createRootItem (new PageSecureCertificateInformation (CMenuSecure.MENU_CERTIFICATE_INFORMATION));
    aMenuTree.createRootItem (new PageSecureTasks (CMenuSecure.MENU_TASKS));
    aMenuTree.createRootSeparator ();

    // Administrator
    {
      final IMenuItemPage aAdmin = aMenuTree.createRootItem (new BasePageShowChildren <WebPageExecutionContext> (CMenuSecure.MENU_ADMIN,
                                                                                                                 "Administration",
                                                                                                                 aMenuTree));
      {
        final IMenuItemPage aAdminSML = aMenuTree.createItem (aAdmin,
                                                              new BasePageShowChildren <> (CMenuSecure.MENU_SML,
                                                                                           "SML",
                                                                                           aMenuTree))
                                                 .setDisplayFilter (aFilterSMLConnectionActiveOrNeeded);
        aMenuTree.createItem (aAdminSML, new PageSecureSMLConfiguration (CMenuSecure.MENU_SML_INFO))
                 .setDisplayFilter (aFilterSMLConnectionActiveOrNeeded);
        aMenuTree.createItem (aAdminSML, new PageSecureSMLRegistration (CMenuSecure.MENU_SML_REGISTRATION))
                 .setDisplayFilter (aFilterSMLConnectionActive);
      }
      aMenuTree.createItem (aAdmin, new PageSecureSMPSettings (CMenuSecure.MENU_SMP_SETTINGS));
      aMenuTree.createItem (aAdmin, new PageSecureTransportProfile (CMenuSecure.MENU_TRANSPORT_PROFILES));
      aMenuTree.createItem (aAdmin,
                            new BasePageSecurityChangePassword <WebPageExecutionContext> (CMenuSecure.MENU_CHANGE_PASSWORD));
      BootstrapPagesMenuConfigurator.addAllItems (aMenuTree, aAdmin, aFilterAdministrators, CApp.DEFAULT_LOCALE);
    }

    // Default menu item
    aMenuTree.setDefaultMenuItemID (CMenuSecure.MENU_SERVICE_GROUPS);
  }
}
