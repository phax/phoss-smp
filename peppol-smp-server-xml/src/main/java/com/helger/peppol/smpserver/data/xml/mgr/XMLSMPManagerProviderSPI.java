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
package com.helger.peppol.smpserver.data.xml.mgr;

import java.io.File;

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
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.app.io.WebFileIO;
import com.helger.photon.basic.mgr.PhotonBasicManager;

public final class XMLSMPManagerProviderSPI implements ISMPManagerProviderSPI
{
  private static final String SMP_SERVICE_GROUP_XML = "smp-servicegroup.xml";
  private static final String SMP_REDIRECT_XML = "smp-redirect.xml";
  private static final String SMP_SERVICE_INFORMATION_XML = "smp-serviceinformation.xml";

  public XMLSMPManagerProviderSPI ()
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

  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    try
    {
      return new SMPServiceGroupManager (SMP_SERVICE_GROUP_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  public ISMPRedirectManager createRedirectMgr ()
  {
    try
    {
      return new SMPRedirectManager (SMP_REDIRECT_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  public ISMPServiceInformationManager createServiceInformationMgr ()
  {
    try
    {
      return new SMPServiceInformationManager (SMP_SERVICE_INFORMATION_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }
}
