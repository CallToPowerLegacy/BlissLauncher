package org.indin.blisslaunchero.framework.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Looper;
import android.os.UserHandle;
import android.support.v4.util.Preconditions;

import org.indin.blisslaunchero.framework.LauncherSettings;
import org.indin.blisslaunchero.framework.UserManagerCompat;
import org.indin.blisslaunchero.framework.Utilities;

/**
 * A wrapper around {@link ContentValues} with some utility methods.
 */
public class ContentWriter {

    private final ContentValues mValues;
    private final Context mContext;

    private CommitParams mCommitParams;
    private Bitmap mIcon;
    private UserHandle mUser;

    public ContentWriter(Context context, CommitParams commitParams) {
        this(context);
        mCommitParams = commitParams;
    }

    public ContentWriter(Context context) {
        this(new ContentValues(), context);
    }

    public ContentWriter(ContentValues values, Context context) {
        mValues = values;
        mContext = context;
    }

    public ContentWriter put(String key, Integer value) {
        mValues.put(key, value);
        return this;
    }

    public ContentWriter put(String key, Long value) {
        mValues.put(key, value);
        return this;
    }

    public ContentWriter put(String key, String value) {
        mValues.put(key, value);
        return this;
    }

    public ContentWriter put(String key, CharSequence value) {
        mValues.put(key, value == null ? null : value.toString());
        return this;
    }

    public ContentWriter put(String key, Intent value) {
        mValues.put(key, value == null ? null : value.toUri(0));
        return this;
    }

    public ContentWriter putIcon(Bitmap value, UserHandle user) {
        mIcon = value;
        mUser = user;
        return this;
    }

    public ContentWriter put(String key, UserHandle user) {
        return put(key, UserManagerCompat.getInstance(mContext).getSerialNumberForUser(user));
    }

    /**
     * Commits any pending validation and returns the final values.
     * Must not be called on UI thread.
     */
    public ContentValues getValues(Context context) {
        if(Looper.myLooper() != Looper.getMainLooper()){
            if (mIcon != null) {
                mValues.put(LauncherSettings.Favorites.ICON, Utilities.flattenBitmap(mIcon));
                mIcon = null;
            }
            return mValues;
        }else {
            throw new IllegalStateException();
        }
    }

    public int commit() {
        if (mCommitParams != null) {
            return mContext.getContentResolver().update(mCommitParams.mUri, getValues(mContext),
                    mCommitParams.mWhere, mCommitParams.mSelectionArgs);
        }
        return 0;
    }

    public static final class CommitParams {

        final Uri mUri = LauncherSettings.Favorites.CONTENT_URI;
        String mWhere;
        String[] mSelectionArgs;

        public CommitParams(String where, String[] selectionArgs) {
            mWhere = where;
            mSelectionArgs = selectionArgs;
        }

    }
}
