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
package com.helger.peppol.smpserver.data.xml;

import java.io.File;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.callback.INonThrowingCallable;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.MicroDocument;
import com.helger.commons.microdom.serialize.MicroReader;
import com.helger.commons.microdom.serialize.MicroWriter;
import com.helger.commons.scope.IScope;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;
import com.helger.commons.state.SuccessWithValue;
import com.helger.peppol.smpserver.data.DataManagerFactory;
import com.helger.peppol.smpserver.data.xml.domain.SMPRedirectManager;
import com.helger.peppol.smpserver.data.xml.domain.SMPServiceGroupManager;
import com.helger.peppol.smpserver.data.xml.domain.SMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.app.io.WebFileIO;
import com.helger.photon.basic.mgr.PhotonBasicManager;

public final class MetaManager extends AbstractGlobalSingleton
{
  private static final String SMP_SERVICE_GROUP_XML = "smp-servicegroup.xml";
  private static final String SMP_REDIRECT_XML = "smp-redirect.xml";
  private static final String SMP_SERVICE_INFORMATION_XML = "smp-serviceinformation.xml";

  private static final Logger s_aLogger = LoggerFactory.getLogger (MetaManager.class);

  private ISMPServiceGroupManager m_aServiceGroupMgr;
  private ISMPRedirectManager m_aRedirectMgr;
  private ISMPServiceInformationManager m_aServiceInformationMgr;

  @Deprecated
  @UsedViaReflection
  public MetaManager ()
  {}

  @Override
  protected void onAfterInstantiation (@Nonnull final IScope aScope)
  {
    try
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

      // Service group manager must be the first one!
      m_aServiceGroupMgr = new SMPServiceGroupManager (SMP_SERVICE_GROUP_XML);
      m_aRedirectMgr = new SMPRedirectManager (SMP_REDIRECT_XML);
      m_aServiceInformationMgr = new SMPServiceInformationManager (SMP_SERVICE_INFORMATION_XML);
      DataManagerFactory.getInstance ();

      s_aLogger.info ("MetaManager was initialized");
    }
    catch (final DAOException ex)
    {
      throw new InitializationException ("Failed to init MetaManager", ex);
    }
  }

  @Nonnull
  public static MetaManager getInstance ()
  {
    return getGlobalSingleton (MetaManager.class);
  }

  @Nonnull
  public static ISMPServiceGroupManager getServiceGroupMgr ()
  {
    return getInstance ().m_aServiceGroupMgr;
  }

  @Nonnull
  public static ISMPRedirectManager getRedirectMgr ()
  {
    return getInstance ().m_aRedirectMgr;
  }

  @Nonnull
  public static ISMPServiceInformationManager getServiceInformationMgr ()
  {
    return getInstance ().m_aServiceInformationMgr;
  }
}
