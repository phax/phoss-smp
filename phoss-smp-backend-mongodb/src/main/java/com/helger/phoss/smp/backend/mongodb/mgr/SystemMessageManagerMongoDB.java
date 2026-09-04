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
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.mgrs.sysmsg.ESystemMessageType;
import com.helger.photon.mgrs.sysmsg.ISystemMessageData;
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

  /**
   * Convert the read document to the system message data.
   *
   * @param aDoc
   *        The document read. May be <code>null</code>.
   * @return Never <code>null</code>. The default data, if no document is present.
   */
  @NonNull
  private static SystemMessageData _toData (@Nullable final Document aDoc)
  {
    if (aDoc == null)
      return new SystemMessageData ();

    final SystemMessageData ret = new SystemMessageData (ESystemMessageType.getFromIDOrDefault (aDoc.getString (BSON_MESSAGETYPE)),
                                                         aDoc.getString (BSON_MESSAGE));
    final Date aDate = aDoc.getDate (BSON_LASTUPDATE);
    if (aDate != null)
      ret.setLastUpdate (TypeConverter.convert (aDate, LocalDateTime.class));
    return ret;
  }

  @NonNull
  public ISystemMessageData getSystemMessageData ()
  {
    return _toData (_readDoc ());
  }

  @NonNull
  public EChange setSystemMessage (@NonNull final ESystemMessageType eMessageType, @Nullable final String sMessage)
  {
    ValueEnforcer.notNull (eMessageType, "MessageType");

    // Use SystemMessageData to check for actual change and compute new lastupdate
    final Document aExisting = _readDoc ();
    final SystemMessageData aData = _toData (aExisting);
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
