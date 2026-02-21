/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.mongodb.mgr;

import java.time.LocalDateTime;
import java.util.Date;

import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.mgrs.sysmsg.ESystemMessageType;
import com.helger.photon.mgrs.sysmsg.ISystemMessageManager;
import com.helger.photon.mgrs.sysmsg.SystemMessageData;
import com.helger.typeconvert.impl.TypeConverter;

/**
 * MongoDB based implementation of {@link ISystemMessageManager}. Stores a single document in the
 * collection (no PK); uses replaceOne or insertOne.
 *
 * @author Philip Helger
 * @since 8.0.16
 */
public class SystemMessageManagerMongoDB extends AbstractManagerMongoDB implements ISystemMessageManager
{
  private static final String BSON_MESSAGETYPE = "messagetype";
  private static final String BSON_LASTUPDATE = "lastupdate";
  private static final String BSON_MESSAGE = "message";

  public SystemMessageManagerMongoDB ()
  {
    super ("smp-sys-message");
  }

  @Nullable
  private Document _readDoc ()
  {
    return getCollection ().find ().first ();
  }

  @Nullable
  public LocalDateTime getLastUpdateDT ()
  {
    final Document aDoc = _readDoc ();
    if (aDoc == null)
      return null;
    final Date aDate = aDoc.getDate (BSON_LASTUPDATE);
    return aDate != null ? TypeConverter.convert (aDate, LocalDateTime.class) : null;
  }

  @NonNull
  public ESystemMessageType getMessageType ()
  {
    final Document aDoc = _readDoc ();
    if (aDoc != null)
      return ESystemMessageType.getFromIDOrDefault (aDoc.getString (BSON_MESSAGETYPE));
    return ESystemMessageType.DEFAULT;
  }

  @Nullable
  public String getSystemMessage ()
  {
    final Document aDoc = _readDoc ();
    return aDoc != null ? aDoc.getString (BSON_MESSAGE) : null;
  }

  public boolean hasSystemMessage ()
  {
    final Document aDoc = _readDoc ();
    if (aDoc == null)
      return false;
    return StringHelper.isNotEmpty (aDoc.getString (BSON_MESSAGE));
  }

  @NonNull
  public EChange setSystemMessage (@NonNull final ESystemMessageType eMessageType, @Nullable final String sMessage)
  {
    ValueEnforcer.notNull (eMessageType, "MessageType");

    // Use SystemMessageData to check for actual change and compute new lastupdate
    final Document aExisting = _readDoc ();
    final SystemMessageData aData;
    if (aExisting != null)
    {
      aData = new SystemMessageData (ESystemMessageType.getFromIDOrDefault (aExisting.getString (BSON_MESSAGETYPE)),
                                     aExisting.getString (BSON_MESSAGE));
      final Date aDate = aExisting.getDate (BSON_LASTUPDATE);
      if (aDate != null)
        aData.setLastUpdate (TypeConverter.convert (aDate, LocalDateTime.class));
    }
    else
    {
      aData = new SystemMessageData ();
    }

    if (aData.setSystemMessage (eMessageType, sMessage).isUnchanged ())
      return EChange.UNCHANGED;

    final LocalDateTime aLastUpdateDT = aData.getLastUpdateDT ();
    final Document aNewDoc = new Document ().append (BSON_MESSAGETYPE, eMessageType.getID ())
                                            .append (BSON_LASTUPDATE, TypeConverter.convert (aLastUpdateDT, Date.class))
                                            .append (BSON_MESSAGE, sMessage);

    if (aExisting != null)
    {
      if (!getCollection ().replaceOne (new Document (), aNewDoc).wasAcknowledged ())
        throw new IllegalStateException ("Failed to replace system message in MongoDB");
    }
    else
    {
      if (!getCollection ().insertOne (aNewDoc).wasAcknowledged ())
        throw new IllegalStateException ("Failed to insert system message into MongoDB");
    }

    AuditHelper.onAuditExecuteSuccess ("update-system-message", eMessageType, sMessage);
    return EChange.CHANGED;
  }
}
