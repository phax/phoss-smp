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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.html.hc.html.grouping.HCP;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCStrong;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.photon.app.url.LinkHelper;
import com.helger.photon.core.servlet.AbstractSecureApplicationServlet;
import com.helger.photon.uicore.page.WebPageExecutionContext;

import jakarta.annotation.Nullable;

/**
 * This is the start page of the public application. It contains a static description of this SMP
 * only - the participants contained in it are deliberately not listed, so that the page can be
 * rendered without touching the backend at all.
 *
 * @author Philip Helger
 */
public final class PagePublicStart extends AbstractSMPWebPage
{
  public PagePublicStart (@NonNull @Nonempty final String sID)
  {
    super (sID, "Start page");
  }

  @Override
  @Nullable
  public String getHeaderText (@NonNull final WebPageExecutionContext aWPEC)
  {
    return "Welcome to this Service Metadata Publisher";
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    aNodeList.addChild (new HCP ().addChild ("This server is a ")
                                  .addChild (new HCStrong ().addChild ("Service Metadata Publisher (SMP)"))
                                  .addChild (". For each of the participants registered on it, it states which " +
                                             "document types that participant is able to receive, and at which " +
                                             "Access Point these documents are to be delivered."));

    aNodeList.addChild (new HCP ().addChild ("That information is served via the ")
                                  .addChild (new HCStrong ().addChild (SMPServerConfiguration.getRESTType ()
                                                                                             .getDisplayName () +
                                                                       " SMP REST API"))
                                  .addChild (" - the interface a sending Access Point queries before it delivers a " +
                                             "document. A query needs nothing but the identifier of the participant " +
                                             "in question and returns a machine readable XML document."));

    aNodeList.addChild (new HCP ().addChild ("The participants managed by this SMP are not listed here. Each of them " +
                                             "is looked up individually, so this page is rendered without querying " +
                                             "the underlying storage at all - no matter how many participants this " +
                                             "SMP manages."));

    aNodeList.addChild (new HCP ().addChild ("Administrators of this SMP manage the participants in the ")
                                  .addChild (new HCA (LinkHelper.getURLWithContext (aWPEC.getRequestScope (),
                                                                                    AbstractSecureApplicationServlet.SERVLET_DEFAULT_PATH +
                                                                                                                     "/")).addChild ("administration area"))
                                  .addChild ("."));
  }
}
