package com.helger.phoss.smp.backend.mongodb.security;

import com.helger.annotation.Nonempty;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.smp.backend.mongodb.MongoClientSingleton;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.IUserManager;
import com.helger.photon.security.user.IUserModificationCallback;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

public class MongoUserManager implements IUserManager
{

  public static final String USER_COLLECTION_NAME = "users";

  private final MongoCollection <Document> m_users;

  public MongoUserManager ()
  {
    this.m_users = MongoClientSingleton.getInstance ().getCollection (USER_COLLECTION_NAME);
  }

  @Override
  public void createDefaultsForTest ()
  {

  }

  @Override
  public @NonNull CallbackList <IUserModificationCallback> userModificationCallbacks ()
  {
    return null;
  }

  @Override
  public @Nullable IUser createNewUser (@NonNull @Nonempty String sLoginName, @Nullable String sEmailAddress, @NonNull String sPlainTextPassword, @Nullable String sFirstName, @Nullable String sLastName, @Nullable String sDescription, @Nullable Locale aDesiredLocale, @Nullable Map <String, String> aCustomAttrs, boolean bDisabled)
  {
    return null;
  }

  @Override
  public @Nullable IUser createPredefinedUser (@NonNull @Nonempty String sID, @NonNull @Nonempty String sLoginName, @Nullable String sEmailAddress, @NonNull String sPlainTextPassword, @Nullable String sFirstName, @Nullable String sLastName, @Nullable String sDescription, @Nullable Locale aDesiredLocale, @Nullable Map <String, String> aCustomAttrs, boolean bDisabled)
  {
    return null;
  }

  @Override
  public @Nullable IUser getUserOfID (@Nullable String sUserID)
  {
    return null;
  }

  @Override
  public @Nullable IUser getUserOfLoginName (@Nullable String sLoginName)
  {
    return null;
  }

  @Override
  public @Nullable IUser getUserOfEmailAddress (@Nullable String sEmailAddress)
  {
    return null;
  }

  @Override
  public @Nullable IUser getUserOfEmailAddressIgnoreCase (@Nullable String sEmailAddress)
  {
    return null;
  }

  @Override
  public @NonNull ICommonsList <IUser> getAllActiveUsers ()
  {
    return null;
  }

  @Override
  public long getActiveUserCount ()
  {
    return 0;
  }

  @Override
  public @NonNull ICommonsList <IUser> getAllDisabledUsers ()
  {
    return null;
  }

  @Override
  public @NonNull ICommonsList <IUser> getAllNotDeletedUsers ()
  {
    return null;
  }

  @Override
  public @NonNull ICommonsList <IUser> getAllDeletedUsers ()
  {
    return null;
  }

  @Override
  public boolean containsAnyActiveUser ()
  {
    return false;
  }

  @Override
  public @NonNull EChange setUserData (@Nullable String sUserID, @NonNull @Nonempty String sNewLoginName, @Nullable String sNewEmailAddress, @Nullable String sNewFirstName, @Nullable String sNewLastName, @Nullable String sNewDescription, @Nullable Locale aNewDesiredLocale, @Nullable Map <String, String> aNewCustomAttrs, boolean bNewDisabled)
  {
    return null;
  }

  @Override
  public @NonNull EChange setUserPassword (@Nullable String sUserID, @NonNull String sNewPlainTextPassword)
  {
    return null;
  }

  @Override
  public @NonNull EChange updateUserLastLogin (@Nullable String sUserID)
  {
    return null;
  }

  @Override
  public @NonNull EChange updateUserLastFailedLogin (@Nullable String sUserID)
  {
    return null;
  }

  @Override
  public @NonNull EChange deleteUser (@Nullable String sUserID)
  {
    return null;
  }

  @Override
  public @NonNull EChange undeleteUser (@Nullable String sUserID)
  {
    return null;
  }

  @Override
  public @NonNull EChange disableUser (@Nullable String sUserID)
  {
    return null;
  }

  @Override
  public @NonNull EChange enableUser (@Nullable String sUserID)
  {
    return null;
  }

  @Override
  public @NonNull <T> ICommonsList <T> getNone ()
  {
    return null;
  }

  @Override
  public @NonNull ICommonsList <IUser> getAll ()
  {
    return null;
  }

  @Override
  public boolean containsWithID (@Nullable String sID)
  {
    return false;
  }

  @Override
  public boolean containsAllIDs (@Nullable Iterable <String> aIDs)
  {
    return false;
  }
}
