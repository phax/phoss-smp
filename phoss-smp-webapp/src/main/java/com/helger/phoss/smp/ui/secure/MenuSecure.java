/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
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
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.photon.bootstrap4.pages.BootstrapPagesMenuConfigurator;
import com.helger.photon.bootstrap4.pages.security.BasePageSecurityChangePassword;
import com.helger.photon.core.menu.IMenuItemPage;
import com.helger.photon.core.menu.IMenuObject;
import com.helger.photon.core.menu.IMenuObjectFilter;
import com.helger.photon.core.menu.IMenuTree;
import com.helger.photon.core.menu.filter.MenuObjectFilterUserAssignedToUserGroup;
import com.helger.photon.security.util.SecurityHelper;
import com.helger.photon.uicore.page.system.BasePageShowChildren;
import com.helger.web.scope.singleton.AbstractRequestWebSingleton;

@Immutable
public final class MenuSecure
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MenuSecure.class);

  private MenuSecure ()
  {}

  /**
   * Cache the check if the current user is in a user group or not, to avoid that too many queries
   * are performed. If necessary, this class can be made more public if required on other places.
   * 
   * @author Philip Helger
   * @since v7.2.7
   */
  public static final class CurrentUserToUserGroupAssignmentCache extends AbstractRequestWebSingleton
  {
    private final ICommonsMap <String, Boolean> m_aMap = new CommonsHashMap <> ();

    @Deprecated
    public CurrentUserToUserGroupAssignmentCache ()
    {}

    public static final CurrentUserToUserGroupAssignmentCache getInstance ()
    {
      return getRequestSingleton (CurrentUserToUserGroupAssignmentCache.class);
    }

    boolean isCurrentUserAssignedToUserGroup (final String sUserGroupID)
    {
      return m_aMap.computeIfAbsent (sUserGroupID,
                                     k -> Boolean.valueOf (SecurityHelper.isCurrentUserAssignedToUserGroup (sUserGroupID)))
                   .booleanValue ();
    }
  }

  public static void init (@Nonnull final IMenuTree aMenuTree)
  {
    final MenuObjectFilterUserAssignedToUserGroup aFilterAdministrators = new MenuObjectFilterUserAssignedToUserGroup (CSMP.USERGROUP_ADMINISTRATORS_ID)
    {
      @Override
      public boolean test (@Nullable final IMenuObject aValue)
      {
        return CurrentUserToUserGroupAssignmentCache.getInstance ()
                                                    .isCurrentUserAssignedToUserGroup (getUserGroupID ());
      }
    };
    final IMenuObjectFilter aFilterPeppolDirectory = x -> SMPMetaManager.getSettings ()
                                                                        .isDirectoryIntegrationEnabled () &&
                                                          SMPMetaManager.hasBusinessCardMgr ();
    final IMenuObjectFilter aFilterSMLConnectionActive = x -> SMPMetaManager.getSettings ().isSMLEnabled ();
    final IMenuObjectFilter aFilterSMLConnectionActiveOrNeeded = x -> {
      final ISMPSettings aSettings = SMPMetaManager.getSettings ();
      return aSettings.isSMLEnabled () || aSettings.isSMLRequired ();
    };

    aMenuTree.createRootItem (new PageSecureHome (CMenuSecure.MENU_HOME, aMenuTree));

    {
      final IMenuItemPage aServiceGroups = aMenuTree.createRootItem (new PageSecureServiceGroup (CMenuSecure.MENU_SERVICE_GROUPS));
      aMenuTree.createItem (aServiceGroups, new PageSecureServiceGroupExport (CMenuSecure.MENU_SERVICE_GROUPS_EXPORT));
      aMenuTree.createItem (aServiceGroups, new PageSecureServiceGroupImport (CMenuSecure.MENU_SERVICE_GROUPS_IMPORT));
      aMenuTree.createItem (aServiceGroups,
                            new PageSecureServiceGroupMigrationOutbound (CMenuSecure.MENU_SERVICE_GROUPS_MIGRATE_OUTBOUND));
      aMenuTree.createItem (aServiceGroups,
                            new PageSecureServiceGroupMigrationInbound (CMenuSecure.MENU_SERVICE_GROUPS_MIGRATE_INBOUND));
    }
    {
      final IMenuItemPage aEndpoints = aMenuTree.createRootItem (new BasePageShowChildren <> (CMenuSecure.MENU_ENDPOINTS,
                                                                                              "Endpoints",
                                                                                              aMenuTree));
      aMenuTree.createItem (aEndpoints, new PageSecureEndpointList (CMenuSecure.MENU_ENDPOINT_LIST));
      aMenuTree.createItem (aEndpoints, new PageSecureEndpointTree (CMenuSecure.MENU_ENDPOINT_TREE));
      aMenuTree.createItem (aEndpoints, new PageSecureEndpointChangeURL (CMenuSecure.MENU_ENDPOINTS_CHANGE_URL));
      aMenuTree.createItem (aEndpoints,
                            new PageSecureEndpointChangeCertificate (CMenuSecure.MENU_ENDPOINTS_CHANGE_CERTIFICATE));
    }
    aMenuTree.createRootItem (new PageSecureRedirect (CMenuSecure.MENU_REDIRECTS));
    aMenuTree.createRootItem (new PageSecureBusinessCard (CMenuSecure.MENU_BUSINESS_CARDS))
             .setDisplayFilter (aFilterPeppolDirectory);
    aMenuTree.createRootItem (new PageSecureCertificateInformation (CMenuSecure.MENU_CERTIFICATE_INFORMATION));
    aMenuTree.createRootItem (new PageSecureTasksProblems (CMenuSecure.MENU_TASKS));
    aMenuTree.createRootSeparator ();

    // Administrator
    {
      final IMenuItemPage aAdmin = aMenuTree.createRootItem (new BasePageShowChildren <> (CMenuSecure.MENU_ADMIN,
                                                                                          "Administration",
                                                                                          aMenuTree));
      {
        final IMenuItemPage aAdminSML = aMenuTree.createItem (aAdmin,
                                                              new BasePageShowChildren <> (CMenuSecure.MENU_SML,
                                                                                           "SML",
                                                                                           aMenuTree))
                                                 .setDisplayFilter (aFilterSMLConnectionActiveOrNeeded);
        aMenuTree.createItem (aAdminSML, new PageSecureSMLConfiguration (CMenuSecure.MENU_SML_CONFIGURATION))
                 .setDisplayFilter (aFilterSMLConnectionActiveOrNeeded);
        aMenuTree.createItem (aAdminSML, new PageSecureSMLRegCreate (CMenuSecure.MENU_SML_REG_CREATE))
                 .setDisplayFilter (aFilterSMLConnectionActive);
        aMenuTree.createItem (aAdminSML, new PageSecureSMLRegUpdate (CMenuSecure.MENU_SML_REG_UPDATE))
                 .setDisplayFilter (aFilterSMLConnectionActive);
        aMenuTree.createItem (aAdminSML, new PageSecureSMLRegDelete (CMenuSecure.MENU_SML_REG_DELETE))
                 .setDisplayFilter (aFilterSMLConnectionActive);
        aMenuTree.createItem (aAdminSML, new PageSecureSMLCertificateUpdate (CMenuSecure.MENU_SML_CERTIFICATE_UPDATE))
                 .setDisplayFilter (aFilterSMLConnectionActive);
      }
      aMenuTree.createItem (aAdmin, new PageSecureSMPSettings (CMenuSecure.MENU_SMP_SETTINGS));
      aMenuTree.createItem (aAdmin, new PageSecureSMPIdentifierMappings (CMenuSecure.MENU_SMP_IDENTIFIER_MAPPINGS));
      aMenuTree.createItem (aAdmin, new PageSecureTransportProfiles (CMenuSecure.MENU_TRANSPORT_PROFILES));
      aMenuTree.createItem (aAdmin, new BasePageSecurityChangePassword <> (CMenuSecure.MENU_CHANGE_PASSWORD));
      BootstrapPagesMenuConfigurator.addAllItems (aMenuTree, aAdmin, aFilterAdministrators, CSMPServer.DEFAULT_LOCALE);

      if (SMPWebAppConfiguration.isWebAppPageSessionManagmentDisabled ())
      {
        LOGGER.warn ("The 'Administration / Monitoring / Sessions' page was explicitly disabled via the configuration.");
        aMenuTree.removeItemWithID (BootstrapPagesMenuConfigurator.MENU_ADMIN_MONITORING_SESSIONS);
      }
      if (SMPWebAppConfiguration.isWebAppPageEnvVarsDisabled ())
      {
        LOGGER.warn ("The 'Administration / System Information / Environment Variables' page was explicitly disabled via the configuration.");
        aMenuTree.removeItemWithID (BootstrapPagesMenuConfigurator.MENU_ADMIN_SYSINFO_ENVVARS);
      }
    }

    // Default menu item
    aMenuTree.setDefaultMenuItemID (CMenuSecure.MENU_HOME);
  }
}
