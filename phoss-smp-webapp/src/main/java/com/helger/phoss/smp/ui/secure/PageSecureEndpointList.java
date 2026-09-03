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

import java.util.Locale;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppol.ui.nicename.NiceNameUI;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.serviceinfo.ESMPServiceInformationColumn;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpointHelper;
import com.helger.phoss.smp.nicename.SMPNiceNameUI;
import com.helger.phoss.smp.rest.SMPRestDataProvider;
import com.helger.phoss.smp.ui.SMPDataTablesOnDemand;
import com.helger.phoss.smp.ui.cache.SMPTransportProfileCache;
import com.helger.photon.ajax.decl.IAjaxFunctionDeclaration;
import com.helger.photon.bootstrap5.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap5.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.photon.icon.fontawesome6.EFontAwesome6Icon;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.ajax.DataTablesOnDemandRequest;
import com.helger.photon.uictrls.datatables.ajax.DataTablesOnDemandResult;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.typeconvert.collection.StringMap;
import com.helger.url.ISimpleURL;
import com.helger.url.SimpleURL;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Class to manage endpoints that belong to a service group. To use this page at least one service
 * group must exist.
 *
 * @author Philip Helger
 */
public final class PageSecureEndpointList extends AbstractPageSecureEndpoint
{
  private final IAjaxFunctionDeclaration m_aAjaxOnDemand = SMPDataTablesOnDemand.registerSecure (this::_getOnDemandData);

  public PageSecureEndpointList (@NonNull @Nonempty final String sID)
  {
    super (sID, "Endpoint List");
  }

  @NonNull
  private HCTable _createTable (@NonNull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    // The column names are the IDs of ESMPServiceInformationColumn, so that the sort order
    // requested by the client can be resolved onto the respective SQL column or MongoDB field.
    // Process ID, Transport Profile and Validity live in child entities and can therefore not be
    // sorted by in the data store.
    return new HCTable (new DTCol ("Service Group").setName (ESMPServiceInformationColumn.SERVICE_GROUP.getID ()),
                        new DTCol ("Document Type ID").setName (ESMPServiceInformationColumn.DOCUMENT_TYPE_ID.getID ()),
                        new DTCol ("Process ID").setOrderable (false),
                        new DTCol ("Transport Profile").setOrderable (false),
                        new DTCol ("Validity").setOrderable (false),
                        new BootstrapDTColAction (aDisplayLocale).setOrderable (false)).setID (getID ());
  }

  /**
   * Provide the rows of a single page. Note that the paging happens on Service Information level,
   * whereas a single row represents a single endpoint - so one page may contain more rows than the
   * page size, because a Service Information contains 1-n processes with 1-n endpoints each.
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
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final String sSearchText = aRequest.getSearchText ();

    // Use the cache here, to avoid too many DB lookups
    final SMPTransportProfileCache aTPCache = new SMPTransportProfileCache ();
    final ICommonsList <HCRow> aRows = new CommonsArrayList <> ();
    for (final ISMPServiceInformation aServiceInfo : aServiceInfoMgr.getAllSMPServiceInformation (aRequest.getPagingSpec (),
                                                                                                  sSearchText))
    {
      final IParticipantIdentifier aParticipantID = aServiceInfo.getServiceGroupParticipantIdentifier ();
      final IDocumentTypeIdentifier aDocTypeID = aServiceInfo.getDocumentTypeIdentifier ();
      final SMPRestDataProvider aDP = new SMPRestDataProvider (aRequestScope);

      for (final ISMPProcess aProcess : aServiceInfo.getAllProcesses ())
      {
        final IProcessIdentifier aProcessID = aProcess.getProcessIdentifier ();
        for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
        {
          final StringMap aParams = createParamMap (aServiceInfo, aProcess, aEndpoint);

          final HCRow aRow = new HCRow ();
          final ISimpleURL aViewURL = createViewURL (aWPEC, aServiceInfo, aParams);
          aRow.addCell (new HCA (aViewURL).addChild (aServiceInfo.getServiceGroupID ()));
          aRow.addCell (NiceNameUI.createDocTypeID (aDocTypeID, false));
          aRow.addCell (NiceNameUI.createProcessID (aDocTypeID, aProcessID, false));

          final String sTransportProfile = aEndpoint.getTransportProfile ();
          aRow.addCell (new HCA ().setHref (createViewURL (aWPEC,
                                                           CMenuSecure.MENU_TRANSPORT_PROFILES,
                                                           sTransportProfile))
                                  .addChild (SMPNiceNameUI.getTransportProfile (sTransportProfile,
                                                                                aTPCache.getFromCache (sTransportProfile),
                                                                                false)));

          aRow.addCell (SMPEndpointHelper.getAsValidityString (aEndpoint.getServiceActivationDate (),
                                                               aEndpoint.getServiceExpirationDate (),
                                                               aDisplayLocale));

          final ISimpleURL aEditURL = createEditURL (aWPEC, aServiceInfo).addAll (aParams);
          final ISimpleURL aCopyURL = createCopyURL (aWPEC, aServiceInfo).addAll (aParams);
          final ISimpleURL aDeleteURL = createDeleteURL (aWPEC, aServiceInfo).addAll (aParams);
          final ISimpleURL aPreviewURL = new SimpleURL (aDP.getServiceMetadataReferenceHref (aParticipantID,
                                                                                             aDocTypeID));
          aRow.addCell (new HCA (aViewURL).setTitle ("View endpoint").addChild (EDefaultIcon.MAGNIFIER.getAsNode ()),
                        new HCTextNode (" "),
                        new HCA (aEditURL).setTitle ("Edit endpoint").addChild (EDefaultIcon.EDIT.getAsNode ()),
                        new HCTextNode (" "),
                        new HCA (aCopyURL).setTitle ("Copy endpoint").addChild (EDefaultIcon.COPY.getAsNode ()),
                        new HCTextNode (" "),
                        new HCA (aDeleteURL).setTitle ("Delete endpoint").addChild (EDefaultIcon.DELETE.getAsNode ()),
                        new HCTextNode (" "),
                        new HCA (aPreviewURL).setTitle ("Perform SMP query on endpoint")
                                             .setTargetBlank ()
                                             .addChild (EFontAwesome6Icon.UP_RIGHT_FROM_SQUARE.getAsNode ()));
          aRows.add (aRow);
        }
      }
    }
    return new DataTablesOnDemandResult (aServiceInfoMgr.getSMPServiceInformationCount (),
                                         aServiceInfoMgr.getSMPServiceInformationCount (sSearchText),
                                         aRows);
  }

  @Override
  protected void showListOfExistingObjects (@NonNull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    EFontAwesome6Icon.registerResourcesForThisRequest ();

    // Toolbar
    final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
    aToolbar.addButton ("Create new Endpoint", createCreateURL (aWPEC), EDefaultIcon.NEW);
    aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.REFRESH);
    aToolbar.addButton ("Tree view", aWPEC.getLinkToMenuItem (CMenuSecure.MENU_ENDPOINT_TREE), EDefaultIcon.MAGNIFIER);
    aNodeList.addChild (aToolbar);

    // The rows are filled by the AJAX function only
    final HCTable aTable = _createTable (aWPEC);
    aNodeList.addChild (aTable).addChild (SMPDataTablesOnDemand.createDataTables (aWPEC, aTable, m_aAjaxOnDemand, ESMPServiceInformationColumn.values ()));
  }
}
