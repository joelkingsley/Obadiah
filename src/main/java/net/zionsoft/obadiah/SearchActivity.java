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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import net.zionsoft.obadiah.bible.TranslationInfo;
import net.zionsoft.obadiah.bible.TranslationManager;
import net.zionsoft.obadiah.bible.TranslationReader;
import net.zionsoft.obadiah.bible.TranslationsDatabaseHelper;
import net.zionsoft.obadiah.util.SettingsManager;

public class SearchActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);

        mSettingsManager = new SettingsManager(this);
        mTranslationsDatabaseHelper = new TranslationsDatabaseHelper(this);
        mTranslationManager = new TranslationManager(this);
        mTranslationReader = new TranslationReader(this);

        // initializes the search bar
        mSearchText = (EditText) findViewById(R.id.search_edittext);
        mSearchText.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    SearchActivity.this.search(null);
                    return true;
                }
                return false;
            }
        });

        // initializes the search results list view
        mSearchResultListView = (ListView) findViewById(R.id.search_result_listview);
        mSearchResultListAdapter = new SearchResultListAdapter(this);
        mSearchResultListView.setAdapter(mSearchResultListAdapter);
        mSearchResultListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= SearchActivity.this.mResults.length)
                    return;

                final SearchResult result = SearchActivity.this.mResults[position];
                final SharedPreferences.Editor editor = getSharedPreferences(Constants.SETTING_KEY, MODE_PRIVATE)
                        .edit();
                editor.putInt(Constants.CURRENT_BOOK_SETTING_KEY, result.bookIndex);
                editor.putInt(Constants.CURRENT_CHAPTER_SETTING_KEY, result.chapterIndex);
                editor.putInt(Constants.CURRENT_VERSE_SETTING_KEY, result.verseIndex);
                editor.commit();

                SearchActivity.this.startActivity(new Intent(SearchActivity.this, TextActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSettingsManager.refresh();
        final int backgroundColor = mSettingsManager.backgroundColor();
        mSearchResultListView.setBackgroundColor(backgroundColor);
        mSearchResultListView.setCacheColorHint(backgroundColor);
        mTextColor = mSettingsManager.textColor();

        mSearchResultListAdapter.notifyDataSetChanged();

        final String selectedTranslationShortName = getSharedPreferences(Constants.SETTING_KEY, MODE_PRIVATE)
                .getString(Constants.CURRENT_TRANSLATION_SETTING_KEY, null);
        if (selectedTranslationShortName == null
                || !selectedTranslationShortName.equals(mSelectedTranslationShortName)) {
            mTranslationReader.selectTranslation(selectedTranslationShortName);
            mSelectedTranslationShortName = mTranslationReader.selectedTranslationShortName();

            final TranslationInfo[] translations = mTranslationManager.translations();
            for (TranslationInfo translationInfo : translations) {
                if (translationInfo.installed && translationInfo.shortName.equals(mSelectedTranslationShortName)) {
                    setTitle(translationInfo.name);
                    break;
                }
            }

            search(null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_select_translation:
                startActivity(new Intent(SearchActivity.this, TranslationSelectionActivity.class));
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void search(View view) {
        final Editable searchToken = mSearchText.getText();
        if (searchToken.length() == 0)
            return;

        // TODO should the search functionality be moved to TranslationReader?
        new SearchAsyncTask().execute(searchToken);
    }

    private static class SearchResult {
        public int bookIndex;
        public int chapterIndex;
        public int verseIndex;
    }

    private class SearchAsyncTask extends AsyncTask<Editable, Void, Void> {
        protected void onPreExecute() {
            // running in the main thread

            SearchActivity.this.mSearchResultListAdapter.setTexts(null);

            InputMethodManager inputManager = (InputMethodManager) SearchActivity.this
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputManager != null) {
                inputManager.hideSoftInputFromWindow(SearchActivity.this.getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }

            mProgressDialog = new ProgressDialog(SearchActivity.this);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setMessage(SearchActivity.this.getText(R.string.text_searching));
            mProgressDialog.show();
        }

        protected Void doInBackground(Editable... params) {
            // running in the worker thread

            final SQLiteDatabase db = SearchActivity.this.mTranslationsDatabaseHelper.getReadableDatabase();
            final Cursor cursor = db.query(SearchActivity.this.mSelectedTranslationShortName, new String[]{
                    TranslationsDatabaseHelper.COLUMN_BOOK_INDEX, TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX,
                    TranslationsDatabaseHelper.COLUMN_VERSE_INDEX, TranslationsDatabaseHelper.COLUMN_TEXT},
                    String.format("%s LIKE ?", TranslationsDatabaseHelper.COLUMN_TEXT),
                    new String[]{String.format("%%%s%%", params[0].toString().trim().replaceAll("\\s+", "%"))},
                    null, null, null);
            if (cursor != null) {
                final int count = cursor.getCount();
                if (count > 0) {
                    final int bookIndexColumnIndex = cursor
                            .getColumnIndex(TranslationsDatabaseHelper.COLUMN_BOOK_INDEX);
                    final int chapterIndexColumnIndex = cursor
                            .getColumnIndex(TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX);
                    final int verseIndexColumnIndex = cursor
                            .getColumnIndex(TranslationsDatabaseHelper.COLUMN_VERSE_INDEX);
                    final int textColumnIndex = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_TEXT);
                    mTexts = new String[count];
                    SearchActivity.this.mResults = new SearchResult[count];
                    int i = 0;
                    while (cursor.moveToNext()) {
                        final SearchResult result = new SearchResult();
                        result.bookIndex = cursor.getInt(bookIndexColumnIndex);
                        result.chapterIndex = cursor.getInt(chapterIndexColumnIndex);
                        result.verseIndex = cursor.getInt(verseIndexColumnIndex);
                        SearchActivity.this.mResults[i] = result;

                        // format: <book name> <chapter index>:<verse index>\n<text>
                        mTexts[i++] = String.format("%s %d:%d\n%s",
                                SearchActivity.this.mTranslationReader.bookNames()[result.bookIndex],
                                result.chapterIndex + 1, result.verseIndex + 1,
                                cursor.getString(textColumnIndex));
                    }
                }
            }
            db.close();
            return null;
        }

        protected void onPostExecute(Void result) {
            // running in the main thread

            SearchActivity.this.mSearchResultListAdapter.setTexts(mTexts);
            mProgressDialog.dismiss();

            String text = SearchActivity.this.getResources().getString(R.string.text_search_result,
                    mTexts == null ? 0 : mTexts.length);
            Toast.makeText(SearchActivity.this, text, Toast.LENGTH_SHORT).show();
        }

        private ProgressDialog mProgressDialog;
        private String[] mTexts;
    }

    private class SearchResultListAdapter extends ListBaseAdapter {
        public SearchResultListAdapter(Context context) {
            super(context);
        }

        public void setTexts(String[] texts) {
            mTexts = texts;
            notifyDataSetChanged();
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView;
            if (convertView == null)
                textView = (TextView) View.inflate(mContext, R.layout.search_result_list_item, null);
            else
                textView = (TextView) convertView;

            textView.setTextColor(SearchActivity.this.mTextColor);
            textView.setText(mTexts[position]);
            return textView;
        }
    }

    private int mTextColor;
    private EditText mSearchText;
    private ListView mSearchResultListView;
    private SearchResult[] mResults;
    private SearchResultListAdapter mSearchResultListAdapter;
    private SettingsManager mSettingsManager;
    private String mSelectedTranslationShortName;
    private TranslationsDatabaseHelper mTranslationsDatabaseHelper;
    private TranslationManager mTranslationManager;
    private TranslationReader mTranslationReader;
}