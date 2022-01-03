/*
 * Copyright (C) 2014-2022 Philip Helger and contributors
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
package com.helger.phoss.smp.status;

import javax.annotation.Nullable;

import com.helger.commons.annotation.IsSPIInterface;
import com.helger.commons.collection.impl.ICommonsOrderedMap;

/**
 * An SPI interface to be implemented by the real backends to add additional,
 * backend specific status data.
 *
 * @author Philip Helger
 * @since 5.4.0
 */
@IsSPIInterface
public interface ISMPStatusProviderExtensionSPI
{
  /**
   * @param bDisableLongRunningOperations
   *        an explicit parameter that can be used to disable long running
   *        operations which may be the case if the status API is used for
   *        health checking
   * @return An ordered map with additional status data elements. May be
   *         <code>null</code> or empty.
   */
  @Nullable
  ICommonsOrderedMap <String, ?> getAdditionalStatusData (boolean bDisableLongRunningOperations);
}
