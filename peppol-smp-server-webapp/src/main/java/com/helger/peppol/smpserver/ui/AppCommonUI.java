/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.ui;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.w3c.dom.Element;

import com.helger.commons.CGlobal;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTToString;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.css.property.CCSSProperties;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCEditPassword;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCHR;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.tabular.HCCol;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.html.textlevel.HCWBR;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.html.jquery.JQuery;
import com.helger.html.jquery.JQueryAjaxBuilder;
import com.helger.html.jscode.JSAnonymousFunction;
import com.helger.html.jscode.JSAssocArray;
import com.helger.html.jscode.JSPackage;
import com.helger.html.jscode.JSVar;
import com.helger.html.jscode.html.JSHtml;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.peppol.identifier.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppol.identifier.peppol.doctype.IPeppolDocumentTypeIdentifierParts;
import com.helger.peppol.identifier.peppol.process.EPredefinedProcessIdentifier;
import com.helger.peppol.smpserver.domain.extension.ISMPHasExtension;
import com.helger.peppol.smpserver.ui.ajax.AjaxExecutorPublicLogin;
import com.helger.peppol.smpserver.ui.ajax.CAjax;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.ext.BootstrapSystemMessage;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.EBootstrapFormType;
import com.helger.photon.bootstrap3.label.BootstrapLabel;
import com.helger.photon.bootstrap3.label.EBootstrapLabelType;
import com.helger.photon.bootstrap3.table.BootstrapTable;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.app.context.ILayoutExecutionContext;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.login.CLogin;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.uictrls.datatables.DataTablesLengthMenu;
import com.helger.photon.uictrls.datatables.EDataTablesFilterType;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTables;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTablesI18N;
import com.helger.photon.uictrls.datatables.plugins.DataTablesPluginSearchHighlight;
import com.helger.photon.uictrls.prism.EPrismLanguage;
import com.helger.photon.uictrls.prism.HCPrismJS;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.write.XMLWriter;

/**
 * Common UI helper methods
 *
 * @author Philip Helger
 */
@Immutable
public final class AppCommonUI
{
  private static final DataTablesLengthMenu LENGTH_MENU = new DataTablesLengthMenu ().addItem (25)
                                                                                     .addItem (50)
                                                                                     .addItem (100)
                                                                                     .addItemAll ();

  private AppCommonUI ()
  {}

  public static void init ()
  {
    BootstrapDataTables.setConfigurator ( (aLEC, aTable, aDataTables) -> {
      final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();
      aDataTables.setAutoWidth (false)
                 .setLengthMenu (LENGTH_MENU)
                 .setAjaxBuilder (new JQueryAjaxBuilder ().url (CAjax.DATATABLES.getInvocationURL (aRequestScope))
                                                          .data (new JSAssocArray ().add (AjaxExecutorDataTables.OBJECT_ID,
                                                                                          aTable.getID ())))
                 .setServerFilterType (EDataTablesFilterType.ALL_TERMS_PER_ROW)
                 .setTextLoadingURL (CAjax.DATATABLES_I18N.getInvocationURL (aRequestScope),
                                     AjaxExecutorDataTablesI18N.LANGUAGE_ID)
                 .addPlugin (new DataTablesPluginSearchHighlight ());
    });

    // By default allow markdown in system message
    BootstrapSystemMessage.setDefaultUseMarkdown (true);
  }

  private static String _getPeriod (@Nonnull final LocalDateTime aNowLDT, @Nonnull final LocalDateTime aNotAfter)
  {
    final Period aPeriod = Period.between (aNowLDT.toLocalDate (), aNotAfter.toLocalDate ());
    final Duration aDuration = Duration.between (aNowLDT.toLocalTime (),
                                                 aNotAfter.plus (1, ChronoUnit.DAYS).toLocalTime ());
    long nSecs = aDuration.getSeconds ();
    final long nHours = nSecs / CGlobal.SECONDS_PER_HOUR;
    nSecs -= nHours * CGlobal.SECONDS_PER_HOUR;
    final long nMinutes = nSecs / CGlobal.SECONDS_PER_MINUTE;
    nSecs -= nMinutes * CGlobal.SECONDS_PER_MINUTE;
    String ret = "";
    if (aPeriod.getYears () > 0)
      ret += aPeriod.getYears () + " years, ";
    return ret +
           aPeriod.getMonths () +
           " months, " +
           aPeriod.getDays () +
           " days, " +
           nHours +
           " hours, " +
           nMinutes +
           " minutes and " +
           nSecs +
           " seconds";
  }

  @Nonnull
  public static BootstrapForm createViewLoginForm (@Nonnull final ILayoutExecutionContext aLEC,
                                                   @Nullable final String sPreselectedUserName,
                                                   final boolean bFullUI)
  {
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();

    // Use new IDs for both fields, in case the login stuff is displayed more
    // than once!
    final String sIDUserName = GlobalIDFactory.getNewStringID ();
    final String sIDPassword = GlobalIDFactory.getNewStringID ();
    final String sIDErrorField = GlobalIDFactory.getNewStringID ();

    final BootstrapForm aForm = new BootstrapForm (aLEC).setAction (aLEC.getSelfHref ())
                                                        .setFormType (bFullUI ? EBootstrapFormType.HORIZONTAL
                                                                              : EBootstrapFormType.DEFAULT);
    aForm.setLeft (3);

    // User name field
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel (EPhotonCoreText.EMAIL_ADDRESS.getDisplayText (aDisplayLocale))
                                                 .setCtrl (new HCEdit (new RequestField (CLogin.REQUEST_ATTR_USERID,
                                                                                         sPreselectedUserName)).setID (sIDUserName)));

    // Password field
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel (EPhotonCoreText.LOGIN_FIELD_PASSWORD.getDisplayText (aDisplayLocale))
                                                 .setCtrl (new HCEditPassword (CLogin.REQUEST_ATTR_PASSWORD).setID (sIDPassword)));

    // Placeholder for error message
    aForm.addChild (new HCDiv ().setID (sIDErrorField).addStyle (CCSSProperties.MARGIN.newValue ("4px 0")));

    // Login button
    final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (new BootstrapButtonToolbar (aLEC));
    {
      final JSPackage aOnClick = new JSPackage ();
      final JSAnonymousFunction aJSSuccess = new JSAnonymousFunction ();
      final JSVar aJSData = aJSSuccess.param ("data");
      aJSSuccess.body ()
                ._if (aJSData.ref (AjaxExecutorPublicLogin.JSON_LOGGEDIN),
                      JSHtml.windowLocationReload (),
                      JQuery.idRef (sIDErrorField).empty ().append (aJSData.ref (AjaxExecutorPublicLogin.JSON_HTML)));
      aOnClick.add (new JQueryAjaxBuilder ().url (CAjax.LOGIN.getInvocationURI (aRequestScope))
                                            .data (new JSAssocArray ().add (CLogin.REQUEST_ATTR_USERID,
                                                                            JQuery.idRef (sIDUserName).val ())
                                                                      .add (CLogin.REQUEST_ATTR_PASSWORD,
                                                                            JQuery.idRef (sIDPassword).val ()))
                                            .success (aJSSuccess)
                                            .build ());
      aOnClick._return (false);
      aToolbar.addSubmitButton (EPhotonCoreText.LOGIN_BUTTON_SUBMIT.getDisplayText (aDisplayLocale), aOnClick);
    }
    return aForm;
  }

  @Nonnull
  public static BootstrapTable createCertificateDetailsTable (@Nonnull final X509Certificate aX509Cert,
                                                              @Nonnull final LocalDateTime aNowLDT,
                                                              @Nonnull final Locale aDisplayLocale)
  {
    final LocalDateTime aNotBefore = PDTFactory.createLocalDateTime (aX509Cert.getNotBefore ());
    final LocalDateTime aNotAfter = PDTFactory.createLocalDateTime (aX509Cert.getNotAfter ());
    final PublicKey aPublicKey = aX509Cert.getPublicKey ();

    final BootstrapTable aCertDetails = new BootstrapTable (HCCol.perc (20), HCCol.star ());
    aCertDetails.addBodyRow ().addCell ("Version:").addCell (Integer.toString (aX509Cert.getVersion ()));
    aCertDetails.addBodyRow ().addCell ("Subject:").addCell (aX509Cert.getSubjectX500Principal ().getName ());
    aCertDetails.addBodyRow ().addCell ("Issuer:").addCell (aX509Cert.getIssuerX500Principal ().getName ());
    aCertDetails.addBodyRow ().addCell ("Serial number:").addCell (aX509Cert.getSerialNumber ().toString (16));
    aCertDetails.addBodyRow ()
                .addCell ("Valid from:")
                .addCell (new HCTextNode (PDTToString.getAsString (aNotBefore, aDisplayLocale) + " "),
                          aNowLDT.isBefore (aNotBefore) ? new BootstrapLabel (EBootstrapLabelType.DANGER).addChild ("!!!NOT YET VALID!!!")
                                                        : null);
    aCertDetails.addBodyRow ()
                .addCell ("Valid to:")
                .addCell (new HCTextNode (PDTToString.getAsString (aNotAfter, aDisplayLocale) + " "),
                          aNowLDT.isAfter (aNotAfter) ? new BootstrapLabel (EBootstrapLabelType.DANGER).addChild ("!!!NO LONGER VALID!!!")
                                                      : new HCDiv ().addChild ("Valid for: " +
                                                                               _getPeriod (aNowLDT, aNotAfter)));

    if (aPublicKey instanceof RSAPublicKey)
    {
      // Special handling for RSA
      aCertDetails.addBodyRow ()
                  .addCell ("Public key:")
                  .addCell (aX509Cert.getPublicKey ().getAlgorithm () +
                            " (" +
                            ((RSAPublicKey) aPublicKey).getModulus ().bitLength () +
                            " bits)");
    }
    else
    {
      // Usually EC or DSA key
      aCertDetails.addBodyRow ().addCell ("Public key:").addCell (aX509Cert.getPublicKey ().getAlgorithm ());
    }
    aCertDetails.addBodyRow ()
                .addCell ("Signature algorithm:")
                .addCell (aX509Cert.getSigAlgName () + " (" + aX509Cert.getSigAlgOID () + ")");
    return aCertDetails;
  }

  @Nonnull
  private static IHCNode _getWBRList (@Nonnull final String s)
  {
    final HCDiv ret = new HCDiv ();
    String sRest = s;
    final int nChars = 30;
    while (sRest.length () > nChars)
    {
      ret.addChild (sRest.substring (0, nChars)).addChild (new HCWBR ());
      sRest = sRest.substring (nChars);
    }
    if (sRest.length () > 0)
      ret.addChild (sRest);
    return ret.getChildCount () == 1 ? ret.getFirstChild () : ret;
  }

  @Nonnull
  public static IHCNode getDocumentTypeID (@Nonnull final IDocumentTypeIdentifier aDocTypeID)
  {
    EPredefinedDocumentTypeIdentifier ePredefined = null;
    for (final EPredefinedDocumentTypeIdentifier e : EPredefinedDocumentTypeIdentifier.values ())
      if (e.getAsDocumentTypeIdentifier ().hasSameContent (aDocTypeID))
      {
        ePredefined = e;
        break;
      }

    if (ePredefined != null)
      return new HCNodeList ().addChild (ePredefined.getCommonName () + " ")
                              .addChild (new BootstrapLabel (EBootstrapLabelType.INFO).addChild ("predefined"));

    return _getWBRList (aDocTypeID.getURIEncoded ());
  }

  @Nonnull
  public static IHCNode getProcessID (@Nonnull final IProcessIdentifier aDocTypeID)
  {
    EPredefinedProcessIdentifier ePredefined = null;
    for (final EPredefinedProcessIdentifier e : EPredefinedProcessIdentifier.values ())
      if (e.getAsProcessIdentifier ().hasSameContent (aDocTypeID))
      {
        ePredefined = e;
        break;
      }

    if (ePredefined != null)
      return new HCNodeList ().addChild (ePredefined.getValue () + " ")
                              .addChild (new BootstrapLabel (EBootstrapLabelType.INFO).addChild ("predefined"));

    return _getWBRList (aDocTypeID.getURIEncoded ());
  }

  @Nonnull
  public static HCUL getDocumentTypeIDDetails (@Nonnull final IPeppolDocumentTypeIdentifierParts aParts)
  {
    final HCUL aUL = new HCUL ();
    aUL.addItem ().addChild ("Root namespace: ").addChild (new HCCode ().addChild (aParts.getRootNS ()));
    aUL.addItem ().addChild ("Local name: ").addChild (new HCCode ().addChild (aParts.getLocalName ()));
    aUL.addItem ().addChild ("Customization ID: ").addChild (new HCCode ().addChild (aParts.getCustomizationID ()));
    aUL.addItem ().addChild ("Version: ").addChild (new HCCode ().addChild (aParts.getVersion ()));
    return aUL;
  }

  @Nonnull
  public static String getOwnerName (@Nonnull @Nonempty final String sOwnerID)
  {
    final IUser aOwner = PhotonSecurityManager.getUserMgr ().getUserOfID (sOwnerID);
    return aOwner == null ? sOwnerID : aOwner.getLoginName () + " (" + aOwner.getDisplayName () + ")";
  }

  @Nullable
  public static IHCNode getExtensionDisplay (@Nonnull final ISMPHasExtension aHasExtension)
  {
    final ICommonsList <com.helger.peppol.bdxr.ExtensionType> aExtensions = aHasExtension.getAllExtensions ();
    if (aExtensions.isEmpty ())
      return null;

    final HCNodeList aNL = new HCNodeList ();
    for (final com.helger.peppol.bdxr.ExtensionType aExtension : aExtensions)
    {
      if (aNL.hasChildren ())
      {
        // add a separator line
        aNL.addChild (new HCHR ());
      }

      // Use only the XML element of the first extension
      final Element aAny = (Element) aExtension.getAny ();
      final String sXML = XMLWriter.getNodeAsString (aAny);
      aNL.addChild (new HCPrismJS (EPrismLanguage.MARKUP).addChild (sXML));
    }
    return aNL;
  }
}
