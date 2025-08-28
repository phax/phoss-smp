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

import java.util.Map;

import com.helger.annotation.Nonempty;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.phoss.smp.nicename.NiceNameEntry;
import com.helger.phoss.smp.nicename.NiceNameHandler;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.photon.bootstrap4.CBootstrapCSS;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.nav.BootstrapTabBox;
import com.helger.photon.bootstrap4.table.BootstrapTable;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.column.DTCol;

import jakarta.annotation.Nonnull;

/**
 * A read-only page that shows all the applied identifier mappings.
 *
 * @author Philip Helger
 * @since 5.3.2
 */
public final class PageSecureSMPIdentifierMappings extends AbstractSMPWebPage
{
  private static final String ACTION_RELOAD = "reload";

  public PageSecureSMPIdentifierMappings (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Identifier Mappings");
  }

  @Nonnull
  private IHCNode _createList (@Nonnull final WebPageExecutionContext aWPEC,
                               @Nonnull final ICommonsOrderedMap <String, NiceNameEntry> aEntries,
                               @Nonnull final String sSuffix)
  {
    final BootstrapTable aTable = new BootstrapTable (new DTCol ("ID").setWidthPerc (60),
                                                      new DTCol ("Name"),
                                                      new DTCol ("State").setWidth (100)).setID (getID () + sSuffix);
    for (final Map.Entry <String, NiceNameEntry> aEntry : aEntries.entrySet ())
    {
      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (span (aEntry.getKey ()).addClass (CBootstrapCSS.TEXT_BREAK));
      aRow.addCell (aEntry.getValue ().getName ());

      final String sState;
      switch (aEntry.getValue ().getState ())
      {
        case ACTIVE:
          sState = "Active";
          break;
        case DEPRECATED:
          sState = "Deprecated";
          break;
        case REMOVED:
          sState = "Removed";
          break;
        default:
          sState = "Unknown";
      }
      aRow.addCell (sState);
    }
    return new HCNodeList ().addChild (aTable).addChild (BootstrapDataTables.createDefaultDataTables (aWPEC, aTable));
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ICommonsOrderedMap <String, NiceNameEntry> aDocTypeEntries = NiceNameHandler.getAllDocumentTypeMappings ();
    final ICommonsOrderedMap <String, NiceNameEntry> aProcEntries = NiceNameHandler.getAllProcessMappings ();

    if (aDocTypeEntries.isNotEmpty () || aProcEntries.isNotEmpty ())
      aNodeList.addChild (info ("The lists below are only for display purposes. The lists are not in any way limiting the registration possibilities in the Endpoints."));

    final BootstrapButtonToolbar aToolbar = aNodeList.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
    aToolbar.addChild (new BootstrapButton ().addChild ("Reload")
                                             .setIcon (EDefaultIcon.REFRESH)
                                             .setOnClick (aWPEC.getSelfHref ()
                                                               .add (CPageParam.PARAM_ACTION, ACTION_RELOAD)));

    if (aWPEC.hasAction (ACTION_RELOAD))
    {
      NiceNameHandler.reloadNames ();
      aNodeList.addChild (success ("Successfully reloaded name mappings"));
    }

    final BootstrapTabBox aTabBox = aNodeList.addAndReturnChild (new BootstrapTabBox ());
    aTabBox.addTab ("doctypes", "Document Types", _createList (aWPEC, aDocTypeEntries, "doctypes"));
    aTabBox.addTab ("procs", "Processes", _createList (aWPEC, aProcEntries, "procs"));
  }
}
