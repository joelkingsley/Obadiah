/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2015 ZionSoft
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

package net.zionsoft.obadiah.mvp.presenters;

import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;

import net.zionsoft.obadiah.mvp.views.MVPView;

public abstract class MVPPresenter<V extends MVPView> {
    private V view;

    public final void takeView(V view) {
        this.view = view;
        onViewTaken();
    }

    @CallSuper
    protected void onViewTaken() {
    }

    public final void dropView() {
        this.view = null;
        onViewDropped();
    }

    @CallSuper
    protected void onViewDropped() {
    }

    @Nullable
    protected final V getView() {
        return view;
    }
}