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

package net.zionsoft.obadiah;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ListView;

import net.zionsoft.obadiah.bible.TranslationReader;
import net.zionsoft.obadiah.support.UpgradeService;
import net.zionsoft.obadiah.util.NetworkHelper;
import net.zionsoft.obadiah.util.SettingsManager;

public class BookSelectionActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bookselection_activity);

        if (needsUpgrade())
            upgrade();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mSettingsManager = new SettingsManager(this);
        mTranslationReader = new TranslationReader(this);

        // initializes views
        mLoadingSpinner = findViewById(R.id.book_selection_loading_spinner);
        mMainView = findViewById(R.id.book_selection_main_view);

        // initializes the book list view
        mBookListView = (ListView) findViewById(R.id.book_listview);
        mBookListAdapter = new BookListAdapter(this, mSettingsManager);
        mBookListView.setAdapter(mBookListAdapter);
        mBookListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mSelectedBook == position)
                    return;
                mSelectedBook = position;
                updateUi();
            }
        });

        // initializes the chapters list view
        mChaptersGridView = (GridView) findViewById(R.id.chapter_gridview);
        mChapterListAdapter = new ChapterListAdapter(this, mSettingsManager);
        mChaptersGridView.setAdapter(mChapterListAdapter);
        mChaptersGridView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mLastReadBook != mSelectedBook || mLastReadChapter != position) {
                    getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE).edit()
                            .putInt(Constants.PREF_KEY_LAST_READ_BOOK, mSelectedBook)
                            .putInt(Constants.PREF_KEY_LAST_READ_CHAPTER, position)
                            .putInt(Constants.PREF_KEY_LAST_READ_VERSE, 0)
                            .commit();
                }

                startActivity(new Intent(BookSelectionActivity.this, TextActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSettingsManager.refresh();
        final int backgroundColor = mSettingsManager.backgroundColor();
        mBookListView.setBackgroundColor(backgroundColor);
        mBookListView.setCacheColorHint(backgroundColor);
        mChaptersGridView.setBackgroundColor(backgroundColor);
        mChaptersGridView.setCacheColorHint(backgroundColor);

        if (isUpgrading())
            return;

        populateUi();
    }

    @Override
    protected void onDestroy() {
        unregisterUpgradeStatusListener();

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bookselection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                startActivity(new Intent(this, SearchActivity.class));
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_select_translation:
                startActivity(new Intent(this, TranslationSelectionActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    // upgrades from an older version

    private boolean needsUpgrade() {
        return getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE)
                .getInt(Constants.PREF_KEY_CURRENT_APPLICATION_VERSION, 0)
                < Constants.CURRENT_APPLICATION_VERSION;
    }

    private boolean isUpgrading() {
        return getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(Constants.PREF_KEY_UPGRADING, false);
    }

    private void upgrade() {
        if (NetworkHelper.hasNetworkConnection(this)) {
            registerUpgradeStatusListener();

            if (!isUpgrading()) {
                getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE).edit()
                        .putBoolean(Constants.PREF_KEY_UPGRADING, true).commit();
                startService(new Intent(this, UpgradeService.class));
            }
        } else {
            showErrorDialog(R.string.dialog_no_network_message,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            upgrade();
                        }
                    });
        }
    }

    private void registerUpgradeStatusListener() {
        if (mUpgradeStatusListener != null)
            return;

        mUpgradeStatusListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                unregisterUpgradeStatusListener();

                if (intent.getBooleanExtra(UpgradeService.KEY_RESULT_UPGRADE_SUCCESS, true)) {
                    populateUi();
                } else {
                    showErrorDialog(R.string.dialog_initialization_failure_message,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    upgrade();
                                }
                            });
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mUpgradeStatusListener,
                new IntentFilter(UpgradeService.ACTION_STATUS_UPDATE));
    }

    private void unregisterUpgradeStatusListener() {
        if (mUpgradeStatusListener == null)
            return;

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mUpgradeStatusListener);
        mUpgradeStatusListener = null;
    }


    // UI related

    private void showErrorDialog(int message, DialogInterface.OnClickListener onPositive) {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setNegativeButton(android.R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                .setPositiveButton(android.R.string.yes, onPositive)
                .setMessage(message)
                .create().show();
    }

    private void populateUi() {
        String lastReadTranslation = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE)
                .getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null);
        if (lastReadTranslation == null) {
            // no translation installed
            showErrorDialog(R.string.dialog_no_translation_message,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startActivity(new Intent(BookSelectionActivity.this,
                                    TranslationSelectionActivity.class));
                        }
                    });
            return;
        }

        if (lastReadTranslation.equals(mLastReadTranslation))
            return;

        mLastReadTranslation = lastReadTranslation;
        new AsyncTask<Void, Void, String[]>() {
            @Override
            protected void onPreExecute() {
                mLoadingSpinner.setVisibility(View.VISIBLE);
                mMainView.setVisibility(View.GONE);
            }

            @Override
            protected String[] doInBackground(Void... params) {
                // loads last read translation, book, and chapter
                final SharedPreferences preferences
                        = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
                mTranslationReader.selectTranslation(preferences
                        .getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null));
                mLastReadBook = preferences.getInt(Constants.PREF_KEY_LAST_READ_BOOK, -1);
                mLastReadChapter = preferences.getInt(Constants.PREF_KEY_LAST_READ_CHAPTER, -1);

                mSelectedBook = mLastReadBook < 0 ? 0 : mLastReadBook;

                return mTranslationReader.bookNames();
            }

            @Override
            protected void onPostExecute(String[] bookNames) {
                Animator.fadeOut(mLoadingSpinner);
                Animator.fadeIn(mMainView);

                // updates book list adapter and chapter list adapter
                mBookListAdapter.setTexts(bookNames);
                mChapterListAdapter.setLastReadChapter(mLastReadBook, mLastReadChapter);

                updateUi();

                // scrolls to the currently selected book
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
                    mBookListView.smoothScrollToPosition(mSelectedBook);
                else
                    mBookListView.setSelection(mSelectedBook);
            }
        }.execute();
    }

    private void updateUi() {
        // format: <translation short name> - <book name>
        setTitle(String.format("%s - %s", mTranslationReader.selectedTranslationShortName(),
                mTranslationReader.bookNames()[mSelectedBook]));

        mBookListAdapter.selectBook(mSelectedBook);
        mChapterListAdapter.selectBook(mSelectedBook);
        mChaptersGridView.setSelection(0);
    }

    private BroadcastReceiver mUpgradeStatusListener;

    private String mLastReadTranslation;
    private int mLastReadBook;
    private int mLastReadChapter;
    private int mSelectedBook = -1;

    private BookListAdapter mBookListAdapter;
    private ChapterListAdapter mChapterListAdapter;

    private GridView mChaptersGridView;
    private ListView mBookListView;
    private View mLoadingSpinner;
    private View mMainView;

    private SettingsManager mSettingsManager;
    private TranslationReader mTranslationReader;
}
