/**
 * Copyright (C) 2014-2020 Philip Helger and contributors
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

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.attr.StringMap;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.url.ISimpleURL;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.nicename.NiceNameUI;
import com.helger.phoss.smp.rest2.Rest2Filter;
import com.helger.photon.app.url.LinkHelper;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.famfam.EFamFamIcon;

/**
 * Class to manage endpoints that belong to a service group. To use this page at
 * least one service group must exist.
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
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();

    final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
    aToolbar.addButton ("Create new Endpoint", createCreateURL (aWPEC), EDefaultIcon.NEW);
    aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.REFRESH);
    aToolbar.addButton ("Tree view", aWPEC.getLinkToMenuItem (CMenuSecure.MENU_ENDPOINT_TREE), EDefaultIcon.MAGNIFIER);
    aNodeList.addChild (aToolbar);

    final HCTable aTable = new HCTable (new DTCol ("Service group").setInitialSorting (ESortOrder.ASCENDING).setDataSort (0, 1, 2, 3),
                                        new DTCol ("Document type ID").setDataSort (1, 0, 2, 3),
                                        new DTCol ("Process ID").setDataSort (2, 0, 1, 3),
                                        new DTCol ("Transport profile").setDataSort (3, 0, 1, 2),
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
    for (final ISMPServiceInformation aServiceInfo : aServiceInfoMgr.getAllSMPServiceInformation ())
    {
      final ISMPServiceGroup aServiceGroup = aServiceInfo.getServiceGroup ();
      final IParticipantIdentifier aParticipantID = aServiceGroup.getParticpantIdentifier ();
      final IDocumentTypeIdentifier aDocTypeID = aServiceInfo.getDocumentTypeIdentifier ();
      for (final ISMPProcess aProcess : aServiceInfo.getAllProcesses ())
      {
        final IProcessIdentifier aProcessID = aProcess.getProcessIdentifier ();
        for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
        {
          final StringMap aParams = createParamMap (aServiceInfo, aProcess, aEndpoint);

          final HCRow aRow = aTable.addBodyRow ();
          aRow.addCell (new HCA (createViewURL (aWPEC, aServiceInfo, aParams)).addChild (aServiceGroup.getID ()));
          aRow.addCell (NiceNameUI.getDocumentTypeID (aDocTypeID, false));
          aRow.addCell (NiceNameUI.getProcessID (aDocTypeID, aProcessID, false));

          final String sTransportProfile = aEndpoint.getTransportProfile ();
          aRow.addCell (new HCA (createViewURL (aWPEC,
                                                CMenuSecure.MENU_TRANSPORT_PROFILES,
                                                sTransportProfile)).addChild (NiceNameUI.getTransportProfile (sTransportProfile, false)));

          final ISimpleURL aEditURL = createEditURL (aWPEC, aServiceInfo).addAll (aParams);
          final ISimpleURL aCopyURL = createCopyURL (aWPEC, aServiceInfo).addAll (aParams);
          final ISimpleURL aDeleteURL = createDeleteURL (aWPEC, aServiceInfo).addAll (aParams);
          final ISimpleURL aPreviewURL = LinkHelper.getURLWithServerAndContext (aParticipantID.getURIPercentEncoded () +
                                                                                Rest2Filter.PATH_SERVICES +
                                                                                aDocTypeID.getURIPercentEncoded ());
          aRow.addCell (new HCA (aEditURL).setTitle ("Edit endpoint").addChild (EDefaultIcon.EDIT.getAsNode ()),
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
