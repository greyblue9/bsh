package com.strobel.core;

import com.strobel.componentmodel.UserDataStore;
import com.strobel.componentmodel.Key;
import com.strobel.annotations.NotNull;
import com.strobel.annotations.Nullable;
import com.strobel.componentmodel.UserDataStoreBase;


public class Freezable implements IFreezable, UserDataStore {
  
  protected UserDataStore _userData;
  private boolean _isFrozen;
  
  @Override
  public <T> void putUserData(
    @NotNull final Key<T> key, @Nullable final T value)
  {
    verifyNotFrozen();
    if (this._userData == null) {
      this._userData = new UserDataStoreBase();
    }
    this._userData.putUserData(key, value);
  }
  
  @Override
  public <T> T putUserDataIfAbsent(
    @NotNull final Key<T> key, @Nullable final T value)
  {
    verifyNotFrozen();
    if (this._userData == null) {
      this._userData = new UserDataStoreBase();
    }
    return this._userData.putUserDataIfAbsent(key, value);
  }
  
  @Override
  public <T> T getUserData(@NotNull final Key<T> key) {
    if (this._userData == null) {
      return null;
    }
    return this._userData.getUserData(key);
  }
  
  @Override
  public <T> boolean replace(
    @NotNull final Key<T> key,
    @Nullable final T oldValue, @Nullable final T newValue)
  {
    verifyNotFrozen();
    if (this._userData == null) {
      this._userData = new UserDataStoreBase();
    }
    return this._userData.replace(key, oldValue, newValue);
  }
  

  @Override
  public boolean canFreeze() {
    return !isFrozen();
  }
  
  @Override
  public final boolean isFrozen() {
    return _isFrozen;
  }
  
  @Override
  public final void freeze()
    throws IllegalStateException {
    if (!canFreeze()) {
      throw new IllegalStateException(
        "Object cannot be frozen.  Be sure to check canFreeze() before calling " +
        "freeze(), or use the tryFreeze() method instead."
      );
    }
    freezeCore();
    _isFrozen = true;
  }
  
  protected void freezeCore() {
  }

  protected final void verifyNotFrozen() {
    if (isFrozen()) {
      throw new IllegalStateException("Frozen object cannot be modified.");
    }
  }

  protected final void verifyFrozen() {
    if (!isFrozen()) {
      throw new IllegalStateException(
        "Object must be frozen before performing this operation."
      );
    }
  }

  @Override
  public final boolean tryFreeze() {
    if (!canFreeze()) {
      return false;
    }
    try {
      freeze();
      return true;
    } catch (final Throwable t) {
      return false;
    }
  }

  @Override
  public final void freezeIfUnfrozen() throws IllegalStateException {
    if (isFrozen()) return;
    freeze();
  }
  
  
}



   