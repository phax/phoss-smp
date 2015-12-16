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
package com.helger.peppol.smpserver.jaxb;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.xml.bind.JAXBElement;

import com.helger.commons.io.resource.IReadableResource;
import com.helger.jaxb.AbstractJAXBMarshaller;
import com.helger.peppol.bdxr.ObjectFactory;
import com.helger.peppol.bdxr.SignedServiceMetadataType;

/**
 * A simple JAXB marshaller for the BDXR {@link SignedServiceMetadataType} type.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class MarshallerBDXRSignedServiceMetadataType extends AbstractJAXBMarshaller <SignedServiceMetadataType>
{
  public MarshallerBDXRSignedServiceMetadataType ()
  {
    super (SignedServiceMetadataType.class, (IReadableResource []) null);
  }

  @Override
  @Nonnull
  protected JAXBElement <SignedServiceMetadataType> wrapObject (@Nonnull final SignedServiceMetadataType aObject)
  {
    return new ObjectFactory ().createSignedServiceMetadata (aObject);
  }
}
