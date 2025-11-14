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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.function.Predicate;

import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smlclient.ManageServiceMetadataServiceCaller;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.phoss.smp.ui.secure.hc.HCSMLSelect;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.pages.BootstrapWebPageUIHandler;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.page.WebPageExecutionContext;

import jakarta.annotation.Nonnull;

public class PageSecureSMLRegDelete extends AbstractPageSecureSMLReg
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureSMLRegDelete.class);

  private static final String FIELD_SML_ID = "sml";

  public PageSecureSMLRegDelete (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Unregister from SML");
  }

  private void _deleteSMPfromSML (@Nonnull final WebPageExecutionContext aWPEC,
                                  @Nonnull final FormErrorList aFormErrors)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final String sSMLID = aWPEC.params ().getAsStringTrimmed (FIELD_SML_ID);
    final ISMLInfo aSMLInfo = SMPMetaManager.getSMLInfoMgr ().getSMLInfoOfID (sSMLID);

    if (aSMLInfo == null)
      aFormErrors.addFieldError (FIELD_SML_ID, "A valid SML must be selected!");

    if (aFormErrors.isEmpty ())
    {
      final String sSMPID = SMPServerConfiguration.getSMLSMPID ();
      try
      {
        final SSLSocketFactory aSocketFactory = SMPKeyManager.getInstance ().createSSLContext ().getSocketFactory ();
        final ManageServiceMetadataServiceCaller aCaller = createSMLCaller (aSMLInfo, aSocketFactory);
        aCaller.delete (sSMPID);

        final String sMsg = "Successfully deleted SMP '" +
                            sSMPID +
                            "' from the SML '" +
                            aSMLInfo.getManagementServiceURL () +
                            "'.";
        LOGGER.info (sMsg);
        aNodeList.addChild (success (sMsg));
        AuditHelper.onAuditExecuteSuccess ("smp-sml-delete", sSMPID, aSMLInfo.getManagementServiceURL ());
      }
      catch (final Exception ex)
      {
        final String sMsg = "Error deleting SMP '" +
                            sSMPID +
                            "' from the SML '" +
                            aSMLInfo.getManagementServiceURL () +
                            "'.";
        aNodeList.addChild (error (sMsg).addChild (SMPCommonUI.getTechnicalDetailsUI (ex)));
        AuditHelper.onAuditExecuteFailure ("smp-sml-delete",
                                           sSMPID,
                                           aSMLInfo.getManagementServiceURL (),
                                           ex.getClass (),
                                           ex.getMessage ());
      }
    }
    else
      aNodeList.addChild (BootstrapWebPageUIHandler.INSTANCE.createIncorrectInputBox (aWPEC));
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    if (!canShowPage (aWPEC))
      return;

    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final FormErrorList aFormErrors = new FormErrorList ();
    final boolean bShowInput = true;
    final ISMLInfo aDefaultSML = SMPMetaManager.getSettings ().getSMLInfo ();
    final String sSMPID = SMPServerConfiguration.getSMLSMPID ();

    if (SMPServerConfiguration.isHREdeliveryExtensionMode ())
    {
      aNodeList.addChild (warn (HR_EXT_WARNING));
    }

    if (aDefaultSML != null)
    {
      // Check if this SMP is already registered
      final String sPublisherDNSName = sSMPID + "." + aDefaultSML.getPublisherDNSZone ();
      try
      {
        final InetAddress aIA = InetAddress.getByName (sPublisherDNSName);
        aNodeList.addChild (success (div ("An SMP is already registered at the configured SML using the DNS name '" +
                                          sPublisherDNSName +
                                          "'. The determined IP address is " +
                                          aIA.getHostAddress ())).addChild (div ("Note: this can be a different machine than this one, if another SMP uses the same ID as this one (" +
                                                                                 sSMPID +
                                                                                 ")")));
      }
      catch (final UnknownHostException ex)
      {
        // continue
      }
    }

    if (aWPEC.hasAction (CPageParam.ACTION_PERFORM))
    {
      _deleteSMPfromSML (aWPEC, aFormErrors);
    }

    if (bShowInput)
    {
      final Predicate <ISMLInfo> aSMLFilter = ISMLInfo::isClientCertificateRequired;

      // Delete SMP from SML
      final BootstrapForm aForm = getUIHandler ().createFormSelf (aWPEC).setLeft (3, 3, 2, 2, 2);
      aForm.addChild (info ("Delete this SMP from the SML."));
      aForm.addChild (error ("This will remove ALL participants / Service Groups from the network! Your local Service Groups will become unreachable."));
      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("SML")
                                                   .setCtrl (new HCSMLSelect (new RequestField (FIELD_SML_ID,
                                                                                                aDefaultSML == null
                                                                                                                    ? null
                                                                                                                    : aDefaultSML.getID ()),
                                                                              aDisplayLocale,
                                                                              aSMLFilter))
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_SML_ID)));
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("SMP ID")
                                                   .setCtrl (em (sSMPID))
                                                   .setHelpText (HELPTEXT_SMP_ID));

      final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
      aToolbar.addHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM);
      aToolbar.addSubmitButton ("Delete SMP from SML");

      aNodeList.addChild (aForm);
    }
  }
}
