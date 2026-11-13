package com.aaliya.codeverdict.gamification;

import java.util.List;

/** Intern -> Junior Dev -> Developer -> Senior Dev -> Staff Engineer -> Principal. */
public final class Levels {

    public record Level(int index, String name, int minXp) {
    }

    public static final List<Level> ALL = List.of(
            new Level(0, "Intern", 0),
            new Level(1, "Junior Dev", 150),
            new Level(2, "Developer", 500),
            new Level(3, "Senior Dev", 1200),
            new Level(4, "Staff Engineer", 2500),
            new Level(5, "Principal", 5000));

    private Levels() {
    }

    public static Level first() {
        return ALL.get(0);
    }

    public static Level forXp(int xp) {
        Level result = ALL.get(0);
        for (Level l : ALL) {
            if (xp >= l.minXp()) {
                result = l;
            }
        }
        return result;
    }

    /** Next level, or null at the top. */
    public static Level next(Level current) {
        return current.index() + 1 < ALL.size() ? ALL.get(current.index() + 1) : null;
    }
}
