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
package com.helger.phoss.smp.rest;

import com.helger.photon.security.lock.SingleRunLock;

/**
 * The process wide locks ensuring, that only a single bulk change of a kind runs at a time. They
 * are shared between the UI and the REST API.
 *
 * @author Philip Helger
 * @since 8.6.3
 */
public final class SMPBulkChangeLocks
{
  public static final SingleRunLock ENDPOINT_URL = new SingleRunLock ("Bulk change of the endpoint URL");
  public static final SingleRunLock ENDPOINT_TECHNICAL_CONTACT_URL = new SingleRunLock ("Bulk change of the endpoint technical contact URL");
  public static final SingleRunLock ENDPOINT_SERVICE_VALIDITY = new SingleRunLock ("Bulk change of the endpoint service validity");
  public static final SingleRunLock ENDPOINT_CERTIFICATE = new SingleRunLock ("Bulk change of the endpoint certificate");

  private SMPBulkChangeLocks ()
  {}
}
