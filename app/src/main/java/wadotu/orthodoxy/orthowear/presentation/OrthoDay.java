package wadotu.orthodoxy.orthowear.presentation;

import java.util.List;

public class OrthoDay {
    public int year;
    public int month;
    public int day;
    public String weekday;
    public String[] titles;
    public String[] feasts;
    public int fast_level;
    public String fast_level_desc;
    public String fast_exception_desc;
    public List<Reading> readings;
    public List<Story> stories;
    public int tone;
    public int feast_level;
    public String feast_level_description;

    public static class Reading {
        public String display;
        public String source;
        public String description;
        public List<Passage> passage;
    }

    public static class Passage {
        public String book;
        public int chapter;
        public int verse;
        public String content;
    }

    public static class Story {
        public String title;
        public String story;
    }
}
