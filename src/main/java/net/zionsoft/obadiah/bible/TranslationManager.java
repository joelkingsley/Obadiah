/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2013 ZionSoft
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.zionsoft.obadiah.bible;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class TranslationManager {
    public TranslationManager(Context context) {
        super();
        mTranslationsDatabaseHelper = new TranslationsDatabaseHelper(context);
    }

    public void addTranslations(List<TranslationInfo> translations) {
        if (translations == null || translations.size() == 0)
            return;

        final TranslationInfo[] existingTranslations = translations();
        List<TranslationInfo> newTranslations;
        if (existingTranslations.length == 0) {
            newTranslations = translations;
        } else {
            newTranslations = new ArrayList<TranslationInfo>(translations.size());
            for (TranslationInfo translation : translations) {
                boolean newTranslation = true;
                for (TranslationInfo existing : existingTranslations) {
                    if (translation.shortName.equals(existing.shortName)) {
                        newTranslation = false;
                        break;
                    }
                }
                if (newTranslation)
                    newTranslations.add(translation);
            }
        }
        if (newTranslations.size() == 0)
            return;

        final ContentValues values = new ContentValues(5);
        final SQLiteDatabase db = mTranslationsDatabaseHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (TranslationInfo translationInfo : newTranslations) {
                values.put(TranslationsDatabaseHelper.COLUMN_INSTALLED, 0);
                values.put(TranslationsDatabaseHelper.COLUMN_TRANSLATION_NAME,
                        translationInfo.name);
                values.put(TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME,
                        translationInfo.shortName);
                values.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, translationInfo.language);
                values.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, translationInfo.size);
                db.insert(TranslationsDatabaseHelper.TABLE_TRANSLATIONS, null, values);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void removeTranslation(String translationShortName) {
        if (translationShortName == null)
            throw new IllegalArgumentException();

        final SQLiteDatabase db = mTranslationsDatabaseHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // deletes the translation table
            db.execSQL(String.format("DROP TABLE IF EXISTS %s", translationShortName));

            // deletes the book names
            db.delete(TranslationsDatabaseHelper.TABLE_BOOK_NAMES,
                    String.format("%s = ?", TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME),
                    new String[]{translationShortName});

            // sets the translation as "not installed"
            final ContentValues values = new ContentValues(1);
            values.put(TranslationsDatabaseHelper.COLUMN_INSTALLED, 0);
            db.update(TranslationsDatabaseHelper.TABLE_TRANSLATIONS, values,
                    String.format("%s = ?", TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME),
                    new String[]{translationShortName});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public TranslationInfo[] translations() {
        final SQLiteDatabase db = mTranslationsDatabaseHelper.getReadableDatabase();
        final String[] columns = new String[]{TranslationsDatabaseHelper.COLUMN_TRANSLATION_NAME,
                TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME,
                TranslationsDatabaseHelper.COLUMN_LANGUAGE,
                TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE,
                TranslationsDatabaseHelper.COLUMN_INSTALLED};
        final Cursor cursor = db.query(TranslationsDatabaseHelper.TABLE_TRANSLATIONS,
                columns, null, null, null, null, null);
        if (cursor != null) {
            final int count = cursor.getCount();
            if (count > 0) {
                final int languageColumnIndex
                        = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_LANGUAGE);
                final int translationNameColumnIndex = cursor
                        .getColumnIndex(TranslationsDatabaseHelper.COLUMN_TRANSLATION_NAME);
                final int translationShortNameColumnIndex = cursor
                        .getColumnIndex(TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME);
                final int downloadSizeColumnIndex = cursor
                        .getColumnIndex(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE);
                final int installedColumnIndex
                        = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_INSTALLED);
                final TranslationInfo[] translations = new TranslationInfo[count];
                int i = 0;
                while (cursor.moveToNext()) {
                    final TranslationInfo translationinfo = new TranslationInfo();
                    translationinfo.installed = (cursor.getInt(installedColumnIndex) == 1);
                    translationinfo.size = cursor.getInt(downloadSizeColumnIndex);
                    translationinfo.shortName = cursor.getString(translationShortNameColumnIndex);
                    translationinfo.name = cursor.getString(translationNameColumnIndex);
                    translationinfo.language = cursor.getString(languageColumnIndex);
                    translations[i++] = translationinfo;
                }
                db.close();
                return translations;
            }
        }
        db.close();
        return null;
    }

    private final TranslationsDatabaseHelper mTranslationsDatabaseHelper;
}
