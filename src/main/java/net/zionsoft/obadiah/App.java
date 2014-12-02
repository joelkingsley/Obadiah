/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2014 ZionSoft
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

import android.app.Application;

import com.crashlytics.android.Crashlytics;

import net.zionsoft.obadiah.legacy.Upgrader;
import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.ui.utils.UiHelper;

import io.fabric.sdk.android.Fabric;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics.Builder().disabled(BuildConfig.DEBUG).build());

        Analytics.initialize(this);
        DatabaseHelper.initialize(this);

        Upgrader.upgrade(this);

        Bible.initialize(this);
        Settings.initialize(this);

        UiHelper.forceActionBarOverflowMenu(this);
    }

    @Override
    public void onLowMemory() {
        Bible.getInstance().clearCache();

        super.onLowMemory();
    }
}
