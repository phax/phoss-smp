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

import java.util.Locale;

import com.helger.annotation.Nonempty;
import com.helger.base.compare.ESortOrder;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.nicename.NiceNameUI;
import com.helger.phoss.smp.rest.SMPRestDataProvider;
import com.helger.phoss.smp.ui.cache.SMPTransportProfileCache;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.famfam.EFamFamIcon;
import com.helger.typeconvert.collection.StringMap;
import com.helger.url.ISimpleURL;
import com.helger.url.SimpleURL;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.annotation.Nonnull;

/**
 * Class to manage endpoints that belong to a service group. To use this page at least one service
 * group must exist.
 *
 * @author Philip Helger
 */
public final class PageSecureEndpointList extends AbstractPageSecureEndpoint
{
  public PageSecureEndpointList (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Endpoint List");
  }

  @Override
  protected void showListOfExistingObjects (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();

    final ICommonsList <ISMPServiceInformation> aAllServiceInfos = aServiceInfoMgr.getAllSMPServiceInformation ();

    // Count unique service groups
    final ICommonsSet <String> aServiceGroupIDs = new CommonsHashSet <> ();
    aAllServiceInfos.findAllMapped (ISMPServiceInformation::getServiceGroupID, aServiceGroupIDs::add);
    final boolean bHideDetails = aServiceGroupIDs.size () > 1000;

    final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
    aToolbar.addButton ("Create new Endpoint", createCreateURL (aWPEC), EDefaultIcon.NEW);
    aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.REFRESH);
    if (!bHideDetails)
      aToolbar.addButton ("Tree view",
                          aWPEC.getLinkToMenuItem (CMenuSecure.MENU_ENDPOINT_TREE),
                          EDefaultIcon.MAGNIFIER);
    aNodeList.addChild (aToolbar);

    // Use the cache here, to avoid too many DB lookups
    final SMPTransportProfileCache aTPCache = new SMPTransportProfileCache ();

    final HCTable aTable = new HCTable (new DTCol ("Service Group").setInitialSorting (ESortOrder.ASCENDING)
                                                                   .setDataSort (0, 1, 2, 3),
                                        new DTCol ("Document Type ID").setDataSort (1, 0, 2, 3),
                                        new DTCol ("Process ID").setDataSort (2, 0, 1, 3),
                                        new DTCol ("Transport Profile").setDataSort (3, 0, 1, 2),
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
    for (final ISMPServiceInformation aServiceInfo : aAllServiceInfos)
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

          final HCRow aRow = aTable.addBodyRow ();
          final ISimpleURL aViewURL = createViewURL (aWPEC, aServiceInfo, aParams);
          aRow.addCell (new HCA (aViewURL).addChild (aServiceInfo.getServiceGroupID ()));
          aRow.addCell (NiceNameUI.getDocumentTypeID (aDocTypeID, false));
          aRow.addCell (NiceNameUI.getProcessID (aDocTypeID, aProcessID, false));

          final String sTransportProfile = aEndpoint.getTransportProfile ();
          aRow.addCell (new HCA (createViewURL (aWPEC, CMenuSecure.MENU_TRANSPORT_PROFILES, sTransportProfile))
                                                                                                               .addChild (NiceNameUI.getTransportProfile (sTransportProfile,
                                                                                                                                                          aTPCache.getFromCache (sTransportProfile),
                                                                                                                                                          false)));

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
                                             .addChild (EFamFamIcon.SCRIPT_GO.getAsNode ()));
        }
      }
    }

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
    aNodeList.addChild (aTable).addChild (aDataTables);
  }
}
