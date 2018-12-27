package org.indin.blisslaunchero.core.database.converters;


import android.arch.persistence.room.TypeConverter;

public class CharSequenceConverter {

    @TypeConverter
    public static String toString(CharSequence value) {
        return value.toString();
    }

    @TypeConverter
    public static CharSequence toCharSequence(String value){
        return value;
    }
}
