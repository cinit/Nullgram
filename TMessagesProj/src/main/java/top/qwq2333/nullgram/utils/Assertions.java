/*
 * Copyright (C) 2019-2023 qwq233 <qwq233@qwq2333.top>
 * https://github.com/qwq233/Nullgram
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this software.
 *  If not, see
 * <https://www.gnu.org/licenses/>
 */

package top.qwq2333.nullgram.utils;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import kotlin.jvm.functions.Function0;

public class Assertions {

    private Assertions() {
    }

    public static void check(boolean condition) {
        check(condition, (String) null);
    }

    public static void check(boolean condition, @Nullable String msg) {
        if (!condition) {
            if (TextUtils.isEmpty(msg)) {
                msg = "check failed";
            }
            throw new AssertionError(msg);
        }
    }

    public static void check(boolean condition, @NonNull Function0<String> expr) {
        if (!condition) {
            String msg = expr.invoke();
            throw new AssertionError(msg);
        }
    }

}
