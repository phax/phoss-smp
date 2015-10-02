/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.smpserver.data.xml.mgr;

import java.io.File;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.callback.INonThrowingCallable;
import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.MicroDocument;
import com.helger.commons.microdom.serialize.MicroReader;
import com.helger.commons.microdom.serialize.MicroWriter;
import com.helger.commons.state.SuccessWithValue;
import com.helger.peppol.smpserver.domain.ISMPManagerProviderSPI;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.app.io.WebFileIO;
import com.helger.photon.basic.mgr.PhotonBasicManager;

@IsSPIImplementation
public final class XMLManagerProviderSPI implements ISMPManagerProviderSPI
{
  private static final String SMP_SERVICE_GROUP_XML = "smp-servicegroup.xml";
  private static final String SMP_REDIRECT_XML = "smp-redirect.xml";
  private static final String SMP_SERVICE_INFORMATION_XML = "smp-serviceinformation.xml";

  public XMLManagerProviderSPI ()
  {
    PhotonBasicManager.getSystemMigrationMgr ()
                      .performMigrationIfNecessary ("service-metadata-2-information",
                                                    new INonThrowingCallable <SuccessWithValue <String>> ()
                                                    {
                                                      public SuccessWithValue <String> call ()
                                                      {
                                                        final File aOldFile = WebFileIO.getDataIO ()
                                                                                       .getFile ("smp-servicemetadata.xml");
                                                        final IMicroDocument aOldDoc = MicroReader.readMicroXML (aOldFile);
                                                        if (aOldDoc != null)
                                                        {
                                                          final IMicroDocument aNewDoc = new MicroDocument ();
                                                          final IMicroElement eNewRoot = aNewDoc.appendElement ("serviceinformationlist");
                                                          for (final IMicroElement eOldMetadata : aOldDoc.getDocumentElement ()
                                                                                                         .getAllChildElements ("servicemetadata"))
                                                          {
                                                            final String sID = eOldMetadata.getAttributeValue ("id");
                                                            final String sSGID = eOldMetadata.getAttributeValue ("sgid");
                                                            final IMicroElement eOldSI = eOldMetadata.getFirstChildElement ("serviceinfo");
                                                            final IMicroElement eNewSI = eNewRoot.appendElement ("serviceinformation");
                                                            eNewSI.setAttribute ("id", sID);
                                                            eNewSI.setAttribute ("servicegroupid", sSGID);
                                                            for (final IMicroElement eChild : eOldSI.getAllChildElements ())
                                                              eNewSI.appendChild (eChild.detachFromParent ());
                                                          }
                                                          if (MicroWriter.writeToFile (aNewDoc,
                                                                                       WebFileIO.getDataIO ()
                                                                                                .getFile (SMP_SERVICE_INFORMATION_XML))
                                                                         .isFailure ())
                                                            return SuccessWithValue.createFailure ("Failed to write new file");
                                                          WebFileIO.getFileOpMgr ().deleteFile (aOldFile);
                                                        }
                                                        return SuccessWithValue.createSuccess (null);
                                                      }
                                                    });
  }

  @Nonnull
  public ISMPUserManager createUserMgr ()
  {
    return new XMLUserManager ();
  }

  @Nonnull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    try
    {
      return new XMLServiceGroupManager (SMP_SERVICE_GROUP_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  @Nonnull
  public ISMPRedirectManager createRedirectMgr ()
  {
    try
    {
      return new XMLRedirectManager (SMP_REDIRECT_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  @Nonnull
  public ISMPServiceInformationManager createServiceInformationMgr ()
  {
    try
    {
      return new XMLServiceInformationManager (SMP_SERVICE_INFORMATION_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }
}
