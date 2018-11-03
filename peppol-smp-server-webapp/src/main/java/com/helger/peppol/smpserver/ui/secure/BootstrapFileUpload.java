/**
 * Copyright (C) 2014-2018 Philip Helger and contributors
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
package com.helger.peppol.smpserver.ui.secure;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.html.hc.IHCConversionSettingsToNode;
import com.helger.html.hc.IHCHasChildrenMutable;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.forms.HCEditFile;
import com.helger.html.hc.html.forms.HCLabel;
import com.helger.html.hc.html.grouping.AbstractHCDiv;
import com.helger.html.jquery.JQuery;
import com.helger.html.js.EJSEvent;
import com.helger.html.jscode.JSExpr;
import com.helger.html.jscode.html.JSHtml;
import com.helger.photon.bootstrap4.CBootstrapCSS;

public class BootstrapFileUpload extends AbstractHCDiv <BootstrapFileUpload>
{
  private final String m_sName;

  public BootstrapFileUpload (@Nullable final String sName)
  {
    m_sName = sName;
  }

  @Override
  protected void onFinalizeNodeState (@Nonnull final IHCConversionSettingsToNode aConversionSettings,
                                      @Nonnull final IHCHasChildrenMutable <?, ? super IHCNode> aTargetNode)
  {
    super.onFinalizeNodeState (aConversionSettings, aTargetNode);
    addClass (CBootstrapCSS.CUSTOM_FILE);

    final HCEditFile aEditFile = new HCEditFile (m_sName);
    aEditFile.addClass (CBootstrapCSS.CUSTOM_FILE_INPUT);
    addChild (aEditFile);

    final HCLabel aLabel = new HCLabel ();
    aLabel.setFor (aEditFile);
    aLabel.addClass (CBootstrapCSS.CUSTOM_FILE_LABEL);
    aLabel.addChild ("Choose file");
    addChild (aLabel);

    aEditFile.addEventHandler (EJSEvent.CHANGE,
                               false ? JSHtml.consoleLog (JSExpr.THIS.ref ("files").component0 ().ref ("name"))
                                     : JQuery.idRef (aLabel)
                                             .empty ()
                                             .append (JSExpr.THIS.ref ("files").component0 ().ref ("name")));
  }
}
