/**
 * Copyright (C) 2019-2021 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.sql.security;

import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.locale.LocaleHelper;
import com.helger.commons.mutable.MutableLong;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.wrapper.Wrapper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.object.BusinessObjectHelper;
import com.helger.photon.security.object.StubObject;
import com.helger.photon.security.password.GlobalPasswordSettings;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.IUserManager;
import com.helger.photon.security.user.IUserModificationCallback;
import com.helger.photon.security.user.User;
import com.helger.photon.security.user.UserManager;
import com.helger.security.password.hash.PasswordHash;
import com.helger.security.password.salt.PasswordSalt;

/**
 * Implementation of {@link IUserManager} for JDBC backends.
 *
 * @author Philip Helger
 */
public class UserManagerJDBC extends AbstractJDBCEnabledSecurityManager implements IUserManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (UserManagerJDBC.class);

  private final CallbackList <IUserModificationCallback> m_aCallbacks = new CallbackList <> ();

  public UserManagerJDBC (@Nonnull final Supplier <? extends DBExecutor> aDBExecSupplier)
  {
    super (aDBExecSupplier);
  }

  @Nonnull
  @ReturnsMutableCopy
  private ICommonsList <IUser> _getAllWhere (@Nullable final String sCondition,
                                             @Nullable final ConstantPreparedStatementDataProvider aDataProvider)
  {
    final ICommonsList <IUser> ret = new CommonsArrayList <> ();
    final ICommonsList <DBResultRow> aDBResult;
    String sSQL = "SELECT id, creationdt, creationuserid, lastmoddt, lastmoduserid, deletedt, deleteuserid, attrs," +
                  " loginname, email, pwalgo, pwsalt, pwhash, firstname, lastname, description, locale, lastlogindt, logincount, failedlogins, disabled" +
                  " FROM smp_secuser";
    if (StringHelper.hasText (sCondition))
    {
      // Condition present
      sSQL += " WHERE " + sCondition;
      if (aDataProvider != null)
        aDBResult = newExecutor ().queryAll (sSQL, aDataProvider);
      else
        aDBResult = newExecutor ().queryAll (sSQL);
    }
    else
    {
      // Simply all
      aDBResult = newExecutor ().queryAll (sSQL);
    }

    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
      {
        final StubObject aStub = new StubObject (aRow.getAsString (0),
                                                 aRow.getAsLocalDateTime (1),
                                                 aRow.getAsString (2),
                                                 aRow.getAsLocalDateTime (3),
                                                 aRow.getAsString (4),
                                                 aRow.getAsLocalDateTime (5),
                                                 aRow.getAsString (6),
                                                 attrsToMap (aRow.getAsString (7)));
        final String sPWAlgo = aRow.getAsString (10);
        final String sPWSalt = aRow.getAsString (11);
        final String sPWHash = aRow.getAsString (12);
        final PasswordHash aPasswordHash = new PasswordHash (sPWAlgo, PasswordSalt.createFromStringMaybe (sPWSalt), sPWHash);
        ret.add (new User (aStub,
                           aRow.getAsString (8),
                           aRow.getAsString (9),
                           aPasswordHash,
                           aRow.getAsString (13),
                           aRow.getAsString (14),
                           aRow.getAsString (15),
                           LocaleHelper.getLocaleFromString (aRow.getAsString (16)),
                           aRow.getAsLocalDateTime (17),
                           aRow.getAsInt (18, 0),
                           aRow.getAsInt (19, 0),
                           aRow.getAsBoolean (20, false)));
      }
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IUser> getAll ()
  {
    return _getAllWhere (null, null);
  }

  public boolean containsWithID (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return false;

    return newExecutor ().queryCount ("SELECT COUNT(*) FROM smp_secuser WHERE id=?", new ConstantPreparedStatementDataProvider (sID)) > 0;
  }

  public boolean containsAllIDs (@Nullable final Iterable <String> aIDs)
  {
    if (aIDs != null)
    {
      // TODO could be optimized
      for (final String sID : aIDs)
        if (!containsWithID (sID))
          return false;
    }
    return true;
  }

  public void createDefaultsForTest ()
  {
    // Create Administrator
    if (!containsWithID (CSecurity.USER_ADMINISTRATOR_ID))
      _internalCreateItem (UserManager.createDefaultUserAdministrator ());

    // Create regular user
    if (!containsWithID (CSecurity.USER_USER_ID))
      _internalCreateItem (UserManager.createDefaultUserUser ());

    // Create guest user
    if (!containsWithID (CSecurity.USER_GUEST_ID))
      _internalCreateItem (UserManager.createDefaultUserGuest ());
  }

  @Nonnull
  @ReturnsMutableObject
  public final CallbackList <IUserModificationCallback> userModificationCallbacks ()
  {
    return m_aCallbacks;
  }

  @Nonnull
  private ESuccess _internalCreateItem (@Nonnull final User aUser)
  {
    final DBExecutor aExecutor = newExecutor ();
    return aExecutor.performInTransaction ( () -> {
      // Create new
      final long nCreated = aExecutor.insertOrUpdateOrDelete ("INSERT INTO smp_secuser (id, creationdt, creationuserid, lastmoddt, lastmoduserid, deletedt, deleteuserid, attrs," +
                                                              " loginname, email, pwalgo, pwsalt, pwhash, firstname, lastname, description, locale, lastlogindt, logincount, failedlogins, disabled)" +
                                                              " VALUES (?, ?, ?, ?, ?, ?, ?, ?," +
                                                              "" +
                                                              " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                                              new ConstantPreparedStatementDataProvider (getTrimmedToLength (aUser.getID (),
                                                                                                                             45),
                                                                                                         toTimestamp (aUser.getCreationDateTime ()),
                                                                                                         getTrimmedToLength (aUser.getCreationUserID (),
                                                                                                                             20),
                                                                                                         toTimestamp (aUser.getLastModificationDateTime ()),
                                                                                                         getTrimmedToLength (aUser.getLastModificationUserID (),
                                                                                                                             20),
                                                                                                         toTimestamp (aUser.getDeletionDateTime ()),
                                                                                                         getTrimmedToLength (aUser.getDeletionUserID (),
                                                                                                                             20),
                                                                                                         attrsToString (aUser.attrs ()),
                                                                                                         aUser.getLoginName (),
                                                                                                         aUser.getEmailAddress (),
                                                                                                         aUser.getPasswordHash ()
                                                                                                              .getAlgorithmName (),
                                                                                                         aUser.getPasswordHash ()
                                                                                                              .getSaltAsString (),
                                                                                                         aUser.getPasswordHash ()
                                                                                                              .getPasswordHashValue (),
                                                                                                         aUser.getFirstName (),
                                                                                                         aUser.getLastName (),
                                                                                                         aUser.getDescription (),
                                                                                                         aUser.getDesiredLocaleAsString (),
                                                                                                         toTimestamp (aUser.getLastLoginDateTime ()),
                                                                                                         Integer.valueOf (aUser.getLoginCount ()),
                                                                                                         Integer.valueOf (aUser.getConsecutiveFailedLoginCount ()),
                                                                                                         Boolean.valueOf (aUser.isDisabled ())));
      if (nCreated != 1)
        throw new IllegalStateException ("Failed to create new DB entry (" + nCreated + ")");
    });
  }

  @Nonnull
  private void _createNewUser (@Nonnull final User aUser, final boolean bPredefined)
  {
    // Store
    if (_internalCreateItem (aUser).isFailure ())
    {
      AuditHelper.onAuditCreateFailure (User.OT,
                                        aUser.getID (),
                                        aUser.getLoginName (),
                                        aUser.getEmailAddress (),
                                        aUser.getFirstName (),
                                        aUser.getLastName (),
                                        aUser.getDescription (),
                                        aUser.getDesiredLocale (),
                                        aUser.attrs (),
                                        Boolean.valueOf (aUser.isDisabled ()),
                                        bPredefined ? "predefined" : "custom",
                                        "database-error");
    }
    else
    {
      AuditHelper.onAuditCreateSuccess (User.OT,
                                        aUser.getID (),
                                        aUser.getLoginName (),
                                        aUser.getEmailAddress (),
                                        aUser.getFirstName (),
                                        aUser.getLastName (),
                                        aUser.getDescription (),
                                        aUser.getDesiredLocale (),
                                        aUser.attrs (),
                                        Boolean.valueOf (aUser.isDisabled ()),
                                        bPredefined ? "predefined" : "custom");

      // Execute callback as the very last action
      m_aCallbacks.forEach (aCB -> aCB.onUserCreated (aUser, bPredefined));
    }
  }

  @Nullable
  public IUser createNewUser (@Nonnull @Nonempty final String sLoginName,
                              @Nullable final String sEmailAddress,
                              @Nonnull final String sPlainTextPassword,
                              @Nullable final String sFirstName,
                              @Nullable final String sLastName,
                              @Nullable final String sDescription,
                              @Nullable final Locale aDesiredLocale,
                              @Nullable final Map <String, String> aCustomAttrs,
                              final boolean bDisabled)
  {
    ValueEnforcer.notEmpty (sLoginName, "LoginName");
    ValueEnforcer.notNull (sPlainTextPassword, "PlainTextPassword");

    if (getUserOfLoginName (sLoginName) != null)
    {
      // Another user with this login name already exists
      AuditHelper.onAuditCreateFailure (User.OT, "login-name-already-in-use", sLoginName);
      return null;
    }

    // Create user
    final User aUser = new User (sLoginName,
                                 sEmailAddress,
                                 GlobalPasswordSettings.createUserDefaultPasswordHash (new PasswordSalt (), sPlainTextPassword),
                                 sFirstName,
                                 sLastName,
                                 sDescription,
                                 aDesiredLocale,
                                 aCustomAttrs,
                                 bDisabled);
    _createNewUser (aUser, false);
    return aUser;
  }

  @Nullable
  public IUser createPredefinedUser (@Nonnull @Nonempty final String sID,
                                     @Nonnull @Nonempty final String sLoginName,
                                     @Nullable final String sEmailAddress,
                                     @Nonnull final String sPlainTextPassword,
                                     @Nullable final String sFirstName,
                                     @Nullable final String sLastName,
                                     @Nullable final String sDescription,
                                     @Nullable final Locale aDesiredLocale,
                                     @Nullable final Map <String, String> aCustomAttrs,
                                     final boolean bDisabled)
  {
    ValueEnforcer.notEmpty (sLoginName, "LoginName");
    ValueEnforcer.notNull (sPlainTextPassword, "PlainTextPassword");

    if (getUserOfLoginName (sLoginName) != null)
    {
      // Another user with this login name already exists
      AuditHelper.onAuditCreateFailure (User.OT, "login-name-already-in-use", sLoginName, "predefined-user");
      return null;
    }

    // Create user
    final User aUser = User.createdPredefinedUser (sID,
                                                   sLoginName,
                                                   sEmailAddress,
                                                   GlobalPasswordSettings.createUserDefaultPasswordHash (new PasswordSalt (),
                                                                                                         sPlainTextPassword),
                                                   sFirstName,
                                                   sLastName,
                                                   sDescription,
                                                   aDesiredLocale,
                                                   aCustomAttrs,
                                                   bDisabled);
    _createNewUser (aUser, true);
    return aUser;
  }

  @Nullable
  public IUser getUserOfID (@Nullable final String sUserID)
  {
    if (StringHelper.hasNoText (sUserID))
      return null;

    final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
    newExecutor ().querySingle ("SELECT creationdt, creationuserid, lastmoddt, lastmoduserid, deletedt, deleteuserid, attrs," +
                                " loginname, email, pwalgo, pwsalt, pwhash, firstname, lastname, description, locale, lastlogindt, logincount, failedlogins, disabled" +
                                " FROM smp_secuser" +
                                " WHERE id=?",
                                new ConstantPreparedStatementDataProvider (sUserID),
                                aDBResult::set);
    if (aDBResult.isNotSet ())
      return null;

    final DBResultRow aRow = aDBResult.get ();
    final StubObject aStub = new StubObject (sUserID,
                                             aRow.getAsLocalDateTime (0),
                                             aRow.getAsString (1),
                                             aRow.getAsLocalDateTime (2),
                                             aRow.getAsString (3),
                                             aRow.getAsLocalDateTime (4),
                                             aRow.getAsString (5),
                                             attrsToMap (aRow.getAsString (6)));
    final String sPWAlgo = aRow.getAsString (9);
    final String sPWSalt = aRow.getAsString (10);
    final String sPWHash = aRow.getAsString (11);
    final PasswordHash aPasswordHash = new PasswordHash (sPWAlgo, PasswordSalt.createFromStringMaybe (sPWSalt), sPWHash);
    return new User (aStub,
                     aRow.getAsString (7),
                     aRow.getAsString (8),
                     aPasswordHash,
                     aRow.getAsString (12),
                     aRow.getAsString (13),
                     aRow.getAsString (14),
                     LocaleHelper.getLocaleFromString (aRow.getAsString (15)),
                     aRow.getAsLocalDateTime (16),
                     aRow.getAsInt (17, 0),
                     aRow.getAsInt (18, 0),
                     aRow.getAsBoolean (19, false));
  }

  @Nullable
  public IUser getUserOfLoginName (@Nullable final String sLoginName)
  {
    if (StringHelper.hasNoText (sLoginName))
      return null;

    final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
    newExecutor ().querySingle ("SELECT id, creationdt, creationuserid, lastmoddt, lastmoduserid, deletedt, deleteuserid, attrs," +
                                " email, pwalgo, pwsalt, pwhash, firstname, lastname, description, locale, lastlogindt, logincount, failedlogins, disabled" +
                                " FROM smp_secuser" +
                                " WHERE loginname=?",
                                new ConstantPreparedStatementDataProvider (sLoginName),
                                aDBResult::set);
    if (aDBResult.isNotSet ())
      return null;

    final DBResultRow aRow = aDBResult.get ();
    final StubObject aStub = new StubObject (aRow.getAsString (0),
                                             aRow.getAsLocalDateTime (1),
                                             aRow.getAsString (2),
                                             aRow.getAsLocalDateTime (3),
                                             aRow.getAsString (4),
                                             aRow.getAsLocalDateTime (5),
                                             aRow.getAsString (6),
                                             attrsToMap (aRow.getAsString (7)));
    final String sPWAlgo = aRow.getAsString (9);
    final String sPWSalt = aRow.getAsString (10);
    final String sPWHash = aRow.getAsString (11);
    final PasswordHash aPasswordHash = new PasswordHash (sPWAlgo, PasswordSalt.createFromStringMaybe (sPWSalt), sPWHash);
    return new User (aStub,
                     sLoginName,
                     aRow.getAsString (8),
                     aPasswordHash,
                     aRow.getAsString (12),
                     aRow.getAsString (13),
                     aRow.getAsString (14),
                     LocaleHelper.getLocaleFromString (aRow.getAsString (15)),
                     aRow.getAsLocalDateTime (16),
                     aRow.getAsInt (17, 0),
                     aRow.getAsInt (18, 0),
                     aRow.getAsBoolean (19, false));
  }

  @Nullable
  private IUser _getUserOfEmailAddress (@Nonnull @Nonempty final String sEmailAddress, final boolean bIgnoreCase)
  {
    final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
    newExecutor ().querySingle ("SELECT id, creationdt, creationuserid, lastmoddt, lastmoduserid, deletedt, deleteuserid, attrs," +
                                " loginname, pwalgo, pwsalt, pwhash, firstname, lastname, description, locale, lastlogindt, logincount, failedlogins, disabled" +
                                " FROM smp_secuser" +
                                " WHERE " +
                                (bIgnoreCase ? "UPPER(email)" : "email") +
                                "=?",
                                new ConstantPreparedStatementDataProvider (bIgnoreCase ? sEmailAddress.toUpperCase (Locale.ROOT)
                                                                                       : sEmailAddress),
                                aDBResult::set);
    if (aDBResult.isNotSet ())
      return null;

    final DBResultRow aRow = aDBResult.get ();
    final StubObject aStub = new StubObject (aRow.getAsString (0),
                                             aRow.getAsLocalDateTime (1),
                                             aRow.getAsString (2),
                                             aRow.getAsLocalDateTime (3),
                                             aRow.getAsString (4),
                                             aRow.getAsLocalDateTime (5),
                                             aRow.getAsString (6),
                                             attrsToMap (aRow.getAsString (7)));
    final String sPWAlgo = aRow.getAsString (9);
    final String sPWSalt = aRow.getAsString (10);
    final String sPWHash = aRow.getAsString (11);
    final PasswordHash aPasswordHash = new PasswordHash (sPWAlgo, PasswordSalt.createFromStringMaybe (sPWSalt), sPWHash);
    return new User (aStub,
                     aRow.getAsString (8),
                     sEmailAddress,
                     aPasswordHash,
                     aRow.getAsString (12),
                     aRow.getAsString (13),
                     aRow.getAsString (14),
                     LocaleHelper.getLocaleFromString (aRow.getAsString (15)),
                     aRow.getAsLocalDateTime (16),
                     aRow.getAsInt (17, 0),
                     aRow.getAsInt (18, 0),
                     aRow.getAsBoolean (19, false));
  }

  @Nullable
  public IUser getUserOfEmailAddress (@Nullable final String sEmailAddress)
  {
    if (StringHelper.hasNoText (sEmailAddress))
      return null;

    return _getUserOfEmailAddress (sEmailAddress, false);
  }

  @Nullable
  public IUser getUserOfEmailAddressIgnoreCase (@Nullable final String sEmailAddress)
  {
    if (StringHelper.hasNoText (sEmailAddress))
      return null;

    return _getUserOfEmailAddress (sEmailAddress, true);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IUser> getAllActiveUsers ()
  {
    return _getAllWhere ("deletedt IS NULL AND disabled=false", null);
  }

  @Nonnegative
  public long getActiveUserCount ()
  {
    return newExecutor ().queryCount ("SELECT COUNT(*) FROM smp_secuser WHERE deletedt IS NULL AND disabled=false");
  }

  public boolean containsAnyActiveUser ()
  {
    return getActiveUserCount () > 0;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IUser> getAllDisabledUsers ()
  {
    return _getAllWhere ("deletedt IS NULL AND disabled=true", null);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IUser> getAllNotDeletedUsers ()
  {
    return _getAllWhere ("deletedt IS NULL", null);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IUser> getAllDeletedUsers ()
  {
    return _getAllWhere ("deletedt IS NOT NULL", null);
  }

  @Nonnull
  public EChange setUserData (@Nullable final String sUserID,
                              @Nonnull @Nonempty final String sNewLoginName,
                              @Nullable final String sNewEmailAddress,
                              @Nullable final String sNewFirstName,
                              @Nullable final String sNewLastName,
                              @Nullable final String sNewDescription,
                              @Nullable final Locale aNewDesiredLocale,
                              @Nullable final Map <String, String> aNewCustomAttrs,
                              final boolean bNewDisabled)
  {
    if (StringHelper.hasNoText (sUserID))
      return EChange.UNCHANGED;

    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Update existing
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secuser" +
                                                              " SET loginname=?, email=?, firstname=?, lastname=?, description=?, locale=?, attrs=?, disabled=?, lastmoddt=?, lastmoduserid=?" +
                                                              " WHERE id=?",
                                                              new ConstantPreparedStatementDataProvider (sNewLoginName,
                                                                                                         sNewEmailAddress,
                                                                                                         sNewFirstName,
                                                                                                         sNewLastName,
                                                                                                         sNewDescription,
                                                                                                         aNewDesiredLocale == null ? null
                                                                                                                                   : aNewDesiredLocale.toString (),
                                                                                                         attrsToString (aNewCustomAttrs),
                                                                                                         Boolean.valueOf (bNewDisabled),
                                                                                                         toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                         getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                             20),
                                                                                                         sUserID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditModifyFailure (User.OT,
                                        "set-all",
                                        sUserID,
                                        sNewLoginName,
                                        sNewEmailAddress,
                                        sNewFirstName,
                                        sNewLastName,
                                        sNewDescription,
                                        aNewDesiredLocale,
                                        aNewCustomAttrs,
                                        Boolean.valueOf (bNewDisabled),
                                        "database-error");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such user ID
      AuditHelper.onAuditModifyFailure (User.OT, "set-all", sUserID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (User.OT,
                                      "set-all",
                                      sUserID,
                                      sNewLoginName,
                                      sNewEmailAddress,
                                      sNewFirstName,
                                      sNewLastName,
                                      sNewDescription,
                                      aNewDesiredLocale,
                                      aNewCustomAttrs,
                                      Boolean.valueOf (bNewDisabled));

    // Execute callback as the very last action
    m_aCallbacks.forEach (aCB -> aCB.onUserUpdated (sUserID));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange setUserPassword (@Nullable final String sUserID, @Nonnull final String sNewPlainTextPassword)
  {
    if (StringHelper.hasNoText (sUserID))
      return EChange.UNCHANGED;

    // Create a new password salt upon password change
    final PasswordHash aPasswordHash = GlobalPasswordSettings.createUserDefaultPasswordHash (new PasswordSalt (), sNewPlainTextPassword);

    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Update existing
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secuser" +
                                                              " SET pwalgo=?, pwsalt=?, pwhash=?, lastmoddt=?, lastmoduserid=?" +
                                                              " WHERE id=?",
                                                              new ConstantPreparedStatementDataProvider (aPasswordHash.getAlgorithmName (),
                                                                                                         aPasswordHash.getSaltAsString (),
                                                                                                         aPasswordHash.getPasswordHashValue (),
                                                                                                         toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                         getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                             20),
                                                                                                         sUserID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditModifyFailure (User.OT, "set-password", sUserID, "database-error");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such user ID
      AuditHelper.onAuditModifyFailure (User.OT, "set-password", sUserID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (User.OT, "set-password", sUserID);

    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("The password of user '" + sUserID + "' was changed");

    // Execute callback as the very last action
    m_aCallbacks.forEach (aCB -> aCB.onUserPasswordChanged (sUserID));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange updateUserLastLogin (@Nullable final String sUserID)
  {
    if (StringHelper.hasNoText (sUserID))
      return EChange.UNCHANGED;

    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Update existing
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secuser" +
                                                              " SET lastlogindt=?, logincount=logincount + 1, failedlogins=0" +
                                                              " WHERE id=?",
                                                              new ConstantPreparedStatementDataProvider (toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                         sUserID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditModifyFailure (User.OT, "update-last-login", sUserID, "database-error");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such user ID
      AuditHelper.onAuditModifyFailure (User.OT, "update-last-login", sUserID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (User.OT, "update-last-login", sUserID);
    return EChange.CHANGED;
  }

  @Nonnull
  public EChange updateUserLastFailedLogin (@Nullable final String sUserID)
  {
    if (StringHelper.hasNoText (sUserID))
      return EChange.UNCHANGED;

    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Update existing
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secuser SET failedlogins=failedlogins + 1 WHERE id=?",
                                                              new ConstantPreparedStatementDataProvider (sUserID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditModifyFailure (User.OT, "update-last-failed-login", sUserID, "database-error");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such user ID
      AuditHelper.onAuditModifyFailure (User.OT, "update-last-failed-login", sUserID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (User.OT, "set-last-failed-login", sUserID);

    // Execute callback as the very last action
    m_aCallbacks.forEach (aCB -> aCB.onUserLastFailedLoginUpdated (sUserID));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteUser (@Nullable final String sUserID)
  {
    if (StringHelper.hasNoText (sUserID))
      return EChange.UNCHANGED;

    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Update existing
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secuser SET deletedt=?, deleteuserid=? WHERE id=?",
                                                              new ConstantPreparedStatementDataProvider (toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                         getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                             20),
                                                                                                         sUserID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditDeleteFailure (User.OT, sUserID, "database-error");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such user ID
      AuditHelper.onAuditDeleteFailure (User.OT, sUserID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditDeleteSuccess (User.OT, sUserID);

    // Execute callback as the very last action
    m_aCallbacks.forEach (aCB -> aCB.onUserDeleted (sUserID));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange undeleteUser (@Nullable final String sUserID)
  {
    if (StringHelper.hasNoText (sUserID))
      return EChange.UNCHANGED;

    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Update existing
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secuser SET lastmoddt=?, lastmoduserid=?, deletedt=NULL, deleteuserid=NULL WHERE id=?",
                                                              new ConstantPreparedStatementDataProvider (toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                         getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                             20),
                                                                                                         sUserID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditUndeleteFailure (User.OT, sUserID, "database-error");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such user ID
      AuditHelper.onAuditUndeleteFailure (User.OT, sUserID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditUndeleteSuccess (User.OT, sUserID);

    // Execute callback as the very last action
    m_aCallbacks.forEach (aCB -> aCB.onUserUndeleted (sUserID));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange disableUser (@Nullable final String sUserID)
  {
    if (StringHelper.hasNoText (sUserID))
      return EChange.UNCHANGED;

    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Update existing
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secuser SET disabled=true, lastmoddt=?, lastmoduserid=? WHERE id=?",
                                                              new ConstantPreparedStatementDataProvider (toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                         getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                             20),
                                                                                                         sUserID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditModifyFailure (User.OT, "disable", sUserID, "database-error");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such user ID
      AuditHelper.onAuditModifyFailure (User.OT, "disable", sUserID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (User.OT, "disable", sUserID);

    // Execute callback as the very last action
    m_aCallbacks.forEach (aCB -> aCB.onUserEnabled (sUserID, false));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange enableUser (@Nullable final String sUserID)
  {
    if (StringHelper.hasNoText (sUserID))
      return EChange.UNCHANGED;

    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Update existing
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secuser SET disabled=false, lastmoddt=?, lastmoduserid=? WHERE id=?",
                                                              new ConstantPreparedStatementDataProvider (toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                         getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                             20),
                                                                                                         sUserID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditModifyFailure (User.OT, "enable", sUserID, "database-error");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such user ID
      AuditHelper.onAuditModifyFailure (User.OT, "enable", sUserID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (User.OT, "enable", sUserID);

    // Execute callback as the very last action
    m_aCallbacks.forEach (aCB -> aCB.onUserEnabled (sUserID, true));

    return EChange.CHANGED;
  }
}
