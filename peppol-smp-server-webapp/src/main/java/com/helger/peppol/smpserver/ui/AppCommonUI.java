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
package com.helger.peppol.smpserver.ui;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.css.property.CCSSProperties;
import com.helger.datetime.PDTFactory;
import com.helger.datetime.format.PDTToString;
import com.helger.datetime.format.PeriodFormatMultilingual;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCEditPassword;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.tabular.HCCol;
import com.helger.html.hc.html.tabular.IHCTable;
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
import com.helger.peppol.identifier.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifierParts;
import com.helger.peppol.identifier.process.EPredefinedProcessIdentifier;
import com.helger.peppol.identifier.process.IPeppolProcessIdentifier;
import com.helger.peppol.smpserver.ui.ajax.AjaxExecutorPublicLogin;
import com.helger.peppol.smpserver.ui.ajax.CAjaxPublic;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.EBootstrapFormType;
import com.helger.photon.bootstrap3.label.BootstrapLabel;
import com.helger.photon.bootstrap3.label.EBootstrapLabelType;
import com.helger.photon.bootstrap3.table.BootstrapTable;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.bootstrap3.uictrls.datatables.IBootstrapDataTablesConfigurator;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.app.context.ILayoutExecutionContext;
import com.helger.photon.core.app.context.LayoutExecutionContext;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.login.CLogin;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.uictrls.datatables.DataTablesLengthMenu;
import com.helger.photon.uictrls.datatables.EDataTablesFilterType;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTables;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTablesI18N;
import com.helger.photon.uictrls.datatables.plugins.DataTablesPluginSearchHighlight;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

@Immutable
public final class AppCommonUI
{
  private static final DataTablesLengthMenu LENGTH_MENU = new DataTablesLengthMenu ().addItem (25).addItem (50).addItem (100).addItemAll ();

  private AppCommonUI ()
  {}

  public static void init ()
  {
    BootstrapDataTables.setConfigurator (new IBootstrapDataTablesConfigurator ()
    {
      public void configure (@Nonnull final ILayoutExecutionContext aLEC,
                             @Nonnull final IHCTable <?> aTable,
                             @Nonnull final BootstrapDataTables aDataTables)
      {
        final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();
        aDataTables.setAutoWidth (false)
                   .setLengthMenu (LENGTH_MENU)
                   .setAjaxBuilder (new JQueryAjaxBuilder ().url (CAjaxPublic.DATATABLES.getInvocationURL (aRequestScope))
                                                            .data (new JSAssocArray ().add (AjaxExecutorDataTables.OBJECT_ID, aTable.getID ())))
                   .setServerFilterType (EDataTablesFilterType.ALL_TERMS_PER_ROW)
                   .setTextLoadingURL (CAjaxPublic.DATATABLES_I18N.getInvocationURL (aRequestScope), AjaxExecutorDataTablesI18N.LANGUAGE_ID)
                   .addPlugin (new DataTablesPluginSearchHighlight ());
      }
    });
  }

  @Nonnull
  public static BootstrapForm createViewLoginForm (@Nonnull final LayoutExecutionContext aLEC,
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

    final BootstrapForm aForm = new BootstrapForm (aLEC.getSelfHref (), bFullUI ? EBootstrapFormType.HORIZONTAL : EBootstrapFormType.DEFAULT);
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
      aJSSuccess.body ()._if (aJSData.ref (AjaxExecutorPublicLogin.JSON_LOGGEDIN),
                              JSHtml.windowLocationReload (),
                              JQuery.idRef (sIDErrorField).empty ().append (aJSData.ref (AjaxExecutorPublicLogin.JSON_HTML)));
      aOnClick.add (new JQueryAjaxBuilder ().url (CAjaxPublic.LOGIN.getInvocationURI (aRequestScope))
                                            .data (new JSAssocArray ().add (CLogin.REQUEST_ATTR_USERID, JQuery.idRef (sIDUserName).val ())
                                                                      .add (CLogin.REQUEST_ATTR_PASSWORD, JQuery.idRef (sIDPassword).val ()))
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

    final BootstrapTable aCertDetails = new BootstrapTable (HCCol.star (), HCCol.star ());
    aCertDetails.addBodyRow ().addCell ("Version:").addCell (Integer.toString (aX509Cert.getVersion ()));
    aCertDetails.addBodyRow ().addCell ("Subject:").addCell (aX509Cert.getSubjectX500Principal ().getName ());
    aCertDetails.addBodyRow ().addCell ("Issuer:").addCell (aX509Cert.getIssuerX500Principal ().getName ());
    aCertDetails.addBodyRow ().addCell ("Serial number:").addCell (aX509Cert.getSerialNumber ().toString (16));
    aCertDetails.addBodyRow ()
                .addCell ("Valid from:")
                .addCell (new HCTextNode (PDTToString.getAsString (aNotBefore, aDisplayLocale) + " "),
                          aNowLDT.isBefore (aNotBefore) ? new BootstrapLabel (EBootstrapLabelType.DANGER).addChild ("!!!NOT YET VALID!!!") : null);
    aCertDetails.addBodyRow ()
                .addCell ("Valid to:")
                .addCell (new HCTextNode (PDTToString.getAsString (aNotAfter, aDisplayLocale) + " "),
                          aNowLDT.isAfter (aNotAfter) ? new BootstrapLabel (EBootstrapLabelType.DANGER).addChild ("!!!NO LONGER VALID!!!")
                                                      : new HCDiv ().addChild ("Valid for: " +
                                                                               PeriodFormatMultilingual.getFormatterLong (aDisplayLocale)
                                                                                                       .print (new Period (aNowLDT, aNotAfter))));

    if (aPublicKey instanceof RSAPublicKey)
    {
      // Special handling for RSA
      aCertDetails.addBodyRow ()
                  .addCell ("Public key:")
                  .addCell (aX509Cert.getPublicKey ().getAlgorithm () + " (" + ((RSAPublicKey) aPublicKey).getModulus ().bitLength () + " bits)");
    }
    else
    {
      // Usually EC or DSA key
      aCertDetails.addBodyRow ().addCell ("Public key:").addCell (aX509Cert.getPublicKey ().getAlgorithm ());
    }
    aCertDetails.addBodyRow ().addCell ("Signature algorithm:").addCell (aX509Cert.getSigAlgName () + " (" + aX509Cert.getSigAlgOID () + ")");
    return aCertDetails;
  }

  @Nonnull
  private static HCDiv _getWBRList (@Nonnull final String s)
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
    return ret;
  }

  @Nonnull
  public static IHCNode getDocumentTypeID (@Nonnull final IPeppolDocumentTypeIdentifier aDocTypeID)
  {
    EPredefinedDocumentTypeIdentifier ePredefined = null;
    for (final EPredefinedDocumentTypeIdentifier e : EPredefinedDocumentTypeIdentifier.values ())
      if (e.getAsDocumentTypeIdentifier ().equals (aDocTypeID))
      {
        ePredefined = e;
        break;
      }

    if (ePredefined != null)
      return new HCTextNode (ePredefined.getCommonName () + " [predefined]");

    return _getWBRList (aDocTypeID.getURIEncoded ());
  }

  @Nonnull
  public static IHCNode getProcessID (@Nonnull final IPeppolProcessIdentifier aDocTypeID)
  {
    EPredefinedProcessIdentifier ePredefined = null;
    for (final EPredefinedProcessIdentifier e : EPredefinedProcessIdentifier.values ())
      if (e.getAsProcessIdentifier ().equals (aDocTypeID))
      {
        ePredefined = e;
        break;
      }

    if (ePredefined != null)
      return new HCTextNode (ePredefined.getValue () + " [predefined]");

    return _getWBRList (aDocTypeID.getURIEncoded ());
  }

  @Nonnull
  public static HCUL getDocumentTypeIDDetails (@Nonnull final IPeppolDocumentTypeIdentifierParts aParts)
  {
    final HCUL aUL = new HCUL ();
    aUL.addItem ().addChild ("Root namespace: ").addChild (new HCCode ().addChild (aParts.getRootNS ()));
    aUL.addItem ().addChild ("Local name: ").addChild (new HCCode ().addChild (aParts.getLocalName ()));
    aUL.addItem ().addChild ("Transaction ID: ").addChild (new HCCode ().addChild (aParts.getTransactionID ()));
    final HCNodeList aExtensions = new HCNodeList ();
    final List <String> aExtensionIDs = aParts.getExtensionIDs ();
    for (final String sExtension : aExtensionIDs)
    {
      if (aExtensions.hasChildren ())
        aExtensions.addChild (", ");
      aExtensions.addChild (new HCCode ().addChild (sExtension));
    }
    aUL.addItem ().addChild ("Extension IDs (" + aExtensionIDs.size () + "): ").addChild (aExtensions);
    aUL.addItem ().addChild ("Customization ID (transaction + extensions): ").addChild (new HCCode ().addChild (aParts.getAsUBLCustomizationID ()));
    aUL.addItem ().addChild ("Version: ").addChild (new HCCode ().addChild (aParts.getVersion ()));
    return aUL;
  }

  @Nonnull
  public static String getOwnerName (@Nonnull @Nonempty final String sOwnerID)
  {
    final IUser aOwner = PhotonSecurityManager.getUserMgr ().getUserOfID (sOwnerID);
    return aOwner == null ? sOwnerID : aOwner.getLoginName () + " (" + aOwner.getDisplayName () + ")";
  }
}
