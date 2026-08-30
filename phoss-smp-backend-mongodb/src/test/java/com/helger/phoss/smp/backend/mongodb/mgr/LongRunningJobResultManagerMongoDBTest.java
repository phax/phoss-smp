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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.base.id.factory.GlobalIDFactory;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.smp.backend.mongodb.SMPServerMongoDBTestRule;
import com.helger.photon.mgrs.longrun.ILongRunningJob;
import com.helger.photon.mgrs.longrun.LongRunningJobData;
import com.helger.photon.mgrs.longrun.LongRunningJobManager;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.text.IMultilingualText;
import com.helger.text.ReadOnlyMultilingualText;
import com.mongodb.client.model.Filters;

/**
 * Test class for class {@link LongRunningJobResultManagerMongoDB}
 *
 * @author Philip Helger
 */
public final class LongRunningJobResultManagerMongoDBTest
{
  private static final String TEST_TYPE_PREFIX = "unittest-type-";
  private static final String BSON_JOB_TYPE_FOR_TEST = "job_type";

  @Rule
  public final TestRule m_aRule = new SMPServerMongoDBTestRule ();

  /**
   * A minimum job implementation, only used to carry a job type into the manager.
   */
  private static final class MockJob implements ILongRunningJob
  {
    private final String m_sJobType;

    MockJob (@NonNull final String sJobType)
    {
      m_sJobType = sJobType;
    }

    @NonNull
    public String getJobType ()
    {
      return m_sJobType;
    }

    @NonNull
    public IMultilingualText getJobDescription ()
    {
      return new ReadOnlyMultilingualText (Locale.US, "Unit test job");
    }

    @NonNull
    public LongRunningJobResult createLongRunningJobResult ()
    {
      return LongRunningJobResult.createText ("done");
    }
  }

  @NonNull
  private static String _runJob (@NonNull final LongRunningJobManager aJobMgr, @NonNull final String sJobType)
  {
    final String sJobID = aJobMgr.onStartJob (new MockJob (sJobType), "testuser");
    aJobMgr.onEndJob (sJobID, ESuccess.SUCCESS, LongRunningJobResult.createText ("done"));
    return sJobID;
  }

  @Test
  public void testJobTypeFiltering ()
  {
    final LongRunningJobResultManagerMongoDB aResultMgr = new LongRunningJobResultManagerMongoDB ();
    final LongRunningJobManager aJobMgr = new LongRunningJobManager (aResultMgr);

    // Remove the leftovers of previously aborted runs
    aResultMgr.getCollection ().deleteMany (Filters.regex (BSON_JOB_TYPE_FOR_TEST, "^" + TEST_TYPE_PREFIX));

    // Unique per run, so that the assertions don't depend on the state of the test database
    final String sType1 = TEST_TYPE_PREFIX + GlobalIDFactory.getNewPersistentStringID ();
    final String sType2 = TEST_TYPE_PREFIX + GlobalIDFactory.getNewPersistentStringID ();

    final String sJobID1 = _runJob (aJobMgr, sType1);
    final String sJobID2 = _runJob (aJobMgr, sType2);

    // Simulate a job result that was written before the job type existed, by copying a stored
    // document without the job type field. Only the BSON field is removed - the serialized job data
    // is kept as it is, because the filtering is driven by the BSON field alone.
    final String sJobIDNoType = "unittest-legacy-" + sJobID1;
    final Document aLegacyDoc = aResultMgr.getCollection ().find (Filters.eq ("id", sJobID1)).first ();
    assertNotNull (aLegacyDoc);
    aLegacyDoc.remove ("_id");
    aLegacyDoc.remove ("job_type");
    aLegacyDoc.put ("id", sJobIDNoType);
    aResultMgr.getCollection ().insertOne (aLegacyDoc);

    try
    {
      // Without a filter everything is returned
      assertTrue (aResultMgr.getAllJobResults ().size () >= 3);

      // With a filter only the matching type is returned
      final ICommonsList <LongRunningJobData> aOfType1 = aResultMgr.getAllJobResults (sType1);
      assertEquals (1, aOfType1.size ());
      assertEquals (sJobID1, aOfType1.getFirstOrNull ().getID ());
      assertEquals (sType1, aOfType1.getFirstOrNull ().getJobType ());

      final ICommonsList <LongRunningJobData> aOfType2 = aResultMgr.getAllJobResults (sType2);
      assertEquals (1, aOfType2.size ());
      assertEquals (sJobID2, aOfType2.getFirstOrNull ().getID ());

      // The legacy document exists and is readable ...
      assertNotNull (aResultMgr.getJobResultOfID (sJobIDNoType));
      // ... but it does not match a non-null filter, because it has no job type field. If it did,
      // the assertion on the size of "aOfType1" above would have failed.

      // An unknown type matches nothing
      assertTrue (aResultMgr.getAllJobResults (TEST_TYPE_PREFIX + "does-not-exist").isEmpty ());

      // The job type survives the round trip
      final LongRunningJobData aRead = aResultMgr.getJobResultOfID (sJobID1);
      assertNotNull (aRead);
      assertEquals (sType1, aRead.getJobType ());
    }
    finally
    {
      aResultMgr.deleteResult (sJobID1);
      aResultMgr.deleteResult (sJobID2);
      aResultMgr.deleteResult (sJobIDNoType);
    }
  }
}
