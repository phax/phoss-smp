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
package com.helger.phoss.smp.ui;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.jquery.JQueryAjaxBuilder;
import com.helger.html.jscode.JSAssocArray;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.ui.ajax.CAjax;
import com.helger.photon.bootstrap4.ext.BootstrapSystemMessage;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapTechnicalUI;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.uictrls.datatables.DataTablesLengthMenu;
import com.helger.photon.uictrls.datatables.EDataTablesFilterType;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTables;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTablesI18N;
import com.helger.photon.uictrls.datatables.plugins.DataTablesPluginSearchHighlight;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.annotation.Nullable;

/**
 * Common UI helper methods
 *
 * @author Philip Helger
 */
@Immutable
public final class SMPCommonUI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPCommonUI.class);
  private static final DataTablesLengthMenu LENGTH_MENU = new DataTablesLengthMenu ().addItem (25)
                                                                                     .addItem (50)
                                                                                     .addItem (100)
                                                                                     .addItemAll ();

  private SMPCommonUI ()
  {}

  public static void init ()
  {
    BootstrapDataTables.setConfigurator ( (aLEC, aTable, aDataTables) -> {
      final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();
      aDataTables.setAutoWidth (false)
                 .setLengthMenu (LENGTH_MENU)
                 .setAjaxBuilder (new JQueryAjaxBuilder ().url (CAjax.DATATABLES.getInvocationURL (aRequestScope))
                                                          .data (new JSAssocArray ().add (AjaxExecutorDataTables.OBJECT_ID,
                                                                                          aTable.getID ())))
                 .setServerFilterType (EDataTablesFilterType.ALL_TERMS_PER_ROW)
                 .setTextLoadingURL (CAjax.DATATABLES_I18N.getInvocationURL (aRequestScope),
                                     AjaxExecutorDataTablesI18N.REQUEST_PARAM_LANGUAGE_ID)
                 .addPlugin (new DataTablesPluginSearchHighlight ());
    });

    // By default allow markdown in system message
    BootstrapSystemMessage.setDefaultUseMarkdown (SMPWebAppConfiguration.isWebAppSystemMessageUseMarkdown ());
  }

  @NonNull
  public static String getOwnerName (@NonNull @Nonempty final String sOwnerID)
  {
    // Will be a DB query
    final IUser aOwner = PhotonSecurityManager.getUserMgr ().getUserOfID (sOwnerID);
    return aOwner == null ? sOwnerID : aOwner.getLoginName () + " (" + aOwner.getDisplayName () + ")";
  }

  @Nullable
  public static HCNodeList getTechnicalDetailsUI (@Nullable final Throwable t)
  {
    if (t == null)
      return null;

    LOGGER.warn ("Technical details", t);
    return BootstrapTechnicalUI.getTechnicalDetailsNode (t, CSMPServer.DEFAULT_LOCALE);
  }

  @Nullable
  public static String getTechnicalDetailsString (@Nullable final Throwable t)
  {
    if (t == null)
      return null;

    LOGGER.warn ("Technical details", t);
    return BootstrapTechnicalUI.getTechnicalDetailsString (t, CSMPServer.DEFAULT_LOCALE);
  }
}
