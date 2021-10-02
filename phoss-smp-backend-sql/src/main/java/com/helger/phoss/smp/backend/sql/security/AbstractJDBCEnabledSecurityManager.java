package com.helger.phoss.smp.backend.sql.security;

import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.string.StringHelper;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.json.IJson;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonReader;
import com.helger.phoss.smp.backend.sql.mgr.AbstractJDBCEnabledManager;

/**
 * A special JDBC enabled manager with common methods for the security managers.
 *
 * @author Philip Helger
 */
public abstract class AbstractJDBCEnabledSecurityManager extends AbstractJDBCEnabledManager
{
  protected AbstractJDBCEnabledSecurityManager (@Nonnull final Supplier <? extends DBExecutor> aDBExecSupplier)
  {
    super (aDBExecSupplier);
  }

  @Nullable
  protected static final ICommonsOrderedMap <String, String> attrsToMap (@Nullable final String sAttrs)
  {
    if (StringHelper.hasNoText (sAttrs))
      return null;

    final IJsonObject aJson = JsonReader.builder ().source (sAttrs).readAsObject ();
    if (aJson == null)
      return null;
    final ICommonsOrderedMap <String, String> ret = new CommonsLinkedHashMap <> ();
    for (final Map.Entry <String, IJson> aEntry : aJson)
      ret.put (aEntry.getKey (), aEntry.getValue ().getAsValue ().getAsString ());
    return ret;
  }

  @Nullable
  protected static final String attrsToString (@Nullable final Map <String, String> aAttrs)
  {
    if (CollectionHelper.isEmpty (aAttrs))
      return null;
    return new JsonObject ().addAll (aAttrs).getAsJsonString ();
  }
}
