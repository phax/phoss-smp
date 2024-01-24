/*
 * Copyright (C) 2015-2024 Philip Helger and contributors
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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.CommonsEnumMap;
import com.helger.commons.collection.impl.ICommonsMap;
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
    void onItem (@Nonnull EImportSummaryAction eAction, int nSuccessCount, int nErrorCount);
  }

  private final ICommonsMap <EImportSummaryAction, ImportSummaryItem> m_aMap = new CommonsEnumMap <> (EImportSummaryAction.class);

  public ImportSummary ()
  {}

  public void onSuccess (@Nonnull final EImportSummaryAction eAction)
  {
    ValueEnforcer.notNull (eAction, "Action");
    m_aMap.computeIfAbsent (eAction, k -> new ImportSummaryItem ()).incSuccess ();
  }

  public void onError (@Nonnull final EImportSummaryAction eAction)
  {
    ValueEnforcer.notNull (eAction, "Action");
    m_aMap.computeIfAbsent (eAction, k -> new ImportSummaryItem ()).incError ();
  }

  public void forEach (@Nonnull final ICallbackItem aCallback)
  {
    ValueEnforcer.notNull (aCallback, "Callback");
    for (final Map.Entry <EImportSummaryAction, ImportSummaryItem> eItem : m_aMap.entrySet ())
      aCallback.onItem (eItem.getKey (), eItem.getValue ().getSuccessCount (), eItem.getValue ().getErrorCount ());
  }

  public void appendTo (@Nonnull final IMicroElement aElement)
  {
    ValueEnforcer.notNull (aElement, "Element");
    for (final Map.Entry <EImportSummaryAction, ImportSummaryItem> eItem : m_aMap.entrySet ())
      aElement.appendElement ("action")
              .setAttribute ("id", eItem.getKey ().getID ())
              .setAttribute ("success", eItem.getValue ().getSuccessCount ())
              .setAttribute ("error", eItem.getValue ().getErrorCount ());
  }

  public void appendTo (@Nonnull final IJsonObject aJson)
  {
    ValueEnforcer.notNull (aJson, "JsonObject");
    final IJsonArray aActions = new JsonArray ();
    for (final Map.Entry <EImportSummaryAction, ImportSummaryItem> eItem : m_aMap.entrySet ())
      aActions.add (new JsonObject ().add ("id", eItem.getKey ().getID ())
                                     .add ("success", eItem.getValue ().getSuccessCount ())
                                     .add ("error", eItem.getValue ().getErrorCount ()));
    aJson.addJson ("actions", aActions);
  }
}
