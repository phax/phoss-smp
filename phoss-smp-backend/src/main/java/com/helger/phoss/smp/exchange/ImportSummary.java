/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.exchange;

import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsEnumMap;
import com.helger.collection.commons.ICommonsMap;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.xml.microdom.IMicroElement;

@NotThreadSafe
public final class ImportSummary
{
  @FunctionalInterface
  public interface ICallbackItem
  {
    void onItem (@NonNull EImportSummaryAction eAction, int nSuccessCount, int nErrorCount);
  }

  private final ICommonsMap <EImportSummaryAction, ImportSummaryItem> m_aMap = new CommonsEnumMap <> (EImportSummaryAction.class);

  public ImportSummary ()
  {}

  public void onSuccess (@NonNull final EImportSummaryAction eAction)
  {
    ValueEnforcer.notNull (eAction, "Action");
    m_aMap.computeIfAbsent (eAction, k -> new ImportSummaryItem ()).incSuccess ();
  }

  public void onError (@NonNull final EImportSummaryAction eAction)
  {
    ValueEnforcer.notNull (eAction, "Action");
    m_aMap.computeIfAbsent (eAction, k -> new ImportSummaryItem ()).incError ();
  }

  public void forEach (@NonNull final ICallbackItem aCallback)
  {
    ValueEnforcer.notNull (aCallback, "Callback");
    for (final Map.Entry <EImportSummaryAction, ImportSummaryItem> eItem : m_aMap.entrySet ())
      aCallback.onItem (eItem.getKey (), eItem.getValue ().getSuccessCount (), eItem.getValue ().getErrorCount ());
  }

  public void appendTo (@NonNull final IMicroElement aElement)
  {
    ValueEnforcer.notNull (aElement, "Element");
    for (final Map.Entry <EImportSummaryAction, ImportSummaryItem> eItem : m_aMap.entrySet ())
      aElement.addElement ("action")
              .setAttribute ("id", eItem.getKey ().getID ())
              .setAttribute ("success", eItem.getValue ().getSuccessCount ())
              .setAttribute ("error", eItem.getValue ().getErrorCount ());
  }

  public void appendTo (@NonNull final IJsonObject aJson)
  {
    ValueEnforcer.notNull (aJson, "JsonObject");
    final IJsonArray aActions = new JsonArray ();
    for (final Map.Entry <EImportSummaryAction, ImportSummaryItem> eItem : m_aMap.entrySet ())
      aActions.add (new JsonObject ().add ("id", eItem.getKey ().getID ())
                                     .add ("success", eItem.getValue ().getSuccessCount ())
                                     .add ("error", eItem.getValue ().getErrorCount ()));
    aJson.add ("actions", aActions);
  }
}
