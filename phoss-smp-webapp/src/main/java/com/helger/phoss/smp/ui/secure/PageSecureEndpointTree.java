/**
 * Copyright (C) 2014-2021 Philip Helger and contributors
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

import com.helger.collection.multimap.IMultiMapListBased;
import com.helger.collection.multimap.MultiHashMapArrayListBased;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.attr.StringMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.url.ISimpleURL;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCLI;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.tabular.HCCol;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.nicename.NiceNameUI;
import com.helger.phoss.smp.rest2.Rest2Filter;
import com.helger.photon.app.url.LinkHelper;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.table.BootstrapTable;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.famfam.EFamFamIcon;

/**
 * Class to manage endpoints that belong to a service group. To use this page at
 * least one service group must exist.
 *
 * @author Philip Helger
 */
public final class PageSecureEndpointTree extends AbstractPageSecureEndpoint
{
  public PageSecureEndpointTree (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Endpoint Tree");
  }

  @Override
  protected void showListOfExistingObjects (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();

    final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
    aToolbar.addButton ("Create new Endpoint", createCreateURL (aWPEC), EDefaultIcon.NEW);
    aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.REFRESH);
    aToolbar.addButton ("List view", aWPEC.getLinkToMenuItem (CMenuSecure.MENU_ENDPOINT_LIST), EDefaultIcon.MAGNIFIER);
    aNodeList.addChild (aToolbar);

    // Create list of service groups
    final IMultiMapListBased <ISMPServiceGroup, ISMPServiceInformation> aMap = new MultiHashMapArrayListBased <> ();
    aServiceInfoMgr.getAllSMPServiceInformation ().forEach (x -> aMap.putSingle (x.getServiceGroup (), x));

    final HCUL aULSG = new HCUL ();
    final ICommonsList <ISMPServiceGroup> aServiceGroups = aServiceGroupMgr.getAllSMPServiceGroups ()
                                                                           .getSortedInline (ISMPServiceGroup.comparator ());
    for (final ISMPServiceGroup aServiceGroup : aServiceGroups)
    {
      // Print service group
      final IParticipantIdentifier aParticipantID = aServiceGroup.getParticpantIdentifier ();
      final HCLI aLISG = aULSG.addAndReturnItem (new HCA (createViewURL (aWPEC,
                                                                         CMenuSecure.MENU_SERVICE_GROUPS,
                                                                         aServiceGroup)).addChild (aParticipantID.getURIEncoded ()));
      final HCUL aULDT = new HCUL ();

      final ICommonsList <ISMPServiceInformation> aServiceInfos = aMap.get (aServiceGroup);
      if (aServiceInfos != null)
      {
        for (final ISMPServiceInformation aServiceInfo : aServiceInfos.getSortedInline (ISMPServiceInformation.comparator ()))
        {
          final HCUL aULP = new HCUL ();
          final IDocumentTypeIdentifier aDocTypeID = aServiceInfo.getDocumentTypeIdentifier ();

          final ICommonsList <ISMPProcess> aProcesses = aServiceInfo.getAllProcesses ().getSortedInline (ISMPProcess.comparator ());
          for (final ISMPProcess aProcess : aProcesses)
          {
            final BootstrapTable aEPTable = new BootstrapTable (HCCol.perc (40), HCCol.perc (40), HCCol.perc (20)).setBordered (true);
            final ICommonsList <ISMPEndpoint> aEndpoints = aProcess.getAllEndpoints ().getSortedInline (ISMPEndpoint.comparator ());
            for (final ISMPEndpoint aEndpoint : aEndpoints)
            {
              final StringMap aParams = createParamMap (aServiceInfo, aProcess, aEndpoint);

              final HCRow aBodyRow = aEPTable.addBodyRow ();

              final String sTransportProfile = aEndpoint.getTransportProfile ();
              final ISimpleURL aViewURL = createViewURL (aWPEC, aServiceInfo, aParams);
              aBodyRow.addCell (new HCA (aViewURL).addChild (NiceNameUI.getTransportProfile (sTransportProfile, false)));

              aBodyRow.addCell (aEndpoint.getEndpointReference ());

              final ISimpleURL aEditURL = createEditURL (aWPEC, aServiceInfo).addAll (aParams);
              final ISimpleURL aCopyURL = createCopyURL (aWPEC, aServiceInfo).addAll (aParams);
              final ISimpleURL aDeleteURL = createDeleteURL (aWPEC, aServiceInfo).addAll (aParams);
              final ISimpleURL aPreviewURL = LinkHelper.getURLWithServerAndContext (aParticipantID.getURIPercentEncoded () +
                                                                                    Rest2Filter.PATH_SERVICES +
                                                                                    aDocTypeID.getURIPercentEncoded ());
              aBodyRow.addAndReturnCell (new HCA (aViewURL).setTitle ("View endpoint").addChild (EDefaultIcon.MAGNIFIER.getAsNode ()),
                                         new HCTextNode (" "),
                                         new HCA (aEditURL).setTitle ("Edit endpoint").addChild (EDefaultIcon.EDIT.getAsNode ()),
                                         new HCTextNode (" "),
                                         new HCA (aCopyURL).setTitle ("Copy endpoint").addChild (EDefaultIcon.COPY.getAsNode ()),
                                         new HCTextNode (" "),
                                         new HCA (aDeleteURL).setTitle ("Delete endpoint").addChild (EDefaultIcon.DELETE.getAsNode ()),
                                         new HCTextNode (" "),
                                         new HCA (aPreviewURL).setTitle ("Perform SMP query on endpoint")
                                                              .setTargetBlank ()
                                                              .addChild (EFamFamIcon.SCRIPT_GO.getAsNode ()))
                      .addClass (CSS_CLASS_RIGHT);
            }

            // Show process + endpoints
            final HCLI aLI = aULP.addItem ();
            final HCDiv aDiv = div (NiceNameUI.getProcessID (aDocTypeID, aProcess.getProcessIdentifier (), false));
            aLI.addChild (aDiv);
            if (aEndpoints.isEmpty ())
            {
              aDiv.addChild (" ")
                  .addChild (new HCA (aWPEC.getSelfHref ()
                                           .addAll (createParamMap (aServiceInfo, aProcess, (ISMPEndpoint) null))
                                           .add (CPageParam.PARAM_ACTION, ACTION_DELETE_PROCESS)).setTitle ("Delete process")
                                                                                                 .addChild (EDefaultIcon.DELETE.getAsNode ()));
            }
            else
              aLI.addChild (aEPTable);
          }

          // Show document types + children
          final HCLI aLI = aULDT.addItem ();
          final HCDiv aDiv = div ().addChild (NiceNameUI.getDocumentTypeID (aServiceInfo.getDocumentTypeIdentifier (), false))
                                   .addChild (" ")
                                   .addChild (new HCA (LinkHelper.getURLWithServerAndContext (aParticipantID.getURIPercentEncoded () +
                                                                                              Rest2Filter.PATH_SERVICES +
                                                                                              aDocTypeID.getURIPercentEncoded ())).setTitle ("Perform SMP query on document type ")
                                                                                                                                  .setTargetBlank ()
                                                                                                                                  .addChild (EFamFamIcon.SCRIPT_GO.getAsNode ()));
          aLI.addChild (aDiv);
          if (aProcesses.isEmpty ())
          {
            aDiv.addChild (" ")
                .addChild (new HCA (aWPEC.getSelfHref ()
                                         .addAll (createParamMap (aServiceInfo, (ISMPProcess) null, (ISMPEndpoint) null))
                                         .add (CPageParam.PARAM_ACTION, ACTION_DELETE_DOCUMENT_TYPE)).setTitle ("Delete document type")
                                                                                                     .addChild (EDefaultIcon.DELETE.getAsNode ()));
          }
          else
            aLI.addChild (aULP);
        }
      }
      if (aServiceInfos == null || aServiceInfos.isEmpty () || aULDT.hasNoChildren ())
        aLISG.addChild (" ").addChild (badgeInfo ("This service group has no assigned endpoints!"));
      else
        aLISG.addChild (aULDT);
    }
    aNodeList.addChild (aULSG);
  }
}
