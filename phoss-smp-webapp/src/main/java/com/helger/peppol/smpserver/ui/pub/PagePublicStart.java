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
package com.helger.peppol.smpserver.ui.pub;

import java.util.Comparator;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.compare.ESortOrder;
import com.helger.html.hc.ext.HCExtHelper;
import com.helger.html.hc.html.tabular.AbstractHCTable;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.smpserver.app.SMPWebAppConfiguration;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPage;
import com.helger.photon.bootstrap4.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap4.table.BootstrapTable;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.url.LinkHelper;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.famfam.EFamFamIcon;

/**
 * This is the start page of the public application. It lists all available
 * service groups.
 *
 * @author Philip Helger
 */
public final class PagePublicStart extends AbstractSMPWebPage
{
  public PagePublicStart (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Start page");
  }

  @Override
  @Nullable
  public String getHeaderText (@Nonnull final WebPageExecutionContext aWPEC)
  {
    return "Managed participants on this SMP";
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    if (SMPWebAppConfiguration.isStartPageParticipantsNone ())
    {
      // New in v5.0.4
      aNodeList.addChild (new BootstrapInfoBox ().addChild ("This SMP has disabled the list of participants."));
    }
    else
    {
      final ISMPServiceGroupManager aSMPServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ICommonsList <ISMPServiceGroup> aServiceGroups = aSMPServiceGroupMgr.getAllSMPServiceGroups ();

      // Use dynamic or static table?
      final boolean bUseDataTables = SMPWebAppConfiguration.isStartPageDynamicTable ();
      final boolean bShowExtensionDetails = SMPWebAppConfiguration.isStartPageExtensionsShow ();

      AbstractHCTable <?> aFinalTable;
      if (bUseDataTables)
      {
        // Dynamic
        final HCTable aTable = new HCTable (new DTCol ("Participant ID").setInitialSorting (ESortOrder.ASCENDING),
                                            new DTCol (bShowExtensionDetails ? "Extension"
                                                                             : "Extension?").setDataSort (1, 0),
                                            new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
        aFinalTable = aTable;
      }
      else
      {
        // Static
        final BootstrapTable aTable = new BootstrapTable ();
        aTable.setBordered (true);
        aTable.setCondensed (true);
        aTable.setStriped (true);
        aTable.addHeaderRow ()
              .addCell ("Participant ID")
              .addCell (bShowExtensionDetails ? "Extension" : "Extension?")
              .addCell (EPhotonCoreText.ACTIONS.getDisplayText (aDisplayLocale));
        aFinalTable = aTable;

        // Sort manually
        aServiceGroups.sort (Comparator.comparing (x -> x.getParticpantIdentifier ().getURIEncoded ()));
      }

      for (final ISMPServiceGroup aServiceGroup : aServiceGroups)
      {
        final String sDisplayName = aServiceGroup.getParticpantIdentifier ().getURIEncoded ();

        final HCRow aRow = aFinalTable.addBodyRow ();
        aRow.addCell (sDisplayName);
        if (bShowExtensionDetails)
        {
          if (aServiceGroup.hasExtension ())
            aRow.addCell (new HCCode ().addChildren (HCExtHelper.nl2divList (aServiceGroup.getFirstExtensionXML ())));
          else
            aRow.addCell ();
        }
        else
        {
          aRow.addCell (EPhotonCoreText.getYesOrNo (aServiceGroup.hasExtension (), aDisplayLocale));
        }
        aRow.addCell (new HCA (LinkHelper.getURLWithServerAndContext (aServiceGroup.getParticpantIdentifier ()
                                                                                   .getURIPercentEncoded ())).setTitle ("Perform SMP query on " +
                                                                                                                        sDisplayName)
                                                                                                             .setTargetBlank ()
                                                                                                             .addChild (EFamFamIcon.SCRIPT_GO.getAsNode ()));
      }
      if (aFinalTable.hasBodyRows ())
      {
        aNodeList.addChild (aFinalTable);

        if (bUseDataTables)
        {
          final BootstrapDataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aFinalTable);
          aNodeList.addChild (aDataTables);
        }
      }
      else
        aNodeList.addChild (new BootstrapInfoBox ().addChild ("This SMP does not manage any participant yet."));
    }
  }
}
