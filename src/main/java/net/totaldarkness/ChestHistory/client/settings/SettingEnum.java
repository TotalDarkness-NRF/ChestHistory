package net.totaldarkness.ChestHistory.client.settings;

import java.util.Objects;

public class SettingEnum<E extends Enum<E>> extends Setting<E> {

    private SettingEnum(final String name, final String description,final E defaultValue, final E value) {
        super(name, description, defaultValue, value);
    }

    E parseValue(final String value) {
        try {
            final E enumValue = getEnumFromString(Objects.requireNonNull(defaultValue).getDeclaringClass(), value);
            if (enumValue == null) return defaultValue;
            return enumValue;
        } catch (Exception ignored) {return defaultValue;}
    }

    private static <T extends Enum<T>> T getEnumFromString(Class<T> c, String string) {
        if(c != null && string != null) {
            try { return Enum.valueOf(c, string.trim().toUpperCase());
            } catch(IllegalArgumentException ignored) { }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> Enum<E> setOrdinal(final int direction, final E value) {
        int set = value.ordinal() + direction;
        final int length = value.getClass().getEnumConstants().length;
        if (set >= length) set = 0;
        else if (set <= 0) set = length - 1;
        return value.getClass().getEnumConstants()[set];
    }
}