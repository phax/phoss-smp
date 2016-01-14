/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.compare.ESortOrder;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPage;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;

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

    final HCTable aTable = new HCTable (new DTCol ("Participant ID").setInitialSorting (ESortOrder.ASCENDING),
                                        new DTCol ("Extension?").setDataSort (1, 0)).setID (getID ());
    for (final ISMPServiceGroup aServiceGroup : SMPMetaManager.getServiceGroupMgr ().getAllSMPServiceGroups ())
    {
      aTable.addBodyRow ()
            .addCell (IdentifierHelper.getIdentifierURIEncoded (aServiceGroup.getParticpantIdentifier ()))
            .addCell (EPhotonCoreText.getYesOrNo (aServiceGroup.hasExtension (), aDisplayLocale));
    }

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);

    aNodeList.addChild (aTable).addChild (aDataTables);
  }
}
