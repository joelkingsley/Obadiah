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

package net.zionsoft.obadiah;

import net.zionsoft.obadiah.biblereading.BibleReadingComponent;
import net.zionsoft.obadiah.biblereading.BibleReadingModule;
import net.zionsoft.obadiah.bookmarks.BookmarksComponent;
import net.zionsoft.obadiah.bookmarks.BookmarksModule;
import net.zionsoft.obadiah.misc.license.OpenSourceLicenseComponent;
import net.zionsoft.obadiah.misc.license.OpenSourceLicenseModule;
import net.zionsoft.obadiah.misc.settings.SettingsComponent;
import net.zionsoft.obadiah.misc.settings.SettingsModule;
import net.zionsoft.obadiah.notes.NotesComponent;
import net.zionsoft.obadiah.notes.NotesModule;
import net.zionsoft.obadiah.notification.PushNotificationReceiver;
import net.zionsoft.obadiah.readingprogress.ReadingProgressComponent;
import net.zionsoft.obadiah.readingprogress.ReadingProgressModule;
import net.zionsoft.obadiah.search.SearchComponent;
import net.zionsoft.obadiah.search.SearchModule;
import net.zionsoft.obadiah.translations.TranslationManagementComponent;
import net.zionsoft.obadiah.translations.TranslationManagementModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = BaseAppModule.class)
public interface AppComponent {
    void inject(App app);

    void inject(PushNotificationReceiver pushNotificationReceiver);

    BibleReadingComponent plus(BibleReadingModule bibleReadingModule);

    BookmarksComponent plus(BookmarksModule bookmarksModule);

    NotesComponent plus(NotesModule notesModule);

    OpenSourceLicenseComponent plus(OpenSourceLicenseModule openSourceLicenseModule);

    ReadingProgressComponent plus(ReadingProgressModule readingProgressModule);

    SearchComponent plus(SearchModule searchModule);

    SettingsComponent plus(SettingsModule settingsModule);

    TranslationManagementComponent plus(TranslationManagementModule translationManagementModule);

    final class Initializer {
        public static AppComponent init(App app) {
            return DaggerAppComponent.builder()
                    .baseAppModule(new AppModule(app))
                    .build();
        }
    }
}
