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

import java.util.Collection;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.WorkInProgress;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.serialize.MicroReader;
import com.helger.commons.string.StringHelper;
import com.helger.commons.type.EBaseType;
import com.helger.commons.url.ISimpleURL;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppol.identifier.CIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smpserver.data.dao.DAODataManager;
import com.helger.peppol.smpserver.data.dao.MetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.servicemetadata.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.servicemetadata.SMPServiceInformationManager;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPageForm;
import com.helger.photon.basic.security.AccessManager;
import com.helger.photon.basic.security.login.LoggedInUserManager;
import com.helger.photon.basic.security.user.IUser;
import com.helger.photon.bootstrap3.alert.BootstrapQuestionBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.BootstrapViewForm;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.EPhotonCoreText;
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
public final class PageSecureServiceGroups extends AbstractSMPWebPageForm <ISMPServiceGroup>
{
  private final static String FIELD_PARTICIPANT_ID = "participantid";
  private final static String FIELD_OWNING_USER_ID = "owninguser";
  private final static String FIELD_EXTENSION = "extension";

  public PageSecureServiceGroups (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Service groups");
  }

  @Override
  @Nullable
  protected ISMPServiceGroup getSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                                @Nullable final String sID)
  {
    final SMPServiceGroupManager aServiceGroupMgr = MetaManager.getServiceGroupMgr ();
    return aServiceGroupMgr.getSMPServiceGroupOfID (sID);
  }

  @Override
  protected void showSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final ISMPServiceGroup aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    aNodeList.addChild (createActionHeader ("Show details of service group"));

    final BootstrapViewForm aForm = new BootstrapViewForm ();
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Participant ID")
                                                 .setCtrl (aSelectedObject.getParticpantIdentifier ()
                                                                          .getURIEncoded ()));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Owning user")
                                                 .setCtrl (aSelectedObject.getOwner ().getLoginName () +
                                                           " (" +
                                                           aSelectedObject.getOwner ().getDisplayName () +
                                                           ")"));
    if (aSelectedObject.hasExtension ())
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Extension")
                                                   .setCtrl (new HCPrismJS (EPrismLanguage.MARKUP).addChild (aSelectedObject.getExtension ())));

    aNodeList.addChild (aForm);
  }

  @Override
  protected void validateAndSaveInputParameters (@Nonnull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMPServiceGroup aSelectedObject,
                                                 @Nonnull final FormErrors aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final boolean bEdit = eFormAction.isEdit ();

    final String sParticipantID = aWPEC.getAttributeAsString (FIELD_PARTICIPANT_ID);
    SimpleParticipantIdentifier aParticipantID = null;
    final String sOwningUserID = aWPEC.getAttributeAsString (FIELD_OWNING_USER_ID);
    final IUser aOwningUser = AccessManager.getInstance ().getUserOfID (sOwningUserID);
    final String sExtension = aWPEC.getAttributeAsString (FIELD_EXTENSION);

    // validations
    if (StringHelper.hasNoText (sParticipantID))
      aFormErrors.addFieldError (FIELD_PARTICIPANT_ID, "Participant ID must not be empty!");
    else
    {
      aParticipantID = SimpleParticipantIdentifier.createFromURIPartOrNull (sParticipantID);
      if (aParticipantID == null)
        aFormErrors.addFieldError (FIELD_PARTICIPANT_ID, "The provided participant ID has an invalid syntax!");
    }

    if (StringHelper.isEmpty (sOwningUserID))
      aFormErrors.addFieldError (FIELD_OWNING_USER_ID, "Owning User must not be empty!");
    else
      if (aOwningUser == null)
        aFormErrors.addFieldError (FIELD_OWNING_USER_ID, "Provided owning user does not exist!");

    if (StringHelper.hasText (sExtension))
    {
      final IMicroDocument aDoc = MicroReader.readMicroXML (sExtension);
      if (aDoc == null)
        aFormErrors.addFieldError (FIELD_EXTENSION, "The extension must be XML content.");
    }

    if (aFormErrors.isEmpty ())
    {
      final SMPServiceGroupManager aServiceGroupMgr = MetaManager.getServiceGroupMgr ();
      if (bEdit)
      {
        // Edit only the internal data objects because no change to the SML is
        // necessary. Only the owner and the extension can be edited!
        aServiceGroupMgr.updateSMPServiceGroup (aSelectedObject.getID (), aOwningUser, sExtension);
        aNodeList.addChild (new BootstrapSuccessBox ().addChild ("The SMP ServiceGroup for participant '" +
                                                                 sParticipantID +
                                                                 "' was successfully edited."));
      }
      else
      {
        if (true)
        {
          // Create the service group both locally and on the SML!
          DAODataManager.getInstance ().saveServiceGroup (aParticipantID, sExtension, aOwningUser);
        }
        else
        {
          // Create the service group only locally but NOT on the SML!
          aServiceGroupMgr.createSMPServiceGroup (aOwningUser, aParticipantID, sExtension);
        }
        aNodeList.addChild (new BootstrapSuccessBox ().addChild ("The new SMP ServiceGroup for participant '" +
                                                                 sParticipantID +
                                                                 "' was successfully created."));
      }
    }
  }

  @Override
  protected void showInputForm (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMPServiceGroup aSelectedObject,
                                @Nonnull final BootstrapForm aForm,
                                @Nonnull final EWebPageFormAction eFormAction,
                                @Nonnull final FormErrors aFormErrors)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final boolean bEdit = eFormAction.isEdit ();

    aForm.addChild (createActionHeader (bEdit ? "Edit service group" : "Create new service group"));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Participant ID")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_PARTICIPANT_ID,
                                                                                         aSelectedObject != null ? aSelectedObject.getParticpantIdentifier ()
                                                                                                                                  .getURIEncoded ()
                                                                                                                 : CIdentifier.DEFAULT_PARTICIPANT_IDENTIFIER_SCHEME +
                                                                                                                   CIdentifier.URL_SCHEME_VALUE_SEPARATOR)).setReadOnly (bEdit))
                                                 .setHelpText ("The participant identifier for which the service group should be created. It must contain the full identifier scheme and value. Example: iso6523-actorid-upis::9915:test")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_PARTICIPANT_ID)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Owning User")
                                                 .setCtrl (new HCUserSelect (new RequestField (FIELD_OWNING_USER_ID,
                                                                                               aSelectedObject != null ? aSelectedObject.getOwnerID ()
                                                                                                                       : LoggedInUserManager.getInstance ()
                                                                                                                                            .getCurrentUserID ()),
                                                                             aDisplayLocale))
                                                 .setHelpText ("The user who owns this entry. Only this user can make changes via the REST API.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_OWNING_USER_ID)));

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
                                  @Nonnull final ISMPServiceGroup aSelectedObject)
  {
    aForm.addChild (new BootstrapQuestionBox ().addChild (new HCDiv ().addChild ("Are you sure you want to delete the complete service group '" +
                                                                                 aSelectedObject.getParticpantIdentifier ()
                                                                                                .getURIEncoded () +
                                                                                 "'?"))
                                               .addChild (new HCDiv ().addChild ("This means that all endpoints and all redirects are deleted as well."))
                                               .addChild (new HCDiv ().addChild ("If the connection to the SML is active this service group will also be deleted from the SML!")));
  }

  @Override
  protected void performDelete (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nonnull final ISMPServiceGroup aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    // Create the service group both locally and on the SML!
    DAODataManager.getInstance ().deleteServiceGroup (
                                                      new SimpleParticipantIdentifier (aSelectedObject.getParticpantIdentifier ()),
                                                      aSelectedObject.getOwner ().getLoginName ());
    aNodeList.addChild (new BootstrapSuccessBox ().addChild ("The SMP ServiceGroup for participant '" +
                                                             aSelectedObject.getParticpantIdentifier ()
                                                                            .getURIEncoded () +
                                                             "' was successfully deleted!"));
  }

  @Override
  protected boolean handleCustomActions (@Nonnull final WebPageExecutionContext aWPEC,
                                         @Nullable final ISMPServiceGroup aSelectedObject)
  {
    return super.handleCustomActions (aWPEC, aSelectedObject);
  }

  @Override
  protected void showListOfExistingObjects (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final SMPServiceGroupManager aServiceGroupMgr = MetaManager.getServiceGroupMgr ();
    final SMPServiceInformationManager aServiceInfoMgr = MetaManager.getServiceInformationMgr ();

    final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
    aToolbar.addButton ("Create new Service group", createCreateURL (aWPEC), EDefaultIcon.NEW);
    aNodeList.addChild (aToolbar);

    final HCTable aTable = new HCTable (new DTCol ("Participant ID").setInitialSorting (ESortOrder.ASCENDING),
                                        new DTCol ("Owner"),
                                        new DTCol ("Has extension?"),
                                        new DTCol ("DocTypes").setDisplayType (EBaseType.INT, aDisplayLocale),
                                        new DTCol ("Processes").setDisplayType (EBaseType.INT, aDisplayLocale),
                                        new DTCol ("Endpoints").setDisplayType (EBaseType.INT, aDisplayLocale),
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
    for (final ISMPServiceGroup aCurObject : aServiceGroupMgr.getAllSMPServiceGroups ())
    {
      final Collection <? extends ISMPServiceInformation> aSIs = aServiceInfoMgr.getAllSMPServiceInformationsOfServiceGroup (aCurObject);
      int nProcesses = 0;
      int nEndpoints = 0;
      for (final ISMPServiceInformation aSI : aSIs)
      {
        nProcesses += aSI.getProcessCount ();
        nEndpoints += aSI.getTotalEndpointCount ();
      }

      final ISimpleURL aViewLink = createViewURL (aWPEC, aCurObject);
      final String sDisplayName = aCurObject.getParticpantIdentifier ().getURIEncoded ();

      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (new HCA (aViewLink).addChild (sDisplayName));
      aRow.addCell (aCurObject.getOwner ().getLoginName ());
      aRow.addCell (EPhotonCoreText.getYesOrNo (aCurObject.hasExtension (), aDisplayLocale));
      aRow.addCell (Integer.toString (aSIs.size ()));
      aRow.addCell (Integer.toString (nProcesses));
      aRow.addCell (Integer.toString (nEndpoints));

      aRow.addCell (createEditLink (aWPEC, aCurObject, "Edit " + sDisplayName),
                    new HCTextNode (" "),
                    createCopyLink (aWPEC, aCurObject, "Copy " + sDisplayName),
                    new HCTextNode (" "),
                    createDeleteLink (aWPEC, aCurObject, "Delete " + sDisplayName),
                    new HCTextNode (" "),
                    new HCA (LinkHelper.getURIWithServerAndContext (aCurObject.getParticpantIdentifier ()
                                                                              .getURIPercentEncoded ())).setTitle ("Perform SMP query on " +
                                                                                                                   sDisplayName)
                                                                                                        .setTargetBlank ()
                                                                                                        .addChild (EFamFamIcon.SCRIPT_GO.getAsNode ()),
                    new HCTextNode (" "),
                    new HCA (LinkHelper.getURIWithServerAndContext ("complete/" +
                                                                    aCurObject.getParticpantIdentifier ()
                                                                              .getURIPercentEncoded ())).setTitle ("Perform complete SMP query on " +
                                                                                                                   sDisplayName)
                                                                                                        .setTargetBlank ()
                                                                                                        .addChild (EFamFamIcon.SCRIPT_LINK.getAsNode ()));
    }

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
    aNodeList.addChild (aTable).addChild (aDataTables);
  }
}
