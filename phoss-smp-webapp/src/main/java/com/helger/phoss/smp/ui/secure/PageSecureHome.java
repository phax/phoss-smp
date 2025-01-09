/*
 * Copyright (C) 2014-2024 Philip Helger and contributors
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
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.photon.bootstrap4.grid.BootstrapGridSpec;
import com.helger.photon.bootstrap4.grid.BootstrapRow;
import com.helger.photon.bootstrap4.layout.BootstrapContainer;
import com.helger.photon.core.menu.IMenuItemPage;
import com.helger.photon.core.menu.IMenuObject;
import com.helger.photon.core.menu.IMenuTree;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uicore.page.system.BasePageShowChildren.ShowChildrenCallback;
import com.helger.photon.uicore.page.system.BasePageShowChildrenRenderer;
import com.helger.tree.util.TreeVisitor;
import com.helger.tree.withid.DefaultTreeItemWithID;

public class PageSecureHome extends AbstractSMPWebPage
{
  private final IMenuTree m_aMenuTree;

  public PageSecureHome (@Nonnull @Nonempty final String sID, @Nonnull final IMenuTree aMenuTree)
  {
    super (sID, "Landing Page");
    m_aMenuTree = aMenuTree;
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    final Function <String, IHCNode> fAndChildren = sPageID -> {
      final HCNodeList ret = new HCNodeList ();
      final DefaultTreeItemWithID <String, IMenuObject> aMenuTreeItem = m_aMenuTree.getItemWithID (sPageID);

      ret.addChild (h2 (((IMenuItemPage) aMenuTreeItem.getData ()).getDisplayText (aDisplayLocale)));

      final BasePageShowChildrenRenderer aRenderer = new BasePageShowChildrenRenderer ();
      final HCUL aUL = ret.addAndReturnChild (new HCUL ());
      aUL.addItem (a (aWPEC.getLinkToMenuItem (aMenuTreeItem.getID ())).addChild ("Show List"));
      TreeVisitor.visitTreeItem (aMenuTreeItem, new ShowChildrenCallback <> (aWPEC, aUL, aRenderer));
      return ret;
    };

    final Function <String, IHCNode> fLinkToPage = sPageID -> {
      final HCNodeList ret = new HCNodeList ();
      final DefaultTreeItemWithID <String, IMenuObject> aMenuTreeItem = m_aMenuTree.getItemWithID (sPageID);

      ret.addChild (h2 (((IMenuItemPage) aMenuTreeItem.getData ()).getDisplayText (aDisplayLocale)));
      ret.addChild (a (aWPEC.getLinkToMenuItem (aMenuTreeItem.getID ())).addChild ("Show List"));

      return ret;
    };

    final BootstrapContainer aContainer = aNodeList.addAndReturnChild (new BootstrapContainer (true));
    final BootstrapRow aRow = aContainer.addAndReturnChild (new BootstrapRow ());
    aRow.createColumn (BootstrapGridSpec.create (-1, -1, -1, 3, -1))
        .addChild (fAndChildren.apply (CMenuSecure.MENU_SERVICE_GROUPS));
    aRow.createColumn (BootstrapGridSpec.create (-1, -1, -1, 3, -1))
        .addChild (fAndChildren.apply (CMenuSecure.MENU_ENDPOINTS));
    aRow.createColumn (BootstrapGridSpec.create (-1, -1, -1, 3, -1))
        .addChild (fLinkToPage.apply (CMenuSecure.MENU_REDIRECTS));
    aRow.createColumn (BootstrapGridSpec.create (-1, -1, -1, 3, -1))
        .addChild (fLinkToPage.apply (CMenuSecure.MENU_BUSINESS_CARDS));
  }
}
