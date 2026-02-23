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
package com.helger.phoss.smp.domain.sgprops;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.annotation.style.MustImplementEqualsAndHashcode;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.state.EChange;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsIterable;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.json.IHasJson;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;

/**
 * This represents a managed list of {@link SGCustomProperty}. The order is undefined but the name
 * uniqueness is verified. Names are case sensitive.
 * 
 * @author Philip Helger
 * @since 8.1.0
 */
@NotThreadSafe
@MustImplementEqualsAndHashcode
public class SGCustomPropertyList implements IHasJson, ICommonsIterable <SGCustomProperty>
{
  private final ICommonsOrderedMap <String, SGCustomProperty> m_aList = new CommonsLinkedHashMap <> ();

  public SGCustomPropertyList ()
  {}

  public SGCustomPropertyList (@Nullable final Iterable <SGCustomProperty> aProperties)
  {
    if (aProperties != null)
      for (final var aProp : aProperties)
        add (aProp);
  }

  public SGCustomPropertyList (@Nullable final SGCustomProperty @Nullable... aProperties)
  {
    if (aProperties != null)
      for (final var aProp : aProperties)
        add (aProp);
  }

  @NonNull
  public EChange add (@NonNull final SGCustomProperty aCustomProperty)
  {
    ValueEnforcer.notNull (aCustomProperty, "CustomProperty");

    final String sName = aCustomProperty.getName ();
    if (m_aList.containsKey (sName))
      return EChange.UNCHANGED;
    m_aList.put (sName, aCustomProperty);
    return EChange.CHANGED;
  }

  public boolean containsName (@Nullable final String sName)
  {
    return sName != null && m_aList.containsKey (sName);
  }

  @NonNull
  public EChange remove (@Nullable final String sName)
  {
    if (!SGCustomProperty.isValidName (sName))
      return EChange.UNCHANGED;
    return EChange.valueOf (m_aList.remove (sName) != null);
  }

  public void forEach (@NonNull final Consumer <? super SGCustomProperty> aConsumer)
  {
    ValueEnforcer.notNull (aConsumer, "Consumer");
    m_aList.forEachValue (aConsumer);
  }

  public void forEach (@Nullable final Predicate <? super SGCustomProperty> aFilter,
                       @NonNull final Consumer <? super SGCustomProperty> aConsumer)
  {
    ValueEnforcer.notNull (aConsumer, "Consumer");
    m_aList.forEachValue (aFilter, aConsumer);
  }

  @Nonnegative
  public int size ()
  {
    return m_aList.size ();
  }

  public boolean isEmpty ()
  {
    return m_aList.isEmpty ();
  }

  public boolean isNotEmpty ()
  {
    return m_aList.isNotEmpty ();
  }

  @NonNull
  public IJsonArray getAsJson ()
  {
    return new JsonArray ().addAllMapped (m_aList.values (), SGCustomProperty::getAsJson);
  }

  @NonNull
  public Iterator <SGCustomProperty> iterator ()
  {
    return m_aList.values ().iterator ();
  }

  @NonNull
  @ReturnsMutableCopy
  public SGCustomPropertyList getFiltered (@NonNull final Predicate <? super SGCustomProperty> aFilter)
  {
    return new SGCustomPropertyList (m_aList.copyOfValues (aFilter));
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (!getClass ().equals (o.getClass ()))
      return false;
    final SGCustomPropertyList rhs = (SGCustomPropertyList) o;
    return m_aList.equals (rhs.m_aList);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aList).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("List", m_aList).getToString ();
  }

  @NonNull
  @ReturnsMutableObject
  public static SGCustomPropertyList fromJson (@NonNull final IJsonArray aJson)
  {
    final SGCustomPropertyList ret = new SGCustomPropertyList ();
    for (final IJsonObject aObj : aJson.iteratorObjects ())
      ret.add (SGCustomProperty.fromJson (aObj));
    return ret;
  }
}
