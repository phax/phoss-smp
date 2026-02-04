/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.extension;

import org.jspecify.annotations.NonNull;

import com.helger.smpclient.extension.SMPExtensionList;

/**
 * Base interface for objects having an extension (service group, redirect,
 * endpoint, process and service metadata)
 *
 * @author Philip Helger
 */
public interface ISMPHasExtension
{
  @NonNull
  SMPExtensionList getExtensions ();
}
