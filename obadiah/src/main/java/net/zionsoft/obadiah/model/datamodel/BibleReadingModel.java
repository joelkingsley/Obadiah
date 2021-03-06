/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2017 ZionSoft
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

package net.zionsoft.obadiah.model.datamodel;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.crash.Crash;
import net.zionsoft.obadiah.model.database.BookNamesTableHelper;
import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.model.database.TranslationHelper;
import net.zionsoft.obadiah.model.database.TranslationsTableHelper;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.model.domain.VerseIndex;
import net.zionsoft.obadiah.model.domain.VerseSearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Single;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

@Singleton
public class BibleReadingModel {
    @SuppressWarnings("WeakerAccess")
    final DatabaseHelper databaseHelper;
    private final SharedPreferences preferences;

    @SuppressWarnings("WeakerAccess")
    final LruCache<String, List<String>> bookNameCache
            = new LruCache<String, List<String>>((int) (Runtime.getRuntime().maxMemory() / 16L)) {
        @Override
        protected int sizeOf(String key, List<String> texts) {
            // strings are UTF-16 encoded (with a length of one or two 16-bit code units)
            int length = 0;
            for (String text : texts)
                length += text.length() * 4;
            return length;
        }
    };
    @SuppressWarnings("WeakerAccess")
    final LruCache<String, List<Verse>> verseCache
            = new LruCache<String, List<Verse>>((int) (Runtime.getRuntime().maxMemory() / 8L)) {
        @Override
        protected int sizeOf(String key, List<Verse> verses) {
            // each Verse contains 3 integers and 2 strings
            // strings are UTF-16 encoded (with a length of one or two 16-bit code units)
            int length = 0;
            for (Verse verse : verses)
                length += 12 + (verse.text.bookName.length() + verse.text.text.length()) * 4;
            return length;
        }
    };

    private final SerializedSubject<String, String> currentTranslationUpdatesSubject
            = PublishSubject.<String>create().toSerialized();
    private final SerializedSubject<Void, Void> parallelTranslationUpdatesSubject
            = PublishSubject.<Void>create().toSerialized();
    private final SerializedSubject<VerseIndex, VerseIndex> currentReadingProgressUpdatesSubject
            = PublishSubject.<VerseIndex>create().toSerialized();

    @SuppressWarnings("WeakerAccess")
    final List<String> parallelTranslations = new ArrayList<>();

    @Inject
    public BibleReadingModel(Context context, DatabaseHelper databaseHelper) {
        this.preferences = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        this.databaseHelper = databaseHelper;
    }

    @Nullable
    public String loadCurrentTranslation() {
        return preferences.getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null);
    }

    public void saveCurrentTranslation(String translation) {
        preferences.edit().putString(Constants.PREF_KEY_LAST_READ_TRANSLATION, translation).apply();
        removeParallelTranslation(translation);
        currentTranslationUpdatesSubject.onNext(translation);

        final Bundle params = new Bundle();
        params.putString(Analytics.PARAM_CONTENT_TYPE, "translation");
        params.putString(Analytics.PARAM_ITEM_ID, translation);
        Analytics.logEvent(Analytics.EVENT_SELECT_CONTENT, params);
    }

    public Observable<String> observeCurrentTranslation() {
        return currentTranslationUpdatesSubject.asObservable();
    }

    public boolean hasParallelTranslation() {
        return parallelTranslations.size() > 0;
    }

    public boolean isParallelTranslation(String translation) {
        return parallelTranslations.contains(translation);
    }

    public void addParallelTranslation(String translation) {
        if (!isParallelTranslation(translation) && !translation.equals(loadCurrentTranslation())) {
            parallelTranslations.add(translation);
            parallelTranslationUpdatesSubject.onNext(null);

            final Bundle params = new Bundle();
            params.putString(Analytics.PARAM_CONTENT_TYPE, "parallel_translation");
            params.putString(Analytics.PARAM_ITEM_ID, translation);
            Analytics.logEvent(Analytics.EVENT_SELECT_CONTENT, params);
        }
    }

    public void removeParallelTranslation(String translation) {
        if (parallelTranslations.remove(translation)) {
            parallelTranslationUpdatesSubject.onNext(null);
        }
    }

    public Observable<Void> observeParallelTranslation() {
        return parallelTranslationUpdatesSubject.asObservable();
    }

    public int loadCurrentBook() {
        return preferences.getInt(Constants.PREF_KEY_LAST_READ_BOOK, 0);
    }

    public int loadCurrentChapter() {
        return preferences.getInt(Constants.PREF_KEY_LAST_READ_CHAPTER, 0);
    }

    public int loadCurrentVerse() {
        return preferences.getInt(Constants.PREF_KEY_LAST_READ_VERSE, 0);
    }

    public void saveReadingProgress(VerseIndex index) {
        preferences.edit()
                .putInt(Constants.PREF_KEY_LAST_READ_BOOK, index.book())
                .putInt(Constants.PREF_KEY_LAST_READ_CHAPTER, index.chapter())
                .putInt(Constants.PREF_KEY_LAST_READ_VERSE, index.verse())
                .apply();
        currentReadingProgressUpdatesSubject.onNext(index);
    }

    public Observable<VerseIndex> observeCurrentReadingProgress() {
        return currentReadingProgressUpdatesSubject.asObservable();
    }

    public Single<List<String>> loadTranslations() {
        return Single.fromCallable(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                return TranslationsTableHelper.getDownloadedTranslations(databaseHelper.getDatabase());
            }
        });
    }

    public Observable<List<String>> loadBookNames(String translation) {
        return Observable.concat(loadBookNamesFromCache(translation),
                loadBookNamesFromDatabase(translation))
                .firstOrDefault(Collections.<String>emptyList(), new Func1<List<String>, Boolean>() {
                    @Override
                    public Boolean call(List<String> bookNames) {
                        return bookNames != null;
                    }
                });
    }

    private Observable<List<String>> loadBookNamesFromCache(String translation) {
        return Observable.just(TextUtils.isEmpty(translation) ? null : bookNameCache.get(translation));
    }

    private Observable<List<String>> loadBookNamesFromDatabase(final String translation) {
        return Observable.defer(new Func0<Observable<List<String>>>() {
            @Override
            public Observable<List<String>> call() {
                try {
                    return Observable.just(BookNamesTableHelper.getBookNames(
                            databaseHelper.getDatabase(), translation));
                } catch (Exception e) {
                    if (!TextUtils.isEmpty(translation)) {
                        // if the translation name is empty, it means there's current no translation
                        // installed, no need to track the exception
                        Crash.report(e);
                    }
                    return Observable.error(e);
                }
            }
        }).doOnNext(new Action1<List<String>>() {
            @Override
            public void call(List<String> bookNames) {
                bookNameCache.put(translation, bookNames);
            }
        });
    }

    @NonNull
    public Single<List<Verse>> loadVersesWithParallelTranslations(final int book, final int chapter) {
        return loadVerses(loadCurrentTranslation(), book, chapter)
                .map(new Func1<List<Verse>, List<Verse>>() {
                    @Override
                    public List<Verse> call(List<Verse> verses) {
                        final int parallelTranslationsCount = parallelTranslations.size();
                        final List<List<String>> textsFromParallelTranslations
                                = new ArrayList<>(parallelTranslationsCount);
                        final List<String> bookNamesFormParallelTranslations
                                = new ArrayList<>(parallelTranslationsCount);
                        final SQLiteDatabase database = databaseHelper.getDatabase();
                        for (int i = 0; i < parallelTranslationsCount; ++i) {
                            final String translation = parallelTranslations.get(i);
                            textsFromParallelTranslations.add(TranslationHelper.getVerseTexts(
                                    database, translation, book, chapter));
                            bookNamesFormParallelTranslations.add(loadBookNames(translation)
                                    .toBlocking().first().get(book));
                        }

                        final int versesCount = verses.size();
                        final List<Verse> results = new ArrayList<>(versesCount);
                        for (int i = 0; i < versesCount; ++i) {
                            final List<Verse.Text> texts = new ArrayList<>(parallelTranslationsCount);
                            final Verse verse = verses.get(i);

                            for (int j = 0; j < parallelTranslationsCount; ++j) {
                                // just in case the translation has less verses (probably an error?)
                                final List<String> t = textsFromParallelTranslations.get(j);
                                texts.add(new Verse.Text(parallelTranslations.get(j),
                                        bookNamesFormParallelTranslations.get(j),
                                        t.size() > i ? t.get(i) : ""));
                            }

                            results.add(new Verse(verse.verseIndex, verse.text, texts));
                        }
                        return results;
                    }
                });
    }

    @NonNull
    public Single<List<Verse>> loadVerses(String translation, int book, int chapter) {
        final List<Verse> fromCache = verseCache.get(buildVersesCacheKey(translation, book, chapter));
        if (fromCache != null && fromCache.size() > 0) {
            return Single.just(fromCache);
        }
        return loadVersesFromDatabase(translation, book, chapter);
    }

    private static final StringBuilder STRING_BUILDER = new StringBuilder(32);

    @SuppressWarnings("WeakerAccess")
    static String buildVersesCacheKey(String translation, int book, int chapter) {
        synchronized (STRING_BUILDER) {
            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append(translation).append('-').append(book).append('-').append(chapter);
            return STRING_BUILDER.toString();
        }
    }

    @NonNull
    private Single<List<Verse>> loadVersesFromDatabase(
            final String translation, final int book, final int chapter) {
        return loadBookNames(translation)
                .map(new Func1<List<String>, List<Verse>>() {
                    @Override
                    public List<Verse> call(List<String> bookNames) {
                        return Collections.unmodifiableList(TranslationHelper.getVerses(
                                databaseHelper.getDatabase(), translation, bookNames.get(book), book, chapter));
                    }
                }).doOnNext(new Action1<List<Verse>>() {
                    @Override
                    public void call(List<Verse> verses) {
                        verseCache.put(buildVersesCacheKey(translation, book, chapter), verses);
                    }
                }).toSingle();
    }

    @NonNull
    public Single<Verse> loadVerse(final String translation, final int book, final int chapter, final int verse) {
        return loadBookNames(translation)
                .map(new Func1<List<String>, Verse>() {
                    @Override
                    public Verse call(List<String> bookNames) {
                        return TranslationHelper.getVerse(databaseHelper.getDatabase(),
                                translation, bookNames.get(book), book, chapter, verse);
                    }
                }).toSingle();
    }

    public Observable<List<VerseSearchResult>> search(final String translation, final String query) {
        return loadBookNames(translation)
                .flatMap(new Func1<List<String>, Observable<List<VerseSearchResult>>>() {
                    @Override
                    public Observable<List<VerseSearchResult>> call(final List<String> bookNames) {
                        final Observable<List<VerseSearchResult>> first
                                = Observable.fromCallable(new Callable<List<VerseSearchResult>>() {
                            @Override
                            public List<VerseSearchResult> call() throws Exception {
                                return TranslationHelper.searchVerses(databaseHelper.getDatabase(),
                                        translation, bookNames, query, 0, 20);
                            }
                        });
                        final Observable<List<VerseSearchResult>> rest
                                = Observable.fromCallable(new Callable<List<VerseSearchResult>>() {
                            @Override
                            public List<VerseSearchResult> call() throws Exception {
                                return TranslationHelper.searchVerses(databaseHelper.getDatabase(),
                                        translation, bookNames, query, 20, 31102);
                            }
                        });
                        return Observable.concat(first, rest);
                    }
                });
    }

    public void clearCache() {
        bookNameCache.evictAll();
        verseCache.evictAll();
    }
}
