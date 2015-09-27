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
package com.helger.peppol.smpserver.ui.secure;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.WorkInProgress;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.serialize.MicroReader;
import com.helger.commons.state.EValidity;
import com.helger.commons.state.IValidityIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.URLHelper;
import com.helger.html.hc.html.HC_Target;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppol.identifier.CIdentifier;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.smpserver.data.dao.MetaManager;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirect;
import com.helger.peppol.smpserver.domain.redirect.SMPRedirectManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.servicemetadata.SMPServiceInformationManager;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPageForm;
import com.helger.peppol.smpserver.ui.AppCommonUI;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap3.alert.BootstrapQuestionBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.BootstrapViewForm;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.url.LinkHelper;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.autosize.HCTextAreaAutosize;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.famfam.EFamFamIcon;
import com.helger.photon.uictrls.prism.EPrismLanguage;
import com.helger.photon.uictrls.prism.HCPrismJS;
import com.helger.validation.error.FormErrors;

@WorkInProgress
public final class PageSecureRedirects extends AbstractSMPWebPageForm <ISMPRedirect>
{
  private final static String FIELD_SERVICE_GROUP_ID = "sgid";
  private final static String FIELD_DOCTYPE_ID = "doctypeid";
  private final static String FIELD_REDIRECT_TO = "redirectto";
  private final static String FIELD_SUBJECT_UNIQUE_IDENTIFIER = "suidentifier";
  private final static String FIELD_EXTENSION = "extension";

  public PageSecureRedirects (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Redirects");
  }

  @Override
  @Nonnull
  protected IValidityIndicator isValidToDisplayPage (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final SMPServiceGroupManager aServiceGroupManager = MetaManager.getServiceGroupMgr ();
    if (aServiceGroupManager.getSMPServiceGroupCount () == 0)
    {
      aNodeList.addChild (new BootstrapWarnBox ().addChild ("No service group is present! At least one service group must be present to create a redirect for it."));
      aNodeList.addChild (new BootstrapButton ().addChild ("Create new service group")
                                                .setOnClick (createCreateURL (aWPEC, CMenuSecure.MENU_SERVICE_GROUPS))
                                                .setIcon (EDefaultIcon.YES));
      return EValidity.INVALID;
    }
    return super.isValidToDisplayPage (aWPEC);
  }

  @Override
  protected boolean isActionAllowed (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final EWebPageFormAction eFormAction,
                                     @Nullable final ISMPRedirect aSelectedObject)
  {
    return super.isActionAllowed (aWPEC, eFormAction, aSelectedObject);
  }

  @Override
  @Nullable
  protected ISMPRedirect getSelectedObject (@Nonnull final WebPageExecutionContext aWPEC, @Nullable final String sID)
  {
    final SMPRedirectManager aRedirectMgr = MetaManager.getRedirectMgr ();
    return aRedirectMgr.getSMPRedirectOfID (sID);
  }

  @Override
  protected void showSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final ISMPRedirect aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    aNodeList.addChild (createActionHeader ("Show details of redirect"));

    final BootstrapViewForm aForm = new BootstrapViewForm ();
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Service Group")
                                                 .setCtrl (new HCA (createViewURL (aWPEC,
                                                                                   CMenuSecure.MENU_SERVICE_GROUPS,
                                                                                   aSelectedObject.getServiceGroup ())).addChild (aSelectedObject.getServiceGroupID ())));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Document type ID")
                                                 .setCtrl (aSelectedObject.getDocumentTypeIdentifier ()
                                                                          .getURIEncoded ()));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Target URL")
                                                 .setCtrl (HCA.createLinkedWebsite (aSelectedObject.getTargetHref (),
                                                                                    HC_Target.BLANK)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Subject unique identifier")
                                                 .setCtrl (aSelectedObject.getSubjectUniqueIdentifier ()));
    if (aSelectedObject.hasExtension ())
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Extension")
                                                   .setCtrl (new HCPrismJS (EPrismLanguage.MARKUP).addChild (aSelectedObject.getExtension ())));

    aNodeList.addChild (aForm);
  }

  @Override
  protected void validateAndSaveInputParameters (@Nonnull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMPRedirect aSelectedObject,
                                                 @Nonnull final FormErrors aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final SMPServiceGroupManager aServiceGroupManager = MetaManager.getServiceGroupMgr ();
    final SMPServiceInformationManager aServiceInfoMgr = MetaManager.getServiceInformationMgr ();
    final SMPRedirectManager aRedirectMgr = MetaManager.getRedirectMgr ();

    final String sServiceGroupID = bEdit ? aSelectedObject.getServiceGroupID ()
                                         : aWPEC.getAttributeAsString (FIELD_SERVICE_GROUP_ID);
    ISMPServiceGroup aServiceGroup = null;
    final String sDocTypeID = bEdit ? aSelectedObject.getDocumentTypeIdentifier ().getURIEncoded ()
                                    : aWPEC.getAttributeAsString (FIELD_DOCTYPE_ID);
    IDocumentTypeIdentifier aDocTypeID = null;
    final String sRedirectTo = aWPEC.getAttributeAsString (FIELD_REDIRECT_TO);
    final String sSubjectUniqueIdentifier = aWPEC.getAttributeAsString (FIELD_SUBJECT_UNIQUE_IDENTIFIER);
    final String sExtension = aWPEC.getAttributeAsString (FIELD_EXTENSION);

    // validations
    if (StringHelper.isEmpty (sServiceGroupID))
      aFormErrors.addFieldError (FIELD_SERVICE_GROUP_ID, "A service group must be selected!");
    else
    {
      aServiceGroup = aServiceGroupManager.getSMPServiceGroupOfID (sServiceGroupID);
      if (aServiceGroup == null)
        aFormErrors.addFieldError (FIELD_SERVICE_GROUP_ID, "The provided service group does not exist!");
    }

    if (StringHelper.isEmpty (sDocTypeID))
      aFormErrors.addFieldError (FIELD_DOCTYPE_ID, "Document type ID must not be empty!");
    else
    {
      aDocTypeID = SimpleDocumentTypeIdentifier.createFromURIPartOrNull (sDocTypeID);
      if (aDocTypeID == null)
        aFormErrors.addFieldError (FIELD_DOCTYPE_ID, "The provided document type ID has an invalid syntax!");
      else
      {
        if (aServiceGroup != null)
          if (aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup.getID (),
                                                                                     aDocTypeID) != null)
            aFormErrors.addFieldError (FIELD_DOCTYPE_ID,
                                       "At least one endpoint is registered for this document type. Delete the endpoint before you can create a redirect.");
      }
    }

    if (StringHelper.isEmpty (sRedirectTo))
      aFormErrors.addFieldError (FIELD_REDIRECT_TO, "The Redirect URL must not be empty!");
    else
      if (URLHelper.getAsURL (sRedirectTo) == null)
        aFormErrors.addFieldError (FIELD_REDIRECT_TO, "The Redirect URL is not a valid URL!");

    if (StringHelper.isEmpty (sSubjectUniqueIdentifier))
      aFormErrors.addFieldError (FIELD_SUBJECT_UNIQUE_IDENTIFIER, "Subject Unique Identifier must not be empty!");

    if (StringHelper.hasText (sExtension))
    {
      final IMicroDocument aDoc = MicroReader.readMicroXML (sExtension);
      if (aDoc == null)
        aFormErrors.addFieldError (FIELD_EXTENSION, "The extension must be XML content.");
    }

    if (aFormErrors.isEmpty ())
    {
      aRedirectMgr.createSMPRedirect (aServiceGroup, aDocTypeID, sRedirectTo, sSubjectUniqueIdentifier, sExtension);
      aNodeList.addChild (new BootstrapSuccessBox ().addChild ("The redirect for service group '" +
                                                               sServiceGroupID +
                                                               "' was successfully saved."));
    }
  }

  @Override
  protected void showInputForm (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMPRedirect aSelectedObject,
                                @Nonnull final BootstrapForm aForm,
                                @Nonnull final EWebPageFormAction eFormAction,
                                @Nonnull final FormErrors aFormErrors)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    aForm.addChild (createActionHeader (bEdit ? "Edit redirect" : "Create new redirect"));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Service group")
                                                 .setCtrl (new HCServiceGroupSelect (new RequestField (FIELD_SERVICE_GROUP_ID,
                                                                                                       aSelectedObject != null ? aSelectedObject.getServiceGroupID ()
                                                                                                                               : null),
                                                                                     aDisplayLocale).setReadOnly (bEdit))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SERVICE_GROUP_ID)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Document type ID")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_DOCTYPE_ID,
                                                                                         aSelectedObject != null ? aSelectedObject.getDocumentTypeIdentifier ()
                                                                                                                                  .getURIEncoded ()
                                                                                                                 : CIdentifier.DEFAULT_DOCUMENT_TYPE_IDENTIFIER_SCHEME +
                                                                                                                   CIdentifier.URL_SCHEME_VALUE_SEPARATOR)).setReadOnly (bEdit))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_DOCTYPE_ID)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Redirect To")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_REDIRECT_TO,
                                                                                         aSelectedObject != null ? aSelectedObject.getTargetHref ()
                                                                                                                 : null)))
                                                 .setHelpText ("URL to redirect to.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_REDIRECT_TO)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Subject Unique Identifier")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_SUBJECT_UNIQUE_IDENTIFIER,
                                                                                         aSelectedObject != null ? aSelectedObject.getSubjectUniqueIdentifier ()
                                                                                                                 : null)))
                                                 .setHelpText ("Holds the Subject Unique Identifier of the certificate of the\r\n" +
                                                               "destination SMP. A client SHOULD validate that the Subject\r\n" +
                                                               "Unique Identifier of the certificate used to sign the resource at the\r\n" +
                                                               "destination SMP matches the Subject Unique Identifier published in\r\n" +
                                                               "the redirecting SMP.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SUBJECT_UNIQUE_IDENTIFIER)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Extension")
                                                 .setCtrl (new HCTextAreaAutosize (new RequestField (FIELD_EXTENSION,
                                                                                                     aSelectedObject != null ? aSelectedObject.getExtension ()
                                                                                                                             : null)))
                                                 .setHelpText ("Optional extension to the service group. If present it must be valid XML content!")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_EXTENSION)));
  }

  @Override
  protected void showDeleteQuery (@Nonnull final WebPageExecutionContext aWPEC,
                                  @Nonnull final BootstrapForm aForm,
                                  @Nonnull final ISMPRedirect aSelectedObject)
  {
    aForm.addChild (new BootstrapQuestionBox ().addChild ("Are you sure you want to delete the redirect for service group '" +
                                                          aSelectedObject.getServiceGroupID () +
                                                          "' and document type '" +
                                                          aSelectedObject.getDocumentTypeIdentifier ()
                                                                         .getURIEncoded () +
                                                          "'?"));
  }

  @Override
  protected void performDelete (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nonnull final ISMPRedirect aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final SMPRedirectManager aRedirectMgr = MetaManager.getRedirectMgr ();
    if (aRedirectMgr.deleteSMPRedirect (aSelectedObject).isChanged ())
      aNodeList.addChild (new BootstrapSuccessBox ().addChild ("The selected redirect was successfully deleted!"));
    else
      aNodeList.addChild (new BootstrapErrorBox ().addChild ("Failed to delete the selected redirect!"));
  }

  @Override
  protected void showListOfExistingObjects (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final SMPRedirectManager aRedirectMgr = MetaManager.getRedirectMgr ();

    final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
    aToolbar.addButton ("Create new Redirect", createCreateURL (aWPEC), EDefaultIcon.NEW);
    aNodeList.addChild (aToolbar);

    final HCTable aTable = new HCTable (new DTCol ("Service Group").setInitialSorting (ESortOrder.ASCENDING),
                                        new DTCol ("Document type ID"),
                                        new DTCol ("Target URL"),
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
    for (final ISMPRedirect aCurObject : aRedirectMgr.getAllSMPRedirects ())
    {
      final ISimpleURL aViewLink = createViewURL (aWPEC, aCurObject);
      final String sDisplayName = aCurObject.getServiceGroupID ();

      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (new HCA (aViewLink).addChild (sDisplayName));
      aRow.addCell (AppCommonUI.getDocumentTypeID (aCurObject.getDocumentTypeIdentifier ()));
      aRow.addCell (aCurObject.getTargetHref ());
      aRow.addCell (createEditLink (aWPEC, aCurObject, "Edit " + sDisplayName),
                    new HCTextNode (" "),
                    createCopyLink (aWPEC, aCurObject, "Create a copy of " + sDisplayName),
                    new HCTextNode (" "),
                    createDeleteLink (aWPEC, aCurObject, "Delete " + sDisplayName),
                    new HCTextNode (" "),
                    new HCA (LinkHelper.getURIWithServerAndContext (aCurObject.getServiceGroup ()
                                                                              .getParticpantIdentifier ()
                                                                              .getURIPercentEncoded () +
                                                                    "/services/" +
                                                                    aCurObject.getDocumentTypeIdentifier ()
                                                                              .getURIPercentEncoded ())).setTitle ("Perform SMP query on " +
                                                                                                                   sDisplayName)
                                                                                                        .setTargetBlank ()
                                                                                                        .addChild (EFamFamIcon.SCRIPT_GO.getAsNode ()));
    }

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);

    aNodeList.addChild (aTable).addChild (aDataTables);
  }
}
