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
package com.helger.phoss.smp.ui;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.time.OffsetDateTime;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.numeric.MathHelper;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.css.property.CCSSProperties;
import com.helger.datetime.format.PDTToString;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.util.PDTDisplayHelper;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCHR;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.tabular.HCCol;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.jquery.JQueryAjaxBuilder;
import com.helger.html.jscode.JSAssocArray;
import com.helger.peppolid.peppol.doctype.IPeppolDocumentTypeIdentifierParts;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.domain.extension.ISMPHasExtension;
import com.helger.phoss.smp.ui.ajax.CAjax;
import com.helger.photon.bootstrap4.badge.BootstrapBadge;
import com.helger.photon.bootstrap4.badge.EBootstrapBadgeType;
import com.helger.photon.bootstrap4.ext.BootstrapSystemMessage;
import com.helger.photon.bootstrap4.table.BootstrapTable;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapTechnicalUI;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.uictrls.datatables.DataTablesLengthMenu;
import com.helger.photon.uictrls.datatables.EDataTablesFilterType;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTables;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTablesI18N;
import com.helger.photon.uictrls.datatables.plugins.DataTablesPluginSearchHighlight;
import com.helger.photon.uictrls.prism.EPrismLanguage;
import com.helger.photon.uictrls.prism.HCPrismJS;
import com.helger.smpclient.extension.SMPExtension;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Common UI helper methods
 *
 * @author Philip Helger
 */
@Immutable
public final class SMPCommonUI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPCommonUI.class);
  private static final DataTablesLengthMenu LENGTH_MENU = new DataTablesLengthMenu ().addItem (25)
                                                                                     .addItem (50)
                                                                                     .addItem (100)
                                                                                     .addItemAll ();

  private SMPCommonUI ()
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
                                     AjaxExecutorDataTablesI18N.REQUEST_PARAM_LANGUAGE_ID)
                 .addPlugin (new DataTablesPluginSearchHighlight ());
    });

    // By default allow markdown in system message
    BootstrapSystemMessage.setDefaultUseMarkdown (SMPWebAppConfiguration.isWebAppSystemMessageUseMarkdown ());
  }

  // Based on PeriodFuncTest code
  @Nonnull
  @Nonempty
  private static String _getPeriodString (final int nYears,
                                          final int nMonths,
                                          final int nDays,
                                          final long nHours,
                                          final long nMinutes,
                                          final long nSeconds)
  {
    // Use "abs" to ensure it is "1 year" and "-1 year"
    final String sYear = MathHelper.abs (nYears) == 1 ? nYears + " year" : nYears + " years";
    final String sMonth = MathHelper.abs (nMonths) == 1 ? nMonths + " month" : nMonths + " months";
    final String sDay = MathHelper.abs (nDays) == 1 ? nDays + " day" : nDays + " days";
    final String sHour = MathHelper.abs (nHours) == 1 ? nHours + " hour" : nHours + " hours";
    final String sMinute = MathHelper.abs (nMinutes) == 1 ? nMinutes + " minute" : nMinutes + " minutes";
    final String sSecond = MathHelper.abs (nSeconds) == 1 ? nSeconds + " second" : nSeconds + " seconds";

    // Skip all "leading 0" parts
    final ICommonsList <String> aParts = new CommonsArrayList <> (6);
    if (nYears != 0)
      aParts.add (sYear);
    if (nMonths != 0 || aParts.isNotEmpty ())
      aParts.add (sMonth);
    if (nDays != 0 || aParts.isNotEmpty ())
      aParts.add (sDay);
    if (nHours != 0 || aParts.isNotEmpty ())
      aParts.add (sHour);
    if (nMinutes != 0 || aParts.isNotEmpty ())
      aParts.add (sMinute);
    aParts.add (sSecond);

    final int nParts = aParts.size ();
    if (nParts == 1)
      return aParts.get (0);
    if (nParts == 2)
      return aParts.get (0) + " and " + aParts.get (1);
    final StringBuilder aSB = new StringBuilder ();
    for (int i = 0; i < nParts - 1; ++i)
    {
      if (aSB.length () > 0)
        aSB.append (", ");
      aSB.append (aParts.get (i));
    }
    return aSB.append (" and ").append (aParts.getLastOrNull ()).toString ();
  }

  @Nonnull
  private static String _inGroupsOf (@Nonnull final String s, final int nChars)
  {
    if (nChars < 1)
      return s;

    final int nMax = s.length ();
    // Worst case: 1 char 1 separator
    final StringBuilder aSB = new StringBuilder (nMax * 2);
    int nIndex = 0;
    while (nIndex < nMax - 1)
    {
      if (aSB.length () > 0)
        aSB.append (' ');
      aSB.append (s, nIndex, Integer.min (nIndex + nChars, nMax));
      nIndex += nChars;
    }
    return aSB.toString ();
  }

  @Nonnull
  public static String getCertIssuer (@Nonnull final X509Certificate aX509Cert)
  {
    return aX509Cert.getIssuerX500Principal ().getName ();
  }

  @Nonnull
  public static String getCertSubject (@Nonnull final X509Certificate aX509Cert)
  {
    return aX509Cert.getSubjectX500Principal ().getName ();
  }

  @Nonnull
  public static String getCertSerialNumber (@Nonnull final X509Certificate aX509Cert)
  {
    return aX509Cert.getSerialNumber ().toString () +
           " / 0x" +
           _inGroupsOf (aX509Cert.getSerialNumber ().toString (16), 4);
  }

  @Nonnull
  public static IHCNode getNodeCertNotBefore (@Nonnull final OffsetDateTime aNotBefore,
                                              @Nonnull final OffsetDateTime aNowDT,
                                              @Nonnull final Locale aDisplayLocale)
  {
    final HCNodeList ret = new HCNodeList ();
    ret.addChild (PDTToString.getAsString (aNotBefore, aDisplayLocale));
    if (aNowDT.isBefore (aNotBefore))
      ret.addChild (new HCDiv ().addChild (new BootstrapBadge (EBootstrapBadgeType.DANGER).addChild ("!!!NOT YET VALID!!!")));
    return ret;
  }

  @Nonnull
  public static IHCNode getNodeCertNotAfter (@Nonnull final OffsetDateTime aNotAfter,
                                             @Nonnull final OffsetDateTime aNowDT,
                                             @Nonnull final Locale aDisplayLocale)
  {
    final HCNodeList ret = new HCNodeList ();
    ret.addChild (PDTToString.getAsString (aNotAfter, aDisplayLocale));
    if (aNowDT.isAfter (aNotAfter))
      ret.addChild (" ").addChild (new BootstrapBadge (EBootstrapBadgeType.DANGER).addChild ("!!!NO LONGER VALID!!!"));
    else
      ret.addChild (" ")
         .addChild (new BootstrapBadge (EBootstrapBadgeType.SUCCESS).addChild ("Valid for: " +
                                                                               PDTDisplayHelper.getPeriodTextEN (aNowDT.toLocalDateTime (),
                                                                                                                 aNotAfter.toLocalDateTime ())));
    return ret;
  }

  @Nonnull
  public static BootstrapTable createCertificateDetailsTable (@Nullable final String sAlias,
                                                              @Nonnull final X509Certificate aX509Cert,
                                                              @Nonnull final OffsetDateTime aNowLDT,
                                                              @Nonnull final Locale aDisplayLocale)
  {
    final OffsetDateTime aNotBefore = PDTFactory.createOffsetDateTime (aX509Cert.getNotBefore ());
    final OffsetDateTime aNotAfter = PDTFactory.createOffsetDateTime (aX509Cert.getNotAfter ());
    final PublicKey aPublicKey = aX509Cert.getPublicKey ();

    final BootstrapTable aCertDetails = new BootstrapTable (new HCCol ().addStyle (CCSSProperties.WIDTH.newValue ("12rem")),
                                                            HCCol.star ());
    aCertDetails.setResponsive (true);
    if (StringHelper.isNotEmpty (sAlias))
      aCertDetails.addBodyRow ().addCell ("Alias:").addCell (sAlias);
    aCertDetails.addBodyRow ().addCell ("Version:").addCell (Integer.toString (aX509Cert.getVersion ()));
    aCertDetails.addBodyRow ().addCell ("Issuer:").addCell (getCertIssuer (aX509Cert));
    aCertDetails.addBodyRow ().addCell ("Subject:").addCell (getCertSubject (aX509Cert));
    aCertDetails.addBodyRow ().addCell ("Serial number:").addCell (getCertSerialNumber (aX509Cert));
    aCertDetails.addBodyRow ()
                .addCell ("Not before:")
                .addCell (getNodeCertNotBefore (aNotBefore, aNowLDT, aDisplayLocale));
    aCertDetails.addBodyRow ()
                .addCell ("Not after:")
                .addCell (getNodeCertNotAfter (aNotAfter, aNowLDT, aDisplayLocale));

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
    // Will be a DB query
    final IUser aOwner = PhotonSecurityManager.getUserMgr ().getUserOfID (sOwnerID);
    return aOwner == null ? sOwnerID : aOwner.getLoginName () + " (" + aOwner.getDisplayName () + ")";
  }

  @Nullable
  public static IHCNode getExtensionDisplay (@Nonnull final ISMPHasExtension aHasExtension)
  {
    final ICommonsList <SMPExtension> aExtensions = aHasExtension.getExtensions ().extensions ();
    if (aExtensions.isEmpty ())
      return null;

    final HCNodeList aNL = new HCNodeList ();
    for (final SMPExtension aExtension : aExtensions)
    {
      if (aNL.hasChildren ())
      {
        // add a separator line
        aNL.addChild (new HCHR ());
      }

      // Use only the XML element of the first extension
      final String sXML = XMLWriter.getNodeAsString (aExtension.getAny (),
                                                     new XMLWriterSettings ().setIndent (EXMLSerializeIndent.INDENT_AND_ALIGN));
      aNL.addChild (new HCPrismJS (EPrismLanguage.MARKUP).addChild (sXML));
    }
    return aNL;
  }

  @Nullable
  public static HCNodeList getTechnicalDetailsUI (@Nullable final Throwable t)
  {
    if (t == null)
      return null;

    LOGGER.warn ("Technical details", t);
    return BootstrapTechnicalUI.getTechnicalDetailsNode (t, CSMPServer.DEFAULT_LOCALE);
  }

  @Nullable
  public static String getTechnicalDetailsString (@Nullable final Throwable t)
  {
    if (t == null)
      return null;

    LOGGER.warn ("Technical details", t);
    return BootstrapTechnicalUI.getTechnicalDetailsString (t, CSMPServer.DEFAULT_LOCALE);
  }
}
