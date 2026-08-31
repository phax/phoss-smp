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

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.phoss.smp.app.SMPInternalErrorHandler;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ESMPServiceGroupColumn;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.rest.SMPRestDataProvider;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.phoss.smp.ui.SMPExtensionUI;
import com.helger.phoss.smp.ui.SMPDataTablesOnDemand;
import com.helger.phoss.smp.ui.SMPPagination;
import com.helger.photon.bootstrap5.table.BootstrapTable;
import com.helger.photon.bootstrap5.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.icon.fontawesome6.EFontAwesome6Icon;
import com.helger.photon.ajax.decl.IAjaxFunctionDeclaration;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.ajax.DataTablesOnDemandRequest;
import com.helger.photon.uictrls.datatables.ajax.DataTablesOnDemandResult;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.url.SimpleURL;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.annotation.Nullable;

/**
 * This is the start page of the public application. It lists all available service groups.
 *
 * @author Philip Helger
 */
public final class PagePublicStart extends AbstractSMPWebPage
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PagePublicStart.class);

  private final IAjaxFunctionDeclaration m_aAjaxOnDemand = SMPDataTablesOnDemand.registerPublic (this::_getOnDemandData);

  public PagePublicStart (@NonNull @Nonempty final String sID)
  {
    super (sID, "Start page");
  }

  @Override
  @Nullable
  public String getHeaderText (@NonNull final WebPageExecutionContext aWPEC)
  {
    return "Managed participants on this SMP";
  }

  private void _addRow (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                        @NonNull final Locale aDisplayLocale,
                        @NonNull final HCRow aRow,
                        @NonNull final ISMPServiceGroup aServiceGroup,
                        final boolean bShowExtensionDetails)
  {
    final String sDisplayName = aServiceGroup.getParticipantIdentifier ().getURIEncoded ();

    aRow.addCell (sDisplayName);
    if (bShowExtensionDetails)
    {
      if (aServiceGroup.getExtensions ().extensions ().isNotEmpty ())
        aRow.addCell (SMPExtensionUI.getSerializedExtensions (aServiceGroup.getExtensions ()));
      else
        aRow.addCell ();
    }
    else
    {
      aRow.addCell (EPhotonCoreText.getYesOrNo (aServiceGroup.getExtensions ().extensions ().isNotEmpty (),
                                                aDisplayLocale));
    }
    final SMPRestDataProvider aDP = new SMPRestDataProvider (aRequestScope);
    aRow.addCell (new HCA (new SimpleURL (aDP.getServiceGroupHref (aServiceGroup.getParticipantIdentifier ()))).setTitle ("Perform SMP query on " +
                                                                                                                          sDisplayName)
                                                                                                               .setTargetBlank ()
                                                                                                               .addChild (EFontAwesome6Icon.UP_RIGHT_FROM_SQUARE.getAsNode ()));
  }

  /**
   * Provide the rows of a single page of the dynamic participant table. Only the entries of the
   * requested page are queried - nothing is kept in the session.
   *
   * @param aRequest
   *        The DataTables request. May not be <code>null</code>.
   * @param aRequestScope
   *        The current request scope. May not be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  private DataTablesOnDemandResult _getOnDemandData (@NonNull final DataTablesOnDemandRequest aRequest,
                                                     @NonNull final IRequestWebScopeWithoutResponse aRequestScope)
  {
    final WebPageExecutionContext aWPEC = new WebPageExecutionContext (LayoutExecutionContext.createForAjaxOrAction (aRequestScope),
                                                                       this);
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ISMPServiceGroupManager aSMPServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final boolean bShowExtensionDetails = SMPWebAppConfiguration.isStartPageExtensionsShow ();
    final String sSearchText = aRequest.getSearchText ();

    try
    {
      final ICommonsList <HCRow> aRows = new CommonsArrayList <> ();
      for (final ISMPServiceGroup aServiceGroup : aSMPServiceGroupMgr.getAllSMPServiceGroups (aRequest.getPagingSpec (),
                                                                                              sSearchText))
      {
        final HCRow aRow = new HCRow ();
        _addRow (aRequestScope, aDisplayLocale, aRow, aServiceGroup, bShowExtensionDetails);
        aRows.add (aRow);
      }
      return new DataTablesOnDemandResult (aSMPServiceGroupMgr.getSMPServiceGroupCount (),
                                           aSMPServiceGroupMgr.getSMPServiceGroupCount (sSearchText),
                                           aRows);
    }
    catch (final RuntimeException ex)
    {
      // E.g. MongoDB having invalid Participant IDs in the DB
      final String sError = "Internal Error listing all Service Groups";
      LOGGER.error (sError, ex);
      SMPInternalErrorHandler.createInternalErrorBuilder ()
                             .addErrorMessage (sError)
                             .setFromWebExecutionContext (aWPEC)
                             .setThrowable (ex)
                             .handle ();
      return DataTablesOnDemandResult.createEmpty ();
    }
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();

    if (SMPWebAppConfiguration.isStartPageParticipantsNone ())
    {
      // New in v5.0.4
      aNodeList.addChild (info ("This SMP has disabled the list of participants."));
      return;
    }

    EFontAwesome6Icon.registerResourcesForThisRequest ();
    final boolean bShowExtensionDetails = SMPWebAppConfiguration.isStartPageExtensionsShow ();

    if (SMPWebAppConfiguration.isStartPageDynamicTable ())
    {
      // Dynamic - paging, sorting and searching are done by DataTables via the AJAX function, so
      // only the rows of the currently displayed page are ever queried
      final HCTable aTable = new HCTable (new DTCol ("Participant ID").setName (ESMPServiceGroupColumn.PARTICIPANT_ID.getID ()),
                                          new DTCol (bShowExtensionDetails ? "Extension" : "Extension?").setOrderable (false),
                                          new BootstrapDTColAction (aDisplayLocale).setOrderable (false)).setID (getID ());
      aNodeList.addChild (aTable).addChild (SMPDataTablesOnDemand.createDataTables (aWPEC, aTable, m_aAjaxOnDemand, ESMPServiceGroupColumn.values ()));
      return;
    }

    // Static table - there is no DataTables that could do the paging, so the server side pagination
    // UI is used instead
    final ISMPServiceGroupManager aSMPServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    try
    {
      final String sSearchText = SMPPagination.getSearchText (aWPEC);
      final SMPPagination aPagination = new SMPPagination (aWPEC,
                                                           aSMPServiceGroupMgr.getSMPServiceGroupCount (sSearchText));
      final ICommonsList <ISMPServiceGroup> aServiceGroups = aSMPServiceGroupMgr.getAllSMPServiceGroups (aPagination.getPagingSpec (),
                                                                                                         sSearchText);

      final BootstrapTable aTable = new BootstrapTable ();
      aTable.setBordered (true);
      aTable.setCondensed (true);
      aTable.setStriped (true);
      aTable.addHeaderRow ()
            .addCell ("Participant ID")
            .addCell (bShowExtensionDetails ? "Extension" : "Extension?")
            .addCell (EPhotonCoreText.ACTIONS.getDisplayText (aDisplayLocale));

      for (final ISMPServiceGroup aServiceGroup : aServiceGroups)
        _addRow (aRequestScope, aDisplayLocale, aTable.addBodyRow (), aServiceGroup, bShowExtensionDetails);

      aNodeList.addChild (aPagination.getUI ());
      if (aTable.hasBodyRows ())
        aNodeList.addChild (aTable);
      else
        aNodeList.addChild (info (StringHelper.isNotEmpty (sSearchText) ? "No participant matches the search criteria."
                                                                        : "This SMP does not manage any participant yet."));
    }
    catch (final RuntimeException ex)
    {
      // E.g. MongoDB having invalid Participant IDs in the DB
      final String sError = "Internal Error listing all Service Groups";
      LOGGER.error (sError, ex);
      aNodeList.addChild (error (sError));
      SMPInternalErrorHandler.createInternalErrorBuilder ()
                             .addErrorMessage (sError)
                             .setFromWebExecutionContext (aWPEC)
                             .setThrowable (ex)
                             .handle ();
    }
  }
}
