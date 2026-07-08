package de.steffzilla.weighttracker.ui;

import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;

/**
 * A one-shot user-facing message. Either a plain string resource ({@link #plain}) or a
 * quantity-aware plurals resource ({@link #quantity}); the Activity resolves it against
 * the right {@code getString}/{@code getQuantityString} overload before showing it.
 */
public record BackupMessage(int resId, boolean isQuantity, int quantity, Object... args) {

    static BackupMessage plain(@StringRes int resId, Object... args) {
        return new BackupMessage(resId, false, 0, args);
    }

    static BackupMessage quantity(@PluralsRes int resId, int quantity, Object... args) {
        return new BackupMessage(resId, true, quantity, args);
    }
}