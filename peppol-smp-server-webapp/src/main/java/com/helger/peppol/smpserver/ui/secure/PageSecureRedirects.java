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

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.WorkInProgress;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.errorlist.FormErrors;
import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.serialize.MicroReader;
import com.helger.commons.state.EValidity;
import com.helger.commons.state.IValidityIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.SMap;
import com.helger.commons.url.URLHelper;
import com.helger.html.hc.html.HC_Target;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCHiddenField;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppol.identifier.CIdentifier;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirect;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPageForm;
import com.helger.peppol.smpserver.ui.AppCommonUI;
import com.helger.peppol.smpserver.ui.secure.hc.HCServiceGroupSelect;
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

@WorkInProgress
public final class PageSecureRedirects extends AbstractSMPWebPageForm <ISMPRedirect>
{
  private static final String FIELD_SERVICE_GROUP_ID = "sgid";
  private static final String FIELD_DOCTYPE_ID = "doctypeid";
  private static final String FIELD_REDIRECT_TO = "redirectto";
  private static final String FIELD_SUBJECT_UNIQUE_IDENTIFIER = "suidentifier";
  private static final String FIELD_EXTENSION = "extension";

  private static final String ATTR_SERVICE_GROUP = "$servicegroup";
  private static final String ATTR_DOCTYPE_ID = "$doctypeid";

  public PageSecureRedirects (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Redirects");
  }

  @Override
  @Nonnull
  protected IValidityIndicator isValidToDisplayPage (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPServiceGroupManager aServiceGroupManager = SMPMetaManager.getServiceGroupMgr ();
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
  @Nullable
  protected ISMPRedirect getSelectedObject (@Nonnull final WebPageExecutionContext aWPEC, @Nullable final String sID)
  {
    final String sServiceGroupID = aWPEC.getAttributeAsString (FIELD_SERVICE_GROUP_ID);
    final SimpleParticipantIdentifier aServiceGroupID = SimpleParticipantIdentifier.createFromURIPartOrNull (sServiceGroupID);
    final ISMPServiceGroup aServiceGroup = SMPMetaManager.getServiceGroupMgr ().getSMPServiceGroupOfID (aServiceGroupID);
    if (aServiceGroup != null)
    {
      final String sDocTypeID = aWPEC.getAttributeAsString (FIELD_DOCTYPE_ID);
      final SimpleDocumentTypeIdentifier aDocTypeID = SimpleDocumentTypeIdentifier.createFromURIPartOrNull (sDocTypeID);
      if (aDocTypeID != null)
      {
        final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
        return aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup, aDocTypeID);
      }
    }
    return null;
  }

  @Override
  protected boolean isActionAllowed (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final EWebPageFormAction eFormAction,
                                     @Nullable final ISMPRedirect aSelectedObject)
  {
    if (eFormAction == EWebPageFormAction.VIEW ||
        eFormAction == EWebPageFormAction.COPY ||
        eFormAction == EWebPageFormAction.EDIT ||
        eFormAction == EWebPageFormAction.DELETE)
    {
      final String sServiceGroupID = aWPEC.getAttributeAsString (FIELD_SERVICE_GROUP_ID);
      final SimpleParticipantIdentifier aServiceGroupID = SimpleParticipantIdentifier.createFromURIPartOrNull (sServiceGroupID);
      final ISMPServiceGroup aServiceGroup = SMPMetaManager.getServiceGroupMgr ().getSMPServiceGroupOfID (aServiceGroupID);
      if (aServiceGroup != null)
      {
        final String sDocTypeID = aWPEC.getAttributeAsString (FIELD_DOCTYPE_ID);
        final SimpleDocumentTypeIdentifier aDocTypeID = SimpleDocumentTypeIdentifier.createFromURIPartOrNull (sDocTypeID);
        if (aDocTypeID != null)
        {
          aWPEC.getRequestScope ().setAttribute (ATTR_SERVICE_GROUP, aServiceGroup);
          aWPEC.getRequestScope ().setAttribute (ATTR_DOCTYPE_ID, aDocTypeID);
          return true;
        }
      }
      return false;
    }
    return super.isActionAllowed (aWPEC, eFormAction, aSelectedObject);
  }

  @Override
  protected void showSelectedObject (@Nonnull final WebPageExecutionContext aWPEC, @Nonnull final ISMPRedirect aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    aNodeList.addChild (createActionHeader ("Show details of redirect"));

    final BootstrapViewForm aForm = new BootstrapViewForm ();
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Service Group")
                                                 .setCtrl (new HCA (createViewURL (aWPEC,
                                                                                   CMenuSecure.MENU_SERVICE_GROUPS,
                                                                                   aSelectedObject.getServiceGroup ())).addChild (aSelectedObject.getServiceGroupID ())));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Document type ID")
                                                 .setCtrl (aSelectedObject.getDocumentTypeIdentifier ().getURIEncoded ()));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Target URL")
                                                 .setCtrl (HCA.createLinkedWebsite (aSelectedObject.getTargetHref (), HC_Target.BLANK)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Subject unique identifier").setCtrl (aSelectedObject.getSubjectUniqueIdentifier ()));
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
    final ISMPServiceGroupManager aServiceGroupManager = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();

    final String sServiceGroupID = bEdit ? aSelectedObject.getServiceGroupID () : aWPEC.getAttributeAsString (FIELD_SERVICE_GROUP_ID);
    ISMPServiceGroup aServiceGroup = null;
    final String sDocTypeID = bEdit ? aSelectedObject.getDocumentTypeIdentifier ().getURIEncoded () : aWPEC.getAttributeAsString (FIELD_DOCTYPE_ID);
    IDocumentTypeIdentifier aDocTypeID = null;
    final String sRedirectTo = aWPEC.getAttributeAsString (FIELD_REDIRECT_TO);
    final String sSubjectUniqueIdentifier = aWPEC.getAttributeAsString (FIELD_SUBJECT_UNIQUE_IDENTIFIER);
    final String sExtension = aWPEC.getAttributeAsString (FIELD_EXTENSION);

    // validations
    if (StringHelper.isEmpty (sServiceGroupID))
      aFormErrors.addFieldError (FIELD_SERVICE_GROUP_ID, "A service group must be selected!");
    else
    {
      aServiceGroup = aServiceGroupManager.getSMPServiceGroupOfID (SimpleParticipantIdentifier.createFromURIPartOrNull (sServiceGroupID));
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
        {
          if (aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDocTypeID) != null)
            aFormErrors.addFieldError (FIELD_DOCTYPE_ID,
                                       "At least one endpoint is registered for this document type. Delete the endpoint before you can create a redirect.");
          else
            if (!bEdit && aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup, aDocTypeID) != null)
              aFormErrors.addFieldError (FIELD_DOCTYPE_ID, "Another redirect for the provided service group and document type is already present.");
        }
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
      aRedirectMgr.createOrUpdateSMPRedirect (aServiceGroup, aDocTypeID, sRedirectTo, sSubjectUniqueIdentifier, sExtension);
      aWPEC.postRedirectGet (new BootstrapSuccessBox ().addChild ("The redirect for service group '" +
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
    aForm.addChild (new HCHiddenField (FIELD_SERVICE_GROUP_ID, aSelectedObject.getServiceGroup ().getParticpantIdentifier ().getURIEncoded ()));
    aForm.addChild (new HCHiddenField (FIELD_DOCTYPE_ID, aSelectedObject.getDocumentTypeIdentifier ().getURIEncoded ()));

    aForm.addChild (new BootstrapQuestionBox ().addChild ("Are you sure you want to delete the redirect for service group '" +
                                                          aSelectedObject.getServiceGroupID () +
                                                          "' and document type '" +
                                                          aSelectedObject.getDocumentTypeIdentifier ().getURIEncoded () +
                                                          "'?"));
  }

  @Override
  protected void performDelete (@Nonnull final WebPageExecutionContext aWPEC, @Nonnull final ISMPRedirect aSelectedObject)
  {
    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
    if (aRedirectMgr.deleteSMPRedirect (aSelectedObject).isChanged ())
      aWPEC.postRedirectGet (new BootstrapSuccessBox ().addChild ("The selected redirect was successfully deleted!"));
    else
      aWPEC.postRedirectGet (new BootstrapErrorBox ().addChild ("Failed to delete the selected redirect!"));
  }

  @Override
  protected void showListOfExistingObjects (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();

    final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
    aToolbar.addButton ("Create new Redirect", createCreateURL (aWPEC), EDefaultIcon.NEW);
    aNodeList.addChild (aToolbar);

    final HCTable aTable = new HCTable (new DTCol ("Service Group").setDataSort (0, 1).setInitialSorting (ESortOrder.ASCENDING),
                                        new DTCol ("Document type ID").setDataSort (1, 0),
                                        new DTCol ("Target URL"),
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
    for (final ISMPRedirect aCurObject : aRedirectMgr.getAllSMPRedirects ())
    {
      final SMap aParams = new SMap ().add (FIELD_SERVICE_GROUP_ID, aCurObject.getServiceGroupID ())
                                      .add (FIELD_DOCTYPE_ID, aCurObject.getDocumentTypeIdentifier ().getURIEncoded ());
      final ISimpleURL aViewLink = createViewURL (aWPEC, aCurObject, aParams);
      final String sDisplayName = aCurObject.getServiceGroupID ();

      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (new HCA (aViewLink).addChild (sDisplayName));
      aRow.addCell (AppCommonUI.getDocumentTypeID (aCurObject.getDocumentTypeIdentifier ()));
      aRow.addCell (aCurObject.getTargetHref ());

      final ISimpleURL aEditURL = createEditURL (aWPEC, aCurObject).addAll (aParams);
      final ISimpleURL aCopyURL = createCopyURL (aWPEC, aCurObject).addAll (aParams);
      final ISimpleURL aDeleteURL = createDeleteURL (aWPEC, aCurObject).addAll (aParams);
      aRow.addCell (new HCA (aEditURL).setTitle ("Edit " + sDisplayName).addChild (EDefaultIcon.EDIT.getAsNode ()),
                    new HCTextNode (" "),
                    new HCA (aCopyURL).setTitle ("Create a copy of " + sDisplayName).addChild (EDefaultIcon.COPY.getAsNode ()),
                    new HCTextNode (" "),
                    new HCA (aDeleteURL).setTitle ("Delete " + sDisplayName).addChild (EDefaultIcon.DELETE.getAsNode ()),
                    new HCTextNode (" "),
                    new HCA (LinkHelper.getURLWithServerAndContext (aCurObject.getServiceGroup ().getParticpantIdentifier ().getURIPercentEncoded () +
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
