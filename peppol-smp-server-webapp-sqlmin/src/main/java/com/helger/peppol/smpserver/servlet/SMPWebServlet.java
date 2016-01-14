/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.helger.commons.url.SimpleURL;
import com.helger.css.property.CCSSProperties;
import com.helger.html.hc.ext.HCA_MailTo;
import com.helger.html.hc.html.grouping.HCP;
import com.helger.html.hc.html.metadata.HCHead;
import com.helger.html.hc.html.metadata.HCStyle;
import com.helger.html.hc.html.root.HCHtml;
import com.helger.html.hc.html.sections.HCBody;
import com.helger.html.hc.html.sections.HCH1;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.render.HCRenderer;
import com.helger.peppol.smpserver.CSMPServer;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.app.CApp;
import com.helger.peppol.smpserver.security.SMPKeyManager;
import com.helger.peppol.smpserver.smlhook.RegistrationHookFactory;

/**
 * This servlet is responsible for rendering the static web site content.
 *
 * @author Philip Helger
 */
public final class SMPWebServlet extends HttpServlet
{
  @Override
  protected void doGet (final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
                                                                                      IOException
  {
    final HCHtml aHtml = new HCHtml ();
    final HCHead aHead = aHtml.getHead ();
    aHead.setPageTitle (CApp.getApplicationTitle ());
    aHead.addCSS (new HCStyle ("*{font-family:sans-serif;}" + "a,a:link,a:visited,a:hover,a:active{color:#109010}"));

    final HCBody aBody = aHtml.getBody ();
    aBody.addChild (new HCH1 ().addChild (CApp.getApplicationTitle ()));

    aBody.addChild (new HCP ().addChild ("SMP-ID: " + SMPServerConfiguration.getSMLSMPID ()));

    aBody.addChild (new HCP ().addChild ("Version: " + CSMPServer.getVersionNumber ()));

    if (!SMPKeyManager.isCertificateValid ())
      aBody.addChild (new HCP ().addChild ("Certificate configuration is invalid. REST queries will not work!"));

    aBody.addChild (new HCP ().addChild ("SML connection: " +
                                         (RegistrationHookFactory.isSMLConnectionActive () ? "active!"
                                                                                           : "NOT active!")));

    aBody.addChild (new HCP ().addStyle (CCSSProperties.BORDER_TOP.newValue ("solid 1px black"))
                              .addStyle (CCSSProperties.PADDING_TOP.newValue ("1em"))
                              .addChild ("Created by ")
                              .addChild (HCA_MailTo.createLinkedEmail ("philip@helger.com", "Philip Helger"))
                              .addChild (" - Twitter: ")
                              .addChild (new HCA (new SimpleURL ("https://twitter.com/philiphelger")).setTargetBlank ()
                                                                                                     .addChild ("@philiphelger"))
                              .addChild (" - ")
                              .addChild (new HCA (new SimpleURL ("https://github.com/phax/peppol-smp-server")).setTargetBlank ()
                                                                                                              .addChild ("Source on GitHub")));

    final String sHTML = HCRenderer.getAsHTMLString (aHtml);
    resp.getWriter ().write (sHTML);
    resp.getWriter ().flush ();
  }
}
