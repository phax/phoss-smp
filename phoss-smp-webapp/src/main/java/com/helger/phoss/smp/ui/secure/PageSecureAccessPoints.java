/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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
import java.time.OffsetDateTime;
import java.util.Locale;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.base.url.URLHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.forms.HCCheckBox;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCHiddenField;
import com.helger.html.hc.html.forms.HCTextArea;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppol.ui.CertificateUI;
import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.accesspoint.ESMPAccessPointColumn;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;
import com.helger.phoss.smp.domain.accesspoint.SMPAccessPointHelper;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.ui.AbstractSMPWebPageForm;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.phoss.smp.ui.ajax.CAjax;
import com.helger.photon.ajax.decl.IAjaxFunctionDeclaration;
import com.helger.photon.bootstrap5.button.BootstrapButton;
import com.helger.photon.bootstrap5.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap5.form.BootstrapForm;
import com.helger.photon.bootstrap5.form.BootstrapFormGroup;
import com.helger.photon.bootstrap5.form.BootstrapViewForm;
import com.helger.photon.bootstrap5.pages.handler.AbstractBootstrapWebPageActionHandler;
import com.helger.photon.bootstrap5.pages.handler.AbstractBootstrapWebPageActionHandlerDelete;
import com.helger.photon.bootstrap5.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap5.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.form.RequestFieldBoolean;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EShowList;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.ajax.DataTablesOnDemandHelper;
import com.helger.photon.uictrls.datatables.ajax.DataTablesOnDemandRequest;
import com.helger.photon.uictrls.datatables.ajax.DataTablesOnDemandResult;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.datatables.column.EDTColType;
import com.helger.security.certificate.CertificateDecodeHelper;
import com.helger.url.ISimpleURL;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.annotation.Nullable;

/**
 * Administration page for all Access Points. An Access Point is an optional, named combination of
 * an endpoint reference URL and a certificate that can be referenced from multiple endpoints.
 *
 * @author Philip Helger
 * @since 8.4.4
 */
public final class PageSecureAccessPoints extends AbstractSMPWebPageForm <ISMPAccessPoint>
{
  private static final String FIELD_NAME = "name";
  private static final String FIELD_ENDPOINT_REFERENCE = "endpointreference";
  private static final String FIELD_CERTIFICATE = "certificate";
  private static final String FIELD_REQUIRE_SAME_URL = "requiresameurl";
  private static final boolean DEFAULT_REQUIRE_SAME_URL = true;
  private static final String ACTION_USE_FOR_MATCHING = "use-for-matching";

  /**
   * Provides the rows of a single page - see
   * {@link #_getOnDemandData(DataTablesOnDemandRequest, IRequestWebScopeWithoutResponse)}
   */
  private final IAjaxFunctionDeclaration m_aAjaxOnDemand = DataTablesOnDemandHelper.registerAjaxFunction (this::_getOnDemandData,
                                                                                                          CAjax.FILTER_IS_USER_LOGGED_IN);

  public PageSecureAccessPoints (@NonNull @Nonempty final String sID)
  {
    super (sID, "Access Points");
    setDeleteHandler (new AbstractBootstrapWebPageActionHandlerDelete <ISMPAccessPoint, WebPageExecutionContext> ()
    {
      @Override
      protected void showQuery (@NonNull final WebPageExecutionContext aWPEC,
                                @NonNull final BootstrapForm aForm,
                                @Nullable final ISMPAccessPoint aSelectedObject)
      {
        aForm.addChild (question ("Are you sure you want to delete the Access Point '" +
                                  aSelectedObject.getName () +
                                  "'?"));
      }

      @Override
      protected void performAction (@NonNull final WebPageExecutionContext aWPEC,
                                    @Nullable final ISMPAccessPoint aSelectedObject)
      {
        final ISMPAccessPointManager aAccessPointMgr = SMPMetaManager.getAccessPointMgr ();
        if (aAccessPointMgr.deleteAccessPoint (aSelectedObject.getID ()).isChanged ())
          aWPEC.postRedirectGetInternal (success ("The Access Point '" +
                                                  aSelectedObject.getName () +
                                                  "' was successfully deleted!"));
        else
          aWPEC.postRedirectGetInternal (error ("Failed to delete Access Point '" +
                                                aSelectedObject.getName () +
                                                "'!"));
      }
    });
    addCustomHandler (ACTION_USE_FOR_MATCHING,
                      new AbstractBootstrapWebPageActionHandler <ISMPAccessPoint, WebPageExecutionContext> (true)
                      {
                        @NonNull
                        public EShowList handleAction (@NonNull final WebPageExecutionContext aWPEC,
                                                       @Nullable final ISMPAccessPoint aSelectedObject)
                        {
                          return _handleUseForMatching (aWPEC, aSelectedObject);
                        }
                      });
  }

  @NonNull
  private EShowList _handleUseForMatching (@NonNull final WebPageExecutionContext aWPEC,
                                           @NonNull final ISMPAccessPoint aSelectedObject)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    if (!aSelectedObject.hasCertificate ())
    {
      aWPEC.postRedirectGetInternal (error ("The Access Point '" +
                                            aSelectedObject.getName () +
                                            "' has no certificate, so no matching endpoints can be determined."));
      return EShowList.SHOW_LIST;
    }

    final boolean bRequireSameURL = aWPEC.params ()
                                         .isCheckBoxChecked (FIELD_REQUIRE_SAME_URL, DEFAULT_REQUIRE_SAME_URL);

    if (aWPEC.hasSubAction (CPageParam.ACTION_SAVE))
    {
      final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
      final long nChanged = aServiceInfoMgr.useAccessPointForMatchingEndpoints (aSelectedObject.getID (),
                                                                                bRequireSameURL);
      if (nChanged > 0)
        aWPEC.postRedirectGetInternal (success (nChanged +
                                                " " +
                                                (nChanged == 1 ? "endpoint is" : "endpoints are") +
                                                " now referencing the Access Point '" +
                                                aSelectedObject.getName () +
                                                "'."));
      else
        aWPEC.postRedirectGetInternal (warn ("No matching endpoint was found - nothing was changed."));
      return EShowList.SHOW_LIST;
    }

    aWPEC.getNodeList ()
         .addChild (getUIHandler ().createActionHeader ("Use Access Point '" +
                                                        aSelectedObject.getName () +
                                                        "' in all matching endpoints"));

    final BootstrapForm aForm = aWPEC.getNodeList ().addAndReturnChild (getUIHandler ().createFormSelf (aWPEC));
    aForm.addChild (new HCHiddenField (CPageParam.PARAM_ACTION, ACTION_USE_FOR_MATCHING));
    aForm.addChild (new HCHiddenField (CPageParam.PARAM_OBJECT, aSelectedObject.getID ()));
    aForm.addChild (new HCHiddenField (CPageParam.PARAM_SUBACTION, CPageParam.ACTION_SAVE));

    aForm.addChild (info ().addChildren (div ("All endpoints that contain the certificate of this Access Point directly - " +
                                              "and that do not yet reference any Access Point - can be changed to reference this Access Point instead."),
                                         div ("This operation is optional. Existing endpoints keep their directly stored " +
                                              "data as long as you do not run this function.")));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Certificate")
                                                 .setCtrl (_getCertificateDisplay (aSelectedObject.getCertificate (),
                                                                                   aDisplayLocale)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Only endpoints with the same Endpoint Reference")
                                                 .setCtrl (new HCCheckBox (new RequestFieldBoolean (FIELD_REQUIRE_SAME_URL,
                                                                                                    DEFAULT_REQUIRE_SAME_URL)))
                                                 .setHelpText ("If checked, only endpoints that additionally use the Endpoint Reference '" +
                                                               aSelectedObject.getEndpointReference () +
                                                               "' are changed. This ensures that no endpoint data is altered. " +
                                                               "If unchecked, the Endpoint Reference of all matching endpoints is " +
                                                               "changed to the one of this Access Point as well."));

    final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
    aToolbar.addSubmitButton ("Use this Access Point", EDefaultIcon.YES);
    aToolbar.addButtonCancel (aDisplayLocale);

    return EShowList.DONT_SHOW_LIST;
  }

  @NonNull
  private IHCNode _getCertificateDisplay (@Nullable final String sCert, @NonNull final Locale aDisplayLocale)
  {
    if (StringHelper.isEmpty (sCert))
      return em ("none");

    final X509Certificate aCert = new CertificateDecodeHelper ().source (sCert).pemEncoded (true).getDecodedOrNull ();
    if (aCert == null)
      return em ("The provided certificate cannot be interpreted as an X.509 certificate");

    final OffsetDateTime aNowODT = PDTFactory.getCurrentOffsetDateTime ();
    return CertificateUI.createCertificateDetailsTable (null, aCert, aNowODT, aDisplayLocale);
  }

  @Override
  protected ISMPAccessPoint getSelectedObject (@NonNull final WebPageExecutionContext aWPEC,
                                               @Nullable final String sID)
  {
    return SMPMetaManager.getAccessPointMgr ().getAccessPointOfID (sID);
  }

  @Override
  protected boolean isActionAllowed (@NonNull final WebPageExecutionContext aWPEC,
                                     @NonNull final EWebPageFormAction eFormAction,
                                     @Nullable final ISMPAccessPoint aSelectedObject)
  {
    if (eFormAction.isDelete ())
    {
      // An Access Point that is in use cannot be deleted
      final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
      if (aServiceInfoMgr.containsAnyEndpointWithAccessPoint (aSelectedObject.getID ()))
        return false;
    }
    return super.isActionAllowed (aWPEC, eFormAction, aSelectedObject);
  }

  @Override
  protected void showSelectedObject (@NonNull final WebPageExecutionContext aWPEC,
                                     @NonNull final ISMPAccessPoint aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();

    aNodeList.addChild (getUIHandler ().createActionHeader ("Show details of Access Point '" +
                                                            aSelectedObject.getName () +
                                                            "'"));

    final BootstrapViewForm aForm = new BootstrapViewForm ();
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Name").setCtrl (aSelectedObject.getName ()));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Endpoint Reference")
                                                 .setCtrl (StringHelper.isEmpty (aSelectedObject.getEndpointReference ()) ? em ("none")
                                                                                                                          : HCA.createLinkedWebsite (aSelectedObject.getEndpointReference ())));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Certificate")
                                                 .setCtrl (_getCertificateDisplay (aSelectedObject.getCertificate (),
                                                                                   aDisplayLocale)));
    final long nEndpoints = aServiceInfoMgr.getEndpointCountUsingAccessPoint (aSelectedObject.getID ());
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Referencing endpoints")
                                                 .setCtrl (Long.toString (nEndpoints)));

    aNodeList.addChild (aForm);
  }

  @Override
  protected void showInputForm (@NonNull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMPAccessPoint aSelectedObject,
                                @NonNull final BootstrapForm aForm,
                                final boolean bFormSubmitted,
                                @NonNull final EWebPageFormAction eFormAction,
                                @NonNull final FormErrorList aFormErrors)
  {
    final boolean bEdit = eFormAction.isEdit ();

    aForm.addChild (getUIHandler ().createActionHeader (bEdit ? "Edit Access Point '" +
                                                                aSelectedObject.getName () +
                                                                "'" : "Create new Access Point"));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Name")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_NAME,
                                                                                         aSelectedObject != null ? aSelectedObject.getName ()
                                                                                                                 : null)))
                                                 .setHelpText ("The unique name of the Access Point. It is used to reference this " +
                                                               "Access Point in the REST API. Allowed characters are letters, digits, " +
                                                               "'.', '-' and '_'.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_NAME)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Endpoint Reference")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_ENDPOINT_REFERENCE,
                                                                                         aSelectedObject != null ? aSelectedObject.getEndpointReference ()
                                                                                                                 : null)))
                                                 .setHelpText ("The URL where messages should be targeted to. Changing it changes the " +
                                                               "URL of all endpoints referencing this Access Point.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_ENDPOINT_REFERENCE)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Certificate")
                                                 .setCtrl (new HCTextArea (new RequestField (FIELD_CERTIFICATE,
                                                                                             aSelectedObject != null ? aSelectedObject.getCertificate ()
                                                                                                                     : null)).setRows (CSMP.TEXT_AREA_CERT_ROWS))
                                                 .setHelpText ("The PEM encoded X.509 certificate of this Access Point. Changing it " +
                                                               "changes the certificate of all endpoints referencing this Access Point.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_CERTIFICATE)));
  }

  @Override
  protected void validateAndSaveInputParameters (@NonNull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMPAccessPoint aSelectedObject,
                                                 @NonNull final FormErrorList aFormErrors,
                                                 @NonNull final EWebPageFormAction eFormAction)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final ISMPAccessPointManager aAccessPointMgr = SMPMetaManager.getAccessPointMgr ();

    final String sName = aWPEC.params ().getAsStringTrimmed (FIELD_NAME);
    final String sEndpointReference = aWPEC.params ().getAsStringTrimmed (FIELD_ENDPOINT_REFERENCE);
    final String sCertificate = aWPEC.params ().getAsStringTrimmed (FIELD_CERTIFICATE);

    if (StringHelper.isEmpty (sName))
      aFormErrors.addFieldError (FIELD_NAME, "The Access Point name must not be empty!");
    else
      if (!SMPAccessPointHelper.isValidName (sName))
        aFormErrors.addFieldError (FIELD_NAME,
                                   "The Access Point name is invalid. It must start with a letter or a digit, may only " +
                                                "contain letters, digits, '.', '-' and '_' and may not be longer than " +
                                                SMPAccessPointHelper.NAME_MAX_LENGTH +
                                                " characters.");
      else
      {
        final ISMPAccessPoint aOther = aAccessPointMgr.getAccessPointOfName (sName);
        if (aOther != null && (!bEdit || !aOther.getID ().equals (aSelectedObject.getID ())))
          aFormErrors.addFieldError (FIELD_NAME, "Another Access Point with the same name already exists!");
      }

    if (StringHelper.isEmpty (sEndpointReference))
      aFormErrors.addFieldError (FIELD_ENDPOINT_REFERENCE, "The Endpoint Reference must not be empty!");
    else
      if (URLHelper.getAsURL (sEndpointReference) == null)
        aFormErrors.addFieldError (FIELD_ENDPOINT_REFERENCE, "The Endpoint Reference is not a valid URL!");
      else
        if (sEndpointReference.length () > ISMPAccessPointManager.ENDPOINT_REFERENCE_MAX_LENGTH)
          aFormErrors.addFieldError (FIELD_ENDPOINT_REFERENCE,
                                     "The Endpoint Reference may not be longer than " +
                                                               ISMPAccessPointManager.ENDPOINT_REFERENCE_MAX_LENGTH +
                                                               " characters.");

    if (StringHelper.isEmpty (sCertificate))
      aFormErrors.addFieldError (FIELD_CERTIFICATE, "The certificate must not be empty!");
    else
    {
      final X509Certificate aCert = new CertificateDecodeHelper ().source (sCertificate)
                                                                  .pemEncoded (true)
                                                                  .getDecodedOrNull ();
      if (aCert == null)
        aFormErrors.addFieldError (FIELD_CERTIFICATE,
                                   "The provided certificate string is not a valid X509 certificate!");
    }

    if (aFormErrors.isEmpty ())
    {
      if (bEdit)
      {
        if (aAccessPointMgr.updateAccessPoint (aSelectedObject.getID (), sName, sEndpointReference, sCertificate)
                           .isChanged ())
          aWPEC.postRedirectGetInternal (success ("The Access Point '" + sName + "' was successfully edited."));
        else
          aWPEC.postRedirectGetInternal (info ("No change editing the Access Point '" + sName + "'."));
      }
      else
      {
        if (aAccessPointMgr.createAccessPoint (sName, sEndpointReference, sCertificate) != null)
          aWPEC.postRedirectGetInternal (success ("The new Access Point '" + sName + "' was successfully created."));
        else
          aWPEC.postRedirectGetInternal (error ("Failed to create the Access Point '" + sName + "'."));
      }
    }
  }

  private void _addRow (@NonNull final WebPageExecutionContext aWPEC,
                        @NonNull final HCRow aRow,
                        @NonNull final ISMPAccessPoint aCurObject)
  {
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final ISimpleURL aViewLink = createViewURL (aWPEC, aCurObject);

    aRow.addCell (new HCA (aViewLink).addChild (aCurObject.getName ()));
    aRow.addCell (aCurObject.getEndpointReference ());
    aRow.addCell (Long.toString (aServiceInfoMgr.getEndpointCountUsingAccessPoint (aCurObject.getID ())));

    final ISimpleURL aUseLink = aWPEC.getSelfHref ()
                                     .add (CPageParam.PARAM_ACTION, ACTION_USE_FOR_MATCHING)
                                     .add (CPageParam.PARAM_OBJECT, aCurObject.getID ());
    aRow.addCell (createEditLink (aWPEC, aCurObject, "Edit " + aCurObject.getName ()),
                  new HCTextNode (" "),
                  createCopyLink (aWPEC, aCurObject, "Copy " + aCurObject.getName ()),
                  new HCTextNode (" "),
                  isActionAllowed (aWPEC, EWebPageFormAction.DELETE, aCurObject) ? createDeleteLink (aWPEC,
                                                                                                     aCurObject,
                                                                                                     "Delete " +
                                                                                                                 aCurObject.getName ())
                                                                                 : createEmptyAction (),
                  new HCTextNode (" "),
                  new HCA (aUseLink).setTitle ("Use this Access Point in all endpoints with the same certificate")
                                    .addChild (EDefaultIcon.NEXT.getAsNode ()));
  }

  @NonNull
  private HCTable _createTable (@NonNull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    // The column names are the IDs of ESMPAccessPointColumn, so that the sort order requested by
    // the client can be resolved onto the respective SQL column or MongoDB field
    return new HCTable (new DTCol ("Name").setName (ESMPAccessPointColumn.NAME.getID ()),
                        new DTCol ("Endpoint Reference").setName (ESMPAccessPointColumn.ENDPOINT_REFERENCE.getID ()),
                        new DTCol ("Endpoints").setDisplayType (EDTColType.INT, aDisplayLocale).setOrderable (false),
                        new BootstrapDTColAction (aDisplayLocale).setOrderable (false)).setID (getID ());
  }

  /**
   * Provide the rows of a single page - only the requested chunk of Access Points is loaded from
   * the backend.
   *
   * @param aRequest
   *        The DataTables request containing the paging specification and the search text.
   * @param aRequestScope
   *        The current request scope.
   * @return The data of the requested page. Never <code>null</code>.
   */
  @NonNull
  private DataTablesOnDemandResult _getOnDemandData (@NonNull final DataTablesOnDemandRequest aRequest,
                                                     @NonNull final IRequestWebScopeWithoutResponse aRequestScope)
  {
    final WebPageExecutionContext aWPEC = new WebPageExecutionContext (LayoutExecutionContext.createForAjaxOrAction (aRequestScope),
                                                                      this);
    final ISMPAccessPointManager aAccessPointMgr = SMPMetaManager.getAccessPointMgr ();
    final String sSearchText = aRequest.getSearchText ();

    final ICommonsList <HCRow> aRows = new CommonsArrayList <> ();
    for (final ISMPAccessPoint aCurObject : aAccessPointMgr.getAllAccessPoints (aRequest.getPagingSpec (),
                                                                                sSearchText))
    {
      final HCRow aRow = new HCRow ();
      _addRow (aWPEC, aRow, aCurObject);
      aRows.add (aRow);
    }
    return new DataTablesOnDemandResult (aAccessPointMgr.getAccessPointCount (),
                                         aAccessPointMgr.getAccessPointCount (sSearchText),
                                         aRows);
  }

  @Override
  protected void showListOfExistingObjects (@NonNull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    aNodeList.addChild (info ().addChildren (div ("An Access Point is a named combination of an Endpoint Reference URL and a certificate."),
                                             div ("Referencing an Access Point from an endpoint is optional - an endpoint may also " +
                                                  "contain the Endpoint Reference and the certificate directly, but never both at the same time.")));

    final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
    aToolbar.addChild (new BootstrapButton ().addChild ("Create new Access Point")
                                             .setOnClick (createCreateURL (aWPEC))
                                             .setIcon (EDefaultIcon.NEW));
    aNodeList.addChild (aToolbar);

    // Server side pagination - only a single page of Access Points is loaded at a time
    final HCTable aTable = _createTable (aWPEC);
    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
    DataTablesOnDemandHelper.applyOnDemandMode (aDataTables,
                                                aTable,
                                                m_aAjaxOnDemand,
                                                aWPEC.getRequestScope (),
                                                ESMPAccessPointColumn.values ());
    aDataTables.setPageLength (SMPCommonUI.PAGE_SIZE);
    aNodeList.addChild (aTable).addChild (aDataTables);
  }
}
