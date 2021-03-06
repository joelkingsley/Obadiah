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

package net.zionsoft.obadiah.misc.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.zionsoft.obadiah.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingTitleDescriptionButton extends FrameLayout {
    @BindView(R.id.title_text_view)
    TextView titleTextView;

    @BindView(R.id.description_text_view)
    TextView descriptionTextView;

    public SettingTitleDescriptionButton(Context context) {
        super(context);
        init(context, null);
    }

    public SettingTitleDescriptionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SettingTitleDescriptionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SettingTitleDescriptionButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.item_height));
        inflate(context, R.layout.item_setting_title_description, this);
        ButterKnife.bind(this, this);

        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SettingTitleDescriptionButton);
            loadTextFromAttrs(titleTextView, a, R.styleable.SettingTitleDescriptionButton_settingTitleDescriptionButtonTitle);
            loadTextFromAttrs(descriptionTextView, a, R.styleable.SettingTitleDescriptionButton_settingTitleDescriptionButtonDescription);
            a.recycle();
        }
    }

    private static void loadTextFromAttrs(TextView textView, TypedArray attrs, int index) {
        final int textResourceId = attrs.getResourceId(index, -1);
        if (textResourceId != -1) {
            textView.setText(textResourceId);
        } else {
            final String textString = attrs.getString(index);
            if (!TextUtils.isEmpty(textString)) {
                textView.setText(textString);
            }
        }
    }

    public void setDescriptionText(@StringRes int text) {
        descriptionTextView.setText(text);
    }

    public void setDescriptionText(CharSequence text) {
        descriptionTextView.setText(text);
    }

    public void setTitleTextColor(int color) {
        titleTextView.setTextColor(color);
    }

    public void setTextSize(int unit, float titleTextSize, float descriptionTextSize) {
        titleTextView.setTextSize(unit, titleTextSize);
        descriptionTextView.setTextSize(unit, descriptionTextSize);
    }
}
