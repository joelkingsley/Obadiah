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

package net.zionsoft.obadiah.model;

public class Verse {
    public final int bookIndex;
    public final int chapterIndex;
    public final int verseIndex;
    public final String bookName;
    public final String verseText;

    public Verse(int bookIndex, int chapterIndex, int verseIndex, String bookName, String verseText) {
        super();

        this.bookIndex = bookIndex;
        this.chapterIndex = chapterIndex;
        this.verseIndex = verseIndex;
        this.bookName = bookName;
        this.verseText = verseText;
    }
}