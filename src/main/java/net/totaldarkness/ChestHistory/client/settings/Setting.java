package net.totaldarkness.ChestHistory.client.settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static net.totaldarkness.ChestHistory.client.util.Helper.getFileManager;
import static net.totaldarkness.ChestHistory.client.util.Helper.printInform;

@SuppressWarnings("unchecked")
public class Setting<E> implements Comparable<Setting<E>> {
    public final static ArrayList<Setting<?>> list = new ArrayList<>();
    private final String name, description;
    final E defaultValue;
    private E value;

    public static <E> Setting<E> build(final String name, final String description, final E defaultValue, final E value) {
        Setting<E> setting = new Setting<>(name, description, defaultValue, value == null ? defaultValue:value);
        list.add(setting);
        System.out.println("Building setting: " + name);
        return setting;
    }

    public static <E> Setting<E> build(final String name, final String description, final E defaultValue) {
        return build(name, description, defaultValue, (E) new Setting<>().readSetting(name));
    }

    Setting(final String name, final String description,final E defaultValue, final E value) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = value;
    }

    private Setting() { name = description = null; defaultValue = null;}

    public E get() {
        return value;
    }

    public Integer getAsInteger() {
        return (Integer) value;
    }

    public Long getAsLong() {
        return (Long) value;
    }

    public Float getAsFloat() {
        return (Float) value;
    }

    public Double getAsDouble() {
        return (Double) value;
    }

    public Boolean getAsBoolean() {
        return (Boolean) value;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public E getDefault() {
        return defaultValue;
    }

    public void set(final Object value, final boolean showMsg) {
        if (showMsg) printInform(String.format("Set %s from %s to %s", name, this.value, value));
        this.value = writeSetting(name, (E) value);
    }

    public void set(final Object value) {
        set(value, true);
    }

    public void reset(final boolean showMsg) {
        set(defaultValue, showMsg);
    }

    public void reset() {
        reset(true);
    }

    public static Setting<?> getSetting(final String name) {
        for (Setting<?> setting: list) {
            if (setting.getName().equals(name)) return setting;
        }
        return null;
    }

    E readSetting(final String name) {
        try {
            final String line = getLineWithName(name);
            return parseValue(line.substring(line.indexOf(':')));
        } catch (Exception ignored) {return writeSetting(name, defaultValue);}
    }

    E writeSetting(final String name, final E value) {
        final Path path = getFileManager().getMkBaseResolve("config/config.cfg");
        // TODO not writing settings
        try {
            if (!path.toFile().exists()) Files.write(path, (name + ':' + value).getBytes(), StandardOpenOption.WRITE);
            final List<String> lines = Files.readAllLines(path);
            lines.set(lines.indexOf(getLineWithName(name)), name + ':' + value);
            StringBuilder string = new StringBuilder();
            lines.forEach(line -> string.append(line).append("\n")); // TODO creates empty lines
            Files.write(path, string.toString().getBytes(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {}
        return value;
    }

    String getLineWithName(final String name) throws IOException {
        for (final String line: Files.readAllLines(getFileManager().getMkBaseResolve("config/config.cfg")))
            if (line.startsWith(name)) return line;
        return null;
    }

    E parseValue(final String value) {
        if (defaultValue instanceof Boolean) return (E) Boolean.valueOf(value);
        else { // TODO is it worth doing numbers?
            return defaultValue;
        }
    }

    public int compareTo(Setting<E> compare) {
        return String.CASE_INSENSITIVE_ORDER.compare(getName(), compare.getName());
    }
}