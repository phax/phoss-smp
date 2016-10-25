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
package com.helger.peppol.smpserver.ui.secure;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.html.hc.html.forms.HCEditFile;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.smpserver.app.CSMPExchange;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCard;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroup;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPServiceInformationMicroTypeConverter;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPage;
import com.helger.peppol.smpserver.ui.ajax.CAjaxSecure;
import com.helger.photon.bootstrap3.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap3.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.button.BootstrapSubmitButton;
import com.helger.photon.bootstrap3.form.BootstrapCheckBox;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.label.BootstrapLabel;
import com.helger.photon.bootstrap3.label.EBootstrapLabelType;
import com.helger.photon.bootstrap3.nav.BootstrapTabBox;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestFieldBoolean;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.web.fileupload.IFileItem;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.convert.MicroTypeConverter;
import com.helger.xml.microdom.serialize.MicroReader;
import com.helger.xml.serialize.read.SAXReaderSettings;

/**
 * Class to import and export service groups with all contents
 *
 * @author Philip Helger
 */
public final class PageSecureServiceGroupExchange extends AbstractSMPWebPage
{
  private static final String FIELD_IMPORT_FILE = "importfile";
  private static final String FIELD_OVERWRITE_EXISTING = "overwriteexisting";
  private static final boolean DEFAULT_OVERWRITE_EXISTING = false;

  public PageSecureServiceGroupExchange (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Import/Export");
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ICommonsList <ISMPServiceGroup> aAllServiceGroups = aServiceGroupMgr.getAllSMPServiceGroups ();
    final FormErrorList aFormErrors = new FormErrorList ();

    if (aWPEC.hasAction (CPageParam.ACTION_PERFORM))
    {
      // Start import
      final IFileItem aImportFile = aWPEC.getFileItem (FIELD_IMPORT_FILE);
      final boolean bOverwriteExisting = aWPEC.getCheckBoxAttr (FIELD_OVERWRITE_EXISTING, DEFAULT_OVERWRITE_EXISTING);

      if (aImportFile == null || aImportFile.getSize () == 0)
        aFormErrors.addFieldError (FIELD_IMPORT_FILE, "A file to import must be selected!");
      else
      {
        final SAXReaderSettings aSRS = new SAXReaderSettings ();
        final IMicroDocument aDoc = MicroReader.readMicroXML (aImportFile, aSRS);
        if (aDoc == null || aDoc.getDocumentElement () == null)
          aFormErrors.addFieldError (FIELD_IMPORT_FILE, "The provided file is not a valid XML file!");
        else
        {
          // Start interpreting
          final HCUL aResultUL = new HCUL ();

          final String sVersion = aDoc.getDocumentElement ().getAttributeValue (CSMPExchange.ATTR_VERSION);
          if (CSMPExchange.VERSION_10.equals (sVersion))
          {
            final ICommonsList <ISMPServiceGroup> aServiceGroups = new CommonsArrayList<> ();
            final ICommonsList <ISMPBusinessCard> aBusinessCards = new CommonsArrayList<> ();
            int nSGIndex = 0;
            int nBCIndex = 0;
            for (final IMicroElement eChild : aDoc.getDocumentElement ().getAllChildElements ())
            {
              if (eChild.hasTagName (CSMPExchange.ELEMENT_SERVICEGROUP))
              {
                final ISMPServiceGroup aServiceGroup = MicroTypeConverter.convertToNative (eChild,
                                                                                           SMPServiceGroup.class);
                if (aServiceGroup == null)
                {
                  aResultUL.addItem (new BootstrapLabel (EBootstrapLabelType.DANGER).addChild ("Failed to read service group at index " +
                                                                                               nSGIndex));
                }
                else
                {
                  aServiceGroups.add (aServiceGroup);
                  aResultUL.addItem (new BootstrapLabel (EBootstrapLabelType.INFO).addChild ("Read service group " +
                                                                                             aServiceGroup.getID ()));

                  // read all contained service information
                  int nSIIndex = 0;
                  final ICommonsList <ISMPServiceInformation> aServiceInfos = new CommonsArrayList<> ();
                  for (final IMicroElement eServiceInfo : eChild.getAllChildElements (CSMPExchange.ELEMENT_SERVICEINFO))
                  {
                    final ISMPServiceInformation aServiceInfo = SMPServiceInformationMicroTypeConverter.convertToNative (eServiceInfo,
                                                                                                                         x -> aServiceGroup);
                    if (aServiceInfo == null)
                    {
                      aResultUL.addItem (new BootstrapLabel (EBootstrapLabelType.DANGER).addChild ("Failed to read service group " +
                                                                                                   aServiceGroup.getID () +
                                                                                                   " service information at index " +
                                                                                                   nSIIndex));
                    }
                    else
                    {
                      aServiceInfos.add (aServiceInfo);
                    }
                    ++nSIIndex;
                  }
                  aResultUL.addItem (new BootstrapLabel (EBootstrapLabelType.INFO).addChild ("Read " +
                                                                                             aServiceInfos.size () +
                                                                                             " service information of service group " +
                                                                                             aServiceGroup.getID ()));
                }
                ++nSGIndex;
              }
              else
                if (eChild.hasTagName (CSMPExchange.ELEMENT_BUSINESSCARD))
                {
                  ++nBCIndex;
                }
                else
                  aResultUL.addItem (new BootstrapLabel (EBootstrapLabelType.WARNING).addChild ("XML contains unsupported element '" +
                                                                                                eChild.getTagName () +
                                                                                                "'"));
            }
          }
          else
            aResultUL.addItem (new BootstrapLabel (EBootstrapLabelType.DANGER).addChild ("The XML file has the unsupported version '" +
                                                                                         sVersion +
                                                                                         "'"));
        }
      }
    }

    final BootstrapTabBox aTabBox = aNodeList.addAndReturnChild (new BootstrapTabBox ());
    {
      final HCNodeList aExport = new HCNodeList ();
      if (aAllServiceGroups.isEmpty ())
        aExport.addChild (new BootstrapWarnBox ().addChild ("Since no service group is present, nothing can be exported!"));
      else
        aExport.addChild (new BootstrapInfoBox ().addChild ("Export all " +
                                                            aAllServiceGroups.size () +
                                                            " service groups to an XML file."));

      final BootstrapButtonToolbar aToolbar = aExport.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
      aToolbar.addChild (new BootstrapButton ().addChild ("Export all Service Groups")
                                               .setIcon (EDefaultIcon.SAVE_ALL)
                                               .setOnClick (CAjaxSecure.FUNCTION_EXPORT_ALL_SERVICE_GROUPS.getInvocationURL (aRequestScope))
                                               .setDisabled (aAllServiceGroups.isEmpty ()));
      aTabBox.addTab ("export", "Export", aExport);
    }
    {
      final HCNodeList aImport = new HCNodeList ();
      final BootstrapForm aForm = aImport.addAndReturnChild (getUIHandler ().createFormFileUploadSelf (aWPEC));

      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("File to import")
                                                   .setCtrl (new HCEditFile (FIELD_IMPORT_FILE))
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_IMPORT_FILE)));
      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Overwrite existing service groups")
                                                   .setCtrl (new BootstrapCheckBox (new RequestFieldBoolean (FIELD_OVERWRITE_EXISTING,
                                                                                                             DEFAULT_OVERWRITE_EXISTING)))
                                                   .setHelpText ("If this box is checked, all existing endpoints etc. of a service group are deleted and new endpoints are created!")
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_OVERWRITE_EXISTING)));

      final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
      aToolbar.addHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM);
      aToolbar.addChild (new BootstrapSubmitButton ().addChild ("Import Service Groups").setIcon (EDefaultIcon.ADD));
      aTabBox.addTab ("import", "Import", aImport);
    }
  }
}
