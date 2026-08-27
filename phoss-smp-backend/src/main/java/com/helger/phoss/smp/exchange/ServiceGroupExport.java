/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.exchange;

import java.io.OutputStream;
import java.io.Writer;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.WillNotClose;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.nonblocking.NonBlockingBufferedWriter;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.state.ETriState;
import com.helger.collection.commons.ICommonsList;
import com.helger.mime.CMimeType;
import com.helger.mime.IMimeType;
import com.helger.mime.MimeType;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardMicroTypeConverter;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpointMicroTypeConverter;
import com.helger.phoss.smp.domain.serviceinfo.SMPProcessMicroTypeConverter;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformationMicroTypeConverter;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.convert.MicroTypeConverter;
import com.helger.xml.microdom.serialize.MicroSerializer;
import com.helger.xml.serialize.write.EXMLSerializeBracketMode;
import com.helger.xml.serialize.write.IXMLWriterSettings;
import com.helger.xml.serialize.write.XMLEmitter;
import com.helger.xml.serialize.write.XMLWriterSettings;

/**
 * Export Service Groups to XML.
 *
 * @author Philip Helger
 * @since 5.6.0
 */
@Immutable
public final class ServiceGroupExport
{
  /**
   * A {@link MicroSerializer} that acts as if it was already one level deep, because the root
   * element of the export is emitted separately. That way the indentation of the streamed export is
   * identical to the one of the in-memory export.
   *
   * @author Philip Helger
   */
  private static final class ExportMicroSerializer extends MicroSerializer
  {
    public ExportMicroSerializer (@NonNull final IXMLWriterSettings aSettings)
    {
      super (aSettings);
      if (aSettings.getIndent ().isIndent ())
        m_aIndent.append (aSettings.getIndentationString ());
    }
  }

  /** The XML writer settings used for creating the export data */
  public static final IXMLWriterSettings XML_WRITER_SETTINGS = XMLWriterSettings.DEFAULT_XML_SETTINGS;

  private static final Logger LOGGER = LoggerFactory.getLogger (ServiceGroupExport.class);

  private ServiceGroupExport ()
  {}

  /**
   * @return The MIME type of the created export data, including the charset parameter. Never
   *         <code>null</code>.
   * @since 8.2.1
   */
  @NonNull
  public static IMimeType getExportMimeType ()
  {
    return new MimeType (CMimeType.APPLICATION_XML).addParameter (CMimeType.PARAMETER_NAME_CHARSET,
                                                                  XML_WRITER_SETTINGS.getCharset ().name ());
  }

  /**
   * Create the XML element of a single Service Group, including all Service Information and all
   * Redirects.
   *
   * @param aServiceInfoMgr
   *        The Service Information manager to use. May not be <code>null</code>.
   * @param aRedirectMgr
   *        The Redirect manager to use. May not be <code>null</code>.
   * @param aServiceGroup
   *        The Service Group to be converted. May not be <code>null</code>.
   * @return The created XML element. Never <code>null</code>.
   */
  @NonNull
  private static IMicroElement _createServiceGroupElement (@NonNull final ISMPServiceInformationManager aServiceInfoMgr,
                                                           @NonNull final ISMPRedirectManager aRedirectMgr,
                                                           @NonNull final ISMPServiceGroup aServiceGroup)
  {
    final IMicroElement eServiceGroup = MicroTypeConverter.convertToMicroElement (aServiceGroup,
                                                                                  CSMPExchange.ELEMENT_SERVICEGROUP);

    // Add all service information
    final ICommonsList <ISMPServiceInformation> aAllServiceInfos = aServiceInfoMgr.getAllSMPServiceInformationOfServiceGroup (aServiceGroup.getParticipantIdentifier ());
    for (final ISMPServiceInformation aServiceInfo : aAllServiceInfos.getSortedInline (ISMPServiceInformation.comparator ()))
    {
      final IMicroElement eServiceInfo = MicroTypeConverter.convertToMicroElement (aServiceInfo,
                                                                                   CSMPExchange.ELEMENT_SERVICEINFO);
      // Remove the "id" attribute from all endpoints because we cannot guarantee it's uniqueness
      // over multiple installations
      for (final var eProcess : eServiceInfo.getAllChildElements (SMPServiceInformationMicroTypeConverter.ELEMENT_PROCESS))
        for (final var eEndpoint : eProcess.getAllChildElements (SMPProcessMicroTypeConverter.ELEMENT_ENDPOINT))
          eEndpoint.removeAttribute (SMPEndpointMicroTypeConverter.ATTR_ID);

      eServiceGroup.addChild (eServiceInfo);
    }

    // Add all redirects
    final ICommonsList <ISMPRedirect> aAllRedirects = aRedirectMgr.getAllSMPRedirectsOfServiceGroup (aServiceGroup.getParticipantIdentifier ());
    for (final ISMPRedirect aServiceInfo : aAllRedirects.getSortedInline (ISMPRedirect.comparator ()))
    {
      eServiceGroup.addChild (MicroTypeConverter.convertToMicroElement (aServiceInfo, CSMPExchange.ELEMENT_REDIRECT));
    }

    return eServiceGroup;
  }

  /**
   * Create XML export data for the provided service groups.<br>
   * Note: this method keeps the whole export in memory. For a potentially large number of Service
   * Groups use {@link #createExportDataXMLVer10(ICommonsList, boolean, OutputStream)} instead.
   *
   * @param aServiceGroups
   *        The service groups to export. May not be <code>null</code> but maybe empty.
   * @param bIncludeBusinessCards
   *        <code>true</code> to include Business Cards, <code>false</code> to skip them
   * @return The created XML document. Never <code>null</code>.
   */
  @NonNull
  public static IMicroDocument createExportDataXMLVer10 (@NonNull final ICommonsList <ISMPServiceGroup> aServiceGroups,
                                                         final boolean bIncludeBusinessCards)
  {
    ValueEnforcer.notNull (aServiceGroups, "ServiceGroups");

    LOGGER.info ("Start creating Service Group export data XML v1.0 for " +
                 aServiceGroups.size () +
                 " entries - " +
                 (bIncludeBusinessCards ? "incl. Business Cards" : "excl. Business Cards"));

    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();

    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement eRoot = aDoc.addElement (CSMPExchange.ELEMENT_SMP_DATA);
    eRoot.setAttribute (CSMPExchange.ATTR_VERSION, CSMPExchange.VERSION_10);
    eRoot.setAttribute (CSMPExchange.ATTR_SMP_VERSION, CSMPServer.getVersionNumber ());

    final ICommonsList <ISMPServiceGroup> aSortedServiceGroups = aServiceGroups.getSorted (ISMPServiceGroup.comparator ());

    // Add all service groups
    int nCount = 0;
    for (final ISMPServiceGroup aServiceGroup : aSortedServiceGroups)
    {
      if ((++nCount % 1_000) == 0)
        LOGGER.info ("  Now at " + nCount + " of " + aServiceGroups.size ());

      eRoot.addChild (_createServiceGroupElement (aServiceInfoMgr, aRedirectMgr, aServiceGroup));
    }

    // Add Business cards only if PD integration is enabled
    if (bIncludeBusinessCards)
    {
      LOGGER.info ("  Now exporting business groups");

      // Add all business cards
      final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
      for (final ISMPServiceGroup aServiceGroup : aSortedServiceGroups)
      {
        final ISMPBusinessCard aBusinessCard = aBusinessCardMgr.getSMPBusinessCardOfID (aServiceGroup.getParticipantIdentifier ());
        if (aBusinessCard != null)
        {
          eRoot.addChild (SMPBusinessCardMicroTypeConverter.convertToMicroElement (aBusinessCard,
                                                                                   null,
                                                                                   CSMPExchange.ELEMENT_BUSINESSCARD,
                                                                                   true));
        }
      }
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Finished creating Service Group XML data");

    return aDoc;
  }

  /**
   * Create XML export data for the provided service groups and write it directly to the provided
   * {@link OutputStream}. Contrary to {@link #createExportDataXMLVer10(ICommonsList, boolean)} only
   * the data of a single Service Group is kept in memory at a time, so that the memory consumption
   * does not depend on the total amount of exported data. The created XML is identical to the one
   * of the in-memory version.
   *
   * @param aServiceGroups
   *        The service groups to export. May not be <code>null</code> but maybe empty.
   * @param bIncludeBusinessCards
   *        <code>true</code> to include Business Cards, <code>false</code> to skip them
   * @param aOS
   *        The output stream to write to. May not be <code>null</code>. The stream is neither
   *        flushed nor closed by this method.
   * @since 8.2.1
   */
  public static void createExportDataXMLVer10 (@NonNull final ICommonsList <ISMPServiceGroup> aServiceGroups,
                                               final boolean bIncludeBusinessCards,
                                               @NonNull @WillNotClose final OutputStream aOS)
  {
    ValueEnforcer.notNull (aServiceGroups, "ServiceGroups");
    ValueEnforcer.notNull (aOS, "OutputStream");

    LOGGER.info ("Start streaming Service Group export data XML v1.0 for " +
                 aServiceGroups.size () +
                 " entries - " +
                 (bIncludeBusinessCards ? "incl. Business Cards" : "excl. Business Cards"));

    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();

    final IXMLWriterSettings aXWS = XML_WRITER_SETTINGS;
    final ICommonsList <ISMPServiceGroup> aSortedServiceGroups = aServiceGroups.getSorted (ISMPServiceGroup.comparator ());

    // Deliberately not closed - closing is up to the caller of this method
    final Writer aWriter = new NonBlockingBufferedWriter (StreamHelper.createWriter (aOS, aXWS.getCharset ()));
    final XMLEmitter aEmitter = new XMLEmitter (aWriter, aXWS);
    final MicroSerializer aMicroSerializer = new ExportMicroSerializer (aXWS);

    // Emit the XML declaration and the root element manually, so that the children can be written
    // one by one
    if (aXWS.getSerializeXMLDeclaration ().isEmit ())
    {
      aEmitter.onXMLDeclaration (aXWS.getXMLVersion (),
                                 aXWS.getCharset ().name (),
                                 ETriState.UNDEFINED,
                                 aXWS.isNewLineAfterXMLDeclaration ());
    }
    aEmitter.elementStartOpen (null, CSMPExchange.ELEMENT_SMP_DATA);
    aEmitter.elementAttr (null, CSMPExchange.ATTR_VERSION, CSMPExchange.VERSION_10);
    aEmitter.elementAttr (null, CSMPExchange.ATTR_SMP_VERSION, CSMPServer.getVersionNumber ());
    aEmitter.elementStartClose (EXMLSerializeBracketMode.OPEN_CLOSE);
    aEmitter.newLine ();

    // Add all service groups
    int nCount = 0;
    for (final ISMPServiceGroup aServiceGroup : aSortedServiceGroups)
    {
      if ((++nCount % 1_000) == 0)
        LOGGER.info ("  Now at " + nCount + " of " + aServiceGroups.size ());

      aMicroSerializer.write (_createServiceGroupElement (aServiceInfoMgr, aRedirectMgr, aServiceGroup), aEmitter);
    }

    // Add Business cards only if PD integration is enabled
    if (bIncludeBusinessCards)
    {
      LOGGER.info ("  Now exporting business groups");

      // Add all business cards - deliberately one by one, so that never all Business Cards are in
      // memory at the same time
      final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
      for (final ISMPServiceGroup aServiceGroup : aSortedServiceGroups)
      {
        final ISMPBusinessCard aBusinessCard = aBusinessCardMgr.getSMPBusinessCardOfID (aServiceGroup.getParticipantIdentifier ());
        if (aBusinessCard != null)
        {
          aMicroSerializer.write (SMPBusinessCardMicroTypeConverter.convertToMicroElement (aBusinessCard,
                                                                                           null,
                                                                                           CSMPExchange.ELEMENT_BUSINESSCARD,
                                                                                           true),
                                  aEmitter);
        }
      }
    }

    aEmitter.onElementEnd (null, CSMPExchange.ELEMENT_SMP_DATA, EXMLSerializeBracketMode.OPEN_CLOSE);
    aEmitter.newLine ();

    // The Writer is buffered, so it must be flushed to reach the OutputStream
    StreamHelper.flush (aWriter);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Finished streaming Service Group XML data");
  }
}
