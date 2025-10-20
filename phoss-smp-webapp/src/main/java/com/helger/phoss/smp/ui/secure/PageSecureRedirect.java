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
package com.helger.phoss.smp.ui.secure;

import java.security.cert.X509Certificate;
import java.util.Locale;

import com.helger.annotation.Nonempty;
import com.helger.base.compare.ESortOrder;
import com.helger.base.state.EValidity;
import com.helger.base.state.IValidityIndicator;
import com.helger.base.string.StringHelper;
import com.helger.base.url.URLHelper;
import com.helger.html.hc.html.HC_Target;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCHiddenField;
import com.helger.html.hc.html.forms.HCTextArea;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppol.ui.nicename.NiceNameUI;
import com.helger.peppolid.CIdentifier;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.rest.SMPRestFilter;
import com.helger.phoss.smp.ui.AbstractSMPWebPageForm;
import com.helger.phoss.smp.ui.SMPExtensionUI;
import com.helger.phoss.smp.ui.secure.hc.HCServiceGroupSelect;
import com.helger.photon.app.url.LinkHelper;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.form.BootstrapViewForm;
import com.helger.photon.bootstrap4.pages.handler.AbstractBootstrapWebPageActionHandlerDelete;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.famfam.EFamFamIcon;
import com.helger.smpclient.extension.SMPExtensionList;
import com.helger.typeconvert.collection.StringMap;
import com.helger.url.ISimpleURL;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroReader;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public final class PageSecureRedirect extends AbstractSMPWebPageForm <ISMPRedirect>
{
  private static final String FIELD_SERVICE_GROUP_ID = "sgid";
  private static final String FIELD_DOCTYPE_ID = "doctypeid";
  private static final String FIELD_REDIRECT_TO = "redirectto";
  private static final String FIELD_SUBJECT_UNIQUE_IDENTIFIER = "suidentifier";
  private static final String FIELD_EXTENSION = "extension";

  private static final String ATTR_SERVICE_GROUP = "$servicegroup";
  private static final String ATTR_DOCTYPE_ID = "$doctypeid";

  public PageSecureRedirect (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Redirects");
    setDeleteHandler (new AbstractBootstrapWebPageActionHandlerDelete <ISMPRedirect, WebPageExecutionContext> ()
    {
      @Override
      protected void showQuery (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nonnull final BootstrapForm aForm,
                                @Nullable final ISMPRedirect aSelectedObject)
      {
        aForm.addChild (new HCHiddenField (FIELD_SERVICE_GROUP_ID, aSelectedObject.getServiceGroupID ()));
        aForm.addChild (new HCHiddenField (FIELD_DOCTYPE_ID,
                                           aSelectedObject.getDocumentTypeIdentifier ().getURIEncoded ()));

        aForm.addChild (question ("Are you sure you want to delete the Redirect for Service Group '" +
                                  aSelectedObject.getServiceGroupID () +
                                  "' and Document Type '" +
                                  aSelectedObject.getDocumentTypeIdentifier ().getURIEncoded () +
                                  "'?"));
      }

      @Override
      protected void performAction (@Nonnull final WebPageExecutionContext aWPEC,
                                    @Nullable final ISMPRedirect aSelectedObject)
      {
        final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
        if (aRedirectMgr.deleteSMPRedirect (aSelectedObject).isChanged ())
          aWPEC.postRedirectGetInternal (success ("The selected Redirect was successfully deleted!"));
        else
          aWPEC.postRedirectGetInternal (error ("Failed to delete the selected Redirect!"));
      }
    });
  }

  @Override
  @Nonnull
  protected IValidityIndicator isValidToDisplayPage (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPServiceGroupManager aServiceGroupManager = SMPMetaManager.getServiceGroupMgr ();
    if (aServiceGroupManager.getSMPServiceGroupCount () <= 0)
    {
      aNodeList.addChild (warn ("No Service Group is present! At least one Service Group must be present to create a Redirect for it."));
      aNodeList.addChild (new BootstrapButton ().addChild ("Create new Service Group")
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
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final String sServiceGroupID = aWPEC.params ().getAsStringTrimmed (FIELD_SERVICE_GROUP_ID);
    final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
    final ISMPServiceGroup aServiceGroup = SMPMetaManager.getServiceGroupMgr ()
                                                         .getSMPServiceGroupOfID (aServiceGroupID);
    if (aServiceGroup != null)
    {
      final String sDocTypeID = aWPEC.params ().getAsStringTrimmed (FIELD_DOCTYPE_ID);
      final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.parseDocumentTypeIdentifier (sDocTypeID);
      if (aDocTypeID != null)
      {
        final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
        return aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroupID, aDocTypeID);
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
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final String sServiceGroupID = aWPEC.params ().getAsStringTrimmed (FIELD_SERVICE_GROUP_ID);
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      final ISMPServiceGroup aServiceGroup = SMPMetaManager.getServiceGroupMgr ()
                                                           .getSMPServiceGroupOfID (aServiceGroupID);
      if (aServiceGroup != null)
      {
        final String sDocTypeID = aWPEC.params ().getAsStringTrimmed (FIELD_DOCTYPE_ID);
        final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.parseDocumentTypeIdentifier (sDocTypeID);
        if (aDocTypeID != null)
        {
          aWPEC.getRequestScope ().attrs ().putIn (ATTR_SERVICE_GROUP, aServiceGroup);
          aWPEC.getRequestScope ().attrs ().putIn (ATTR_DOCTYPE_ID, aDocTypeID);
          return true;
        }
      }
      return false;
    }
    return super.isActionAllowed (aWPEC, eFormAction, aSelectedObject);
  }

  @Override
  @Nonnull
  protected BootstrapButtonToolbar createViewToolbar (@Nonnull final WebPageExecutionContext aWPEC,
                                                      final boolean bCanGoBack,
                                                      @Nonnull final ISMPRedirect aSelectedObject)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    final BootstrapButtonToolbar aToolbar = createNewViewToolbar (aWPEC);
    if (bCanGoBack)
    {
      // Back to list
      aToolbar.addButtonBack (aDisplayLocale);
    }
    if (isActionAllowed (aWPEC, EWebPageFormAction.EDIT, aSelectedObject))
    {
      // Edit object
      aToolbar.addButtonEdit (aDisplayLocale,
                              createEditURL (aWPEC, aSelectedObject).add (FIELD_SERVICE_GROUP_ID,
                                                                          aSelectedObject.getServiceGroupID ())
                                                                    .add (FIELD_DOCTYPE_ID,
                                                                          aSelectedObject.getDocumentTypeIdentifier ()
                                                                                         .getURIEncoded ()));
    }

    // Callback
    modifyViewToolbar (aWPEC, aSelectedObject, aToolbar);
    return aToolbar;
  }

  @Override
  protected void showSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final ISMPRedirect aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    aNodeList.addChild (getUIHandler ().createActionHeader ("Show details of Redirect"));

    final BootstrapViewForm aForm = new BootstrapViewForm ();
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Service Group")
                                                 .setCtrl (new HCA (createViewURL (aWPEC,
                                                                                   CMenuSecure.MENU_SERVICE_GROUPS,
                                                                                   aSelectedObject.getServiceGroupID ())).addChild (aSelectedObject.getServiceGroupID ())));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Document type ID")
                                                 .setCtrl (NiceNameUI.createDocTypeID (aSelectedObject.getDocumentTypeIdentifier (),
                                                                                       true)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Target URL")
                                                 .setCtrl (HCA.createLinkedWebsite (aSelectedObject.getTargetHref (),
                                                                                    HC_Target.BLANK)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Subject unique identifier")
                                                 .setCtrl (aSelectedObject.getSubjectUniqueIdentifier ()));
    if (aSelectedObject.getExtensions ().extensions ().isNotEmpty ())
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Extension")
                                                   .setCtrl (SMPExtensionUI.getExtensionDisplay (aSelectedObject)));

    aNodeList.addChild (aForm);
  }

  @Override
  protected void validateAndSaveInputParameters (@Nonnull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMPRedirect aSelectedObject,
                                                 @Nonnull final FormErrorList aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final ISMPServiceGroupManager aServiceGroupManager = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();

    final String sServiceGroupID = bEdit ? aSelectedObject.getServiceGroupID () : aWPEC.params ()
                                                                                       .getAsStringTrimmed (FIELD_SERVICE_GROUP_ID);
    IParticipantIdentifier aParticipantID = null;
    ISMPServiceGroup aServiceGroup = null;
    final String sDocTypeID = bEdit ? aSelectedObject.getDocumentTypeIdentifier ().getURIEncoded () : aWPEC.params ()
                                                                                                           .getAsStringTrimmed (FIELD_DOCTYPE_ID);
    IDocumentTypeIdentifier aDocTypeID = null;
    final String sRedirectTo = aWPEC.params ().getAsStringTrimmed (FIELD_REDIRECT_TO);
    final String sSubjectUniqueIdentifier = aWPEC.params ().getAsStringTrimmed (FIELD_SUBJECT_UNIQUE_IDENTIFIER);
    // TODO add certificate redirect support
    final X509Certificate aCertificate = null;
    final String sExtension = aWPEC.params ().getAsStringTrimmed (FIELD_EXTENSION);

    // validations
    if (StringHelper.isEmpty (sServiceGroupID))
      aFormErrors.addFieldError (FIELD_SERVICE_GROUP_ID, "A Service Group must be selected!");
    else
    {
      aParticipantID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      aServiceGroup = aServiceGroupManager.getSMPServiceGroupOfID (aParticipantID);
      if (aServiceGroup == null)
        aFormErrors.addFieldError (FIELD_SERVICE_GROUP_ID, "The provided Service Group does not exist!");
    }

    if (StringHelper.isEmpty (sDocTypeID))
      aFormErrors.addFieldError (FIELD_DOCTYPE_ID, "Document Type ID must not be empty!");
    else
    {
      aDocTypeID = aIdentifierFactory.parseDocumentTypeIdentifier (sDocTypeID);
      if (aDocTypeID == null)
        aFormErrors.addFieldError (FIELD_DOCTYPE_ID, "The provided Document Type ID has an invalid syntax!");
      else
      {
        if (aServiceGroup != null)
        {
          if (aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aParticipantID, aDocTypeID) !=
              null)
            aFormErrors.addFieldError (FIELD_DOCTYPE_ID,
                                       "At least one Endpoint is registered for this Document Type. Delete the Endpoint before you can create a Redirect.");
          else
            if (!bEdit && aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (aParticipantID, aDocTypeID) != null)
              aFormErrors.addFieldError (FIELD_DOCTYPE_ID,
                                         "Another Redirect for the provided Service Group and Document Type is already present.");
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

    if (StringHelper.isNotEmpty (sExtension))
    {
      if (SMPExtensionUI.ONLY_ONE_EXTENSION_ALLOWED)
      {
        final IMicroDocument aDoc = MicroReader.readMicroXML (sExtension);
        if (aDoc == null)
          aFormErrors.addFieldError (FIELD_EXTENSION, "The Extension must be valid XML content.");
      }
      else
      {
        if (new SMPExtensionList ().setExtensionAsString (sExtension).isUnchanged ())
          aFormErrors.addFieldError (FIELD_EXTENSION, "The extension must be valid JSON or XML content.");
      }
    }

    if (aFormErrors.isEmpty ())
    {
      if (aRedirectMgr.createOrUpdateSMPRedirect (aParticipantID,
                                                  aDocTypeID,
                                                  sRedirectTo,
                                                  sSubjectUniqueIdentifier,
                                                  aCertificate,
                                                  sExtension) == null)
        aWPEC.postRedirectGetInternal (error ("Error creating the Redirect for Service Group '" +
                                              sServiceGroupID +
                                              "'."));
      else
        aWPEC.postRedirectGetInternal (success ("The Redirect for Service Group '" +
                                                sServiceGroupID +
                                                "' was successfully saved."));
    }
  }

  @Override
  protected void showInputForm (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMPRedirect aSelectedObject,
                                @Nonnull final BootstrapForm aForm,
                                final boolean bFormSubmitted,
                                @Nonnull final EWebPageFormAction eFormAction,
                                @Nonnull final FormErrorList aFormErrors)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();

    aForm.addChild (getUIHandler ().createActionHeader (bEdit ? "Edit Redirect" : "Create new Redirect"));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Service Group")
                                                 .setCtrl (new HCServiceGroupSelect (new RequestField (FIELD_SERVICE_GROUP_ID,
                                                                                                       aSelectedObject !=
                                                                                                                               null ? aSelectedObject.getServiceGroupID ()
                                                                                                                                    : null),
                                                                                     aDisplayLocale).setReadOnly (bEdit))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SERVICE_GROUP_ID)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Document Type ID")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_DOCTYPE_ID,
                                                                                         aSelectedObject != null
                                                                                                                 ? aSelectedObject.getDocumentTypeIdentifier ()
                                                                                                                                  .getURIEncoded ()
                                                                                                                 : aIdentifierFactory.getDefaultDocumentTypeIdentifierScheme () +
                                                                                                                   CIdentifier.URL_SCHEME_VALUE_SEPARATOR)).setReadOnly (bEdit))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_DOCTYPE_ID)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Redirect To")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_REDIRECT_TO,
                                                                                         aSelectedObject != null
                                                                                                                 ? aSelectedObject.getTargetHref ()
                                                                                                                 : null)))
                                                 .setHelpText ("URL to redirect to. Must include the service group and the document type in the URL!")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_REDIRECT_TO)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Subject Unique Identifier")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_SUBJECT_UNIQUE_IDENTIFIER,
                                                                                         aSelectedObject != null
                                                                                                                 ? aSelectedObject.getSubjectUniqueIdentifier ()
                                                                                                                 : null)))
                                                 .setHelpText ("Holds the Subject Unique Identifier of the certificate of the " +
                                                               "destination SMP. A client SHOULD validate that the Subject " +
                                                               "Unique Identifier of the certificate used to sign the resource at the " +
                                                               "destination SMP matches the Subject Unique Identifier published in " +
                                                               "the redirecting SMP.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SUBJECT_UNIQUE_IDENTIFIER)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Extension")
                                                 .setCtrl (new HCTextArea (new RequestField (FIELD_EXTENSION,
                                                                                             aSelectedObject != null
                                                                                                                     ? SMPExtensionUI.getSerializedExtensionsForEdit (aSelectedObject.getExtensions ())
                                                                                                                     : null)).setRows (CSMP.TEXT_AREA_CERT_EXTENSION))
                                                 .setHelpText ("Optional extension to the service group. If present it must be valid " +
                                                               (SMPExtensionUI.ONLY_ONE_EXTENSION_ALLOWED ? "XML"
                                                                                                              : "JSON or XML") +
                                                               "  content!")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_EXTENSION)));
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

    final HCTable aTable = new HCTable (new DTCol ("Service Group").setDataSort (0, 1)
                                                                   .setInitialSorting (ESortOrder.ASCENDING),
                                        new DTCol ("Document type ID").setDataSort (1, 0),
                                        new DTCol ("Target URL"),
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
    for (final ISMPRedirect aCurObject : aRedirectMgr.getAllSMPRedirects ())
    {
      final StringMap aParams = new StringMap ();
      aParams.putIn (FIELD_SERVICE_GROUP_ID, aCurObject.getServiceGroupID ());
      aParams.putIn (FIELD_DOCTYPE_ID, aCurObject.getDocumentTypeIdentifier ().getURIEncoded ());
      final ISimpleURL aViewLink = createViewURL (aWPEC, aCurObject, aParams);
      final String sDisplayName = aCurObject.getServiceGroupID ();

      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (new HCA (aViewLink).addChild (sDisplayName));
      aRow.addCell (NiceNameUI.createDocTypeID (aCurObject.getDocumentTypeIdentifier (), false));
      aRow.addCell (aCurObject.getTargetHref ());

      final ISimpleURL aEditURL = createEditURL (aWPEC, aCurObject).addAll (aParams);
      final ISimpleURL aCopyURL = createCopyURL (aWPEC, aCurObject).addAll (aParams);
      final ISimpleURL aDeleteURL = createDeleteURL (aWPEC, aCurObject).addAll (aParams);
      final ISimpleURL aPreviewURL = LinkHelper.getURLWithServerAndContext (aCurObject.getServiceGroupParticipantIdentifier ()
                                                                                      .getURIPercentEncoded () +
                                                                            SMPRestFilter.PATH_SERVICES +
                                                                            aCurObject.getDocumentTypeIdentifier ()
                                                                                      .getURIPercentEncoded ());
      aRow.addCell (new HCA (aEditURL).setTitle ("Edit " + sDisplayName).addChild (EDefaultIcon.EDIT.getAsNode ()),
                    new HCTextNode (" "),
                    new HCA (aCopyURL).setTitle ("Create a copy of " + sDisplayName)
                                      .addChild (EDefaultIcon.COPY.getAsNode ()),
                    new HCTextNode (" "),
                    new HCA (aDeleteURL).setTitle ("Delete " + sDisplayName)
                                        .addChild (EDefaultIcon.DELETE.getAsNode ()),
                    new HCTextNode (" "),
                    new HCA (aPreviewURL).setTitle ("Perform SMP query on " + sDisplayName)
                                         .setTargetBlank ()
                                         .addChild (EFamFamIcon.SCRIPT_GO.getAsNode ()));
    }

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);

    aNodeList.addChild (aTable).addChild (aDataTables);
  }
}
