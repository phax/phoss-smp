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

import java.util.Date;

import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.photon.mgrs.longrun.ILongRunningJobResultManager;
import com.helger.photon.mgrs.longrun.LongRunningJobData;
import com.helger.photon.mgrs.longrun.LongRunningJobDataMicroTypeConverter;
import com.helger.typeconvert.impl.TypeConverter;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.serialize.MicroReader;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;

/**
 * MongoDB based implementation of {@link ILongRunningJobResultManager}. Serializes
 * {@link LongRunningJobData} as a full XML blob stored in the {@code job_data} field.
 *
 * @author Philip Helger
 */
public class LongRunningJobResultManagerMongoDB extends AbstractManagerMongoDB implements ILongRunningJobResultManager
{
  private static final String BSON_ID = "id";
  private static final String BSON_START_DT = "start_dt";
  private static final String BSON_JOB_DATA = "job_data";

  private static final String ELEMENT_JOB = "job";
  private static final LongRunningJobDataMicroTypeConverter CONVERTER = new LongRunningJobDataMicroTypeConverter ();

  public LongRunningJobResultManagerMongoDB ()
  {
    super ("smp-long-running-job");
    getCollection ().createIndex (Indexes.ascending (BSON_ID));
  }

  @NonNull
  private static String _serialize (@NonNull final LongRunningJobData aJobData)
  {
    final IMicroElement eJob = CONVERTER.convertToMicroElement (aJobData, null, ELEMENT_JOB);
    return MicroWriter.getNodeAsString (eJob);
  }

  @Nullable
  private static LongRunningJobData _deserialize (@Nullable final String sJobData)
  {
    if (sJobData == null)
      return null;
    final IMicroElement eJob = MicroReader.readMicroXML (sJobData).getDocumentElement ();
    return CONVERTER.convertToNative (eJob);
  }

  public void addResult (@NonNull final LongRunningJobData aJobData)
  {
    ValueEnforcer.notNull (aJobData, "JobData");
    if (!aJobData.isEnded ())
      throw new IllegalArgumentException ("Passed jobData is not yet finished");

    final Document aDoc = new Document ().append (BSON_ID, aJobData.getID ())
                                         .append (BSON_START_DT,
                                                  TypeConverter.convert (aJobData.getStartDateTime (), Date.class))
                                         .append (BSON_JOB_DATA, _serialize (aJobData));

    if (!getCollection ().insertOne (aDoc).wasAcknowledged ())
      throw new IllegalStateException ("Failed to insert long running job '" + aJobData.getID () + "'");
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <LongRunningJobData> getAllJobResults ()
  {
    final ICommonsList <LongRunningJobData> ret = new CommonsArrayList <> ();
    for (final Document aDoc : getCollection ().find ().sort (Sorts.ascending (BSON_START_DT)))
    {
      final LongRunningJobData aJobData = _deserialize (aDoc.getString (BSON_JOB_DATA));
      if (aJobData != null)
        ret.add (aJobData);
    }
    return ret;
  }

  @Nullable
  public LongRunningJobData getJobResultOfID (@Nullable final String sJobResultID)
  {
    if (sJobResultID == null)
      return null;
    final Document aDoc = getCollection ().find (Filters.eq (BSON_ID, sJobResultID)).first ();
    if (aDoc == null)
      return null;
    return _deserialize (aDoc.getString (BSON_JOB_DATA));
  }
}
