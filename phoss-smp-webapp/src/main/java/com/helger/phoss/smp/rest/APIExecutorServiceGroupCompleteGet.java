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
package com.helger.phoss.smp.rest;

import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.mime.CMimeType;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.exception.SMPInternalErrorException;
import com.helger.phoss.smp.restapi.BDXR1ServerAPI;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPServerAPI;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.smpclient.bdxr1.marshal.BDXR1MarshallerCompleteServiceGroupType;
import com.helger.smpclient.peppol.marshal.SMPMarshallerCompleteServiceGroupType;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.write.XMLWriterSettings;

public final class APIExecutorServiceGroupCompleteGet extends AbstractSMPAPIExecutor
{
  @Override
  protected void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                            @NonNull @Nonempty final String sPath,
                            @NonNull final Map <String, String> aPathVariables,
                            @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                            @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sPathServiceGroupID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID));
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    final byte [] aBytes;
    switch (SMPServerConfiguration.getRESTType ())
    {
      case PEPPOL:
      {
        // Unspecified extension
        final var ret = new SMPServerAPI (aDataProvider).getCompleteServiceGroup (sPathServiceGroupID);
        aBytes = new SMPMarshallerCompleteServiceGroupType ().setUseSchema (XML_SCHEMA_VALIDATION).getAsBytes (ret);
        break;
      }
      case OASIS_BDXR_V1:
      {
        // Unspecified extension
        final var ret = new BDXR1ServerAPI (aDataProvider).getCompleteServiceGroup (sPathServiceGroupID);
        aBytes = new BDXR1MarshallerCompleteServiceGroupType ().setUseSchema (XML_SCHEMA_VALIDATION).getAsBytes (ret);
        break;
      }
      // Not supported with OASIS BDXR v2
      default:
        throw new UnsupportedOperationException ("Unsupported REST type specified!");
    }

    if (aBytes == null)
    {
      // Internal error serializing the payload
      throw new SMPInternalErrorException ("Failed to convert the returned CompleteServiceGroup to XML");
    }

    aUnifiedResponse.setContent (aBytes)
                    .setMimeType (CMimeType.TEXT_XML)
                    .setCharset (XMLWriterSettings.DEFAULT_XML_CHARSET_OBJ);
  }
}
