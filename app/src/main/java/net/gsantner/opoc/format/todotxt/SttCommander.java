/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0 / Commercial
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.format.todotxt;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Text = the whole document ;; line = one task line, \n separated
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "SameParameterValue"})
public class SttCommander {
    //
    // Statics
    //
    public static final Pattern TODOTXT_FILE_PATTERN = Pattern.compile("(?i)(^todo[-.]?.*)|(.*[-.]todo\\.((txt)|(text))$)");
    public static final SimpleDateFormat DATEF_YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    public static final int DATEF_YYYY_MM_DD_LEN = "yyyy-MM-dd".length();
    public static final String PT_DATE = "\\d{4}-\\d{2}-\\d{2}";

    public static final Pattern PATTERN_DESCRIPTION = Pattern.compile("(?:^|\\n)" +
            "(?:" +
            "(?:[Xx](?: " + PT_DATE + " " + PT_DATE + ")?)" + // Done in front
            "|(?:\\([A-Za-z]\\)(?: " + PT_DATE + ")?)" + // Priority in front
            "|(?:" + PT_DATE + ")" + // Creation date in front
            " )?" + // End of prefix
            "((a|.)*)" // Take whats left
    );
    public static final Pattern PATTERN_PROJECTS = Pattern.compile("\\B(?:\\++)(\\S+)");
    public static final Pattern PATTERN_CONTEXTS = Pattern.compile("\\B(?:\\@+)(\\S+)");
    public static final Pattern PATTERN_DONE = Pattern.compile("(?m)(^[Xx]) (.*)$");
    public static final Pattern PATTERN_DATE = Pattern.compile("(?:^|\\s|:)(" + PT_DATE + ")(?:$|\\s)");
    public static final Pattern PATTERN_KEY_VALUE_PAIRS__TAG_ONLY = Pattern.compile("(?i)([a-z]+):([a-z0-9_-]+)");
    public static final Pattern PATTERN_KEY_VALUE_PAIRS = Pattern.compile("(?i)((?:[a-z]+):(?:[a-z0-9_-]+))");
    public static final Pattern PATTERN_DUE_DATE = Pattern.compile("(?:due:)(" + PT_DATE + ")");
    public static final Pattern PATTERN_PRIORITY_ANY = Pattern.compile("(?:^|\\n)\\(([A-Za-z])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_A = Pattern.compile("(?:^|\\n)\\(([Aa])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_B = Pattern.compile("(?:^|\\n)\\(([Bb])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_C = Pattern.compile("(?:^|\\n)\\(([Cc])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_D = Pattern.compile("(?:^|\\n)\\(([Dd])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_E = Pattern.compile("(?:^|\\n)\\(([Ee])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_F = Pattern.compile("(?:^|\\n)\\(([Ff])\\)\\s");
    public static final Pattern PATTERN_COMPLETION_DATE = Pattern.compile("(?:^|\\n)(?:[Xx] )(" + PT_DATE + ")");
    public static final Pattern PATTERN_CREATION_DATE = Pattern.compile("(?:^|\\n)(?:\\([A-Za-z]\\)\\s)?(?:[Xx] " + PT_DATE + " )?(" + PT_DATE + ")");

    public static String getToday() {
        return DATEF_YYYY_MM_DD.format(new Date());
    }

    public static List<String> parseContexts(String text) {
        return parseAllUniqueMatchesWithOneValue(text, PATTERN_CONTEXTS);
    }

    public static List<String> parseProjects(String text) {
        return parseAllUniqueMatchesWithOneValue(text, PATTERN_PROJECTS);
    }

    private static boolean parseDone(String line) {
        return isPatternFindable(line, PATTERN_DONE);
    }

    private static String parseCompletionDate(String line) {
        return parseOneValueOrDefault(line, PATTERN_COMPLETION_DATE, "");
    }

    private static String parseCreationDate(String line) {
        return parseOneValueOrDefault(line, PATTERN_CREATION_DATE, "");
    }

    public static String parseDueDate(final String line, final String def) {
        return parseOneValueOrDefault(line, PATTERN_DUE_DATE, def);
    }

    private static Character parsePriority(String line) {
        String ret = parseOneValueOrDefault(line, PATTERN_PRIORITY_ANY, "");
        if (ret.length() == 1) {
            return ret.charAt(0);
        } else {
            return null;
        }
    }

    public static boolean isTodoFile(String filepath) {
        return filepath != null && SttCommander.TODOTXT_FILE_PATTERN.matcher(filepath).matches() && (filepath.endsWith(".txt") || filepath.endsWith(".text"));
    }

    // Only captures the first group of each match
    private static List<String> parseAllUniqueMatchesWithOneValue(String text, Pattern pattern) {
        List<String> ret = new ArrayList<>();
        for (Matcher m = pattern.matcher(text); m.find(); ) {
            if (m.groupCount() > 0) {
                String found = m.group(1);
                if (!ret.contains(found)) {
                    ret.add(found);
                }
            }
        }
        return ret;
    }

    private static String parseOneValueOrDefault(String text, Pattern pattern, String defaultValue) {
        for (Matcher m = pattern.matcher(text); m.find(); ) {
            // group / group(0) => everything, including non-capturing. group 1 = first capturing group
            if (m.groupCount() > 0) {
                return m.group(1);
            }
        }
        return defaultValue;
    }

    private static boolean isPatternFindable(String text, Pattern pattern) {
        return pattern.matcher(text).find();
    }

    //
    // Singleton
    //
    /*
    private static SttCommander __instance;

    public static SttCommander get() {
        if (__instance == null) {
            __instance = new SttCommander();
        }
        return __instance;
    }

    //
    // Members, Constructors
    //
    public SttCommander() {

    }


    private String parseDescription(String line) {
        String d = parseOneValueOrDefault(line, PATTERN_DESCRIPTION, "");
        if (parseCreationDate(line).isEmpty() && d.startsWith(" " + parseCompletionDate(line) + " ")) {
            d = d.substring(1 + 4 + 1 + 2 + 1 + 2 + 1);
        }
        return d;
    }

    private Map<String, String> parseKeyValuePairs(String line) {
        Map<String, String> values = new HashMap<>();
        for (String kvp : parseAllUniqueMatchesWithOneValue(line, PATTERN_KEY_VALUE_PAIRS)) {
            int s = kvp.indexOf(':');
            values.put(kvp.substring(0, s), kvp.substring(s + 1));
        }
        return values;
    }

    //
    // General methods
    //
    @SuppressWarnings("StatementWithEmptyBody")
    private static String[] splitAtIndexFailsafe(String text, int atIndex) {
        String left = "";
        String right = "";

        if (text == null || text.isEmpty()) {

        } else if (atIndex >= text.length()) {
            left = text;
        } else if (atIndex < 0) {
            right = text;
        } else {
            left = text.substring(0, atIndex);
            right = text.substring(atIndex);
        }

        return new String[]{left, right};
    }

    // Replace till the end of the line, starting from index
    public static String replaceTillEndOfLineFromIndex(int index, String text, String replacementLine) {
        String[] split = splitAtIndexFailsafe(text, index);
        split[1] = split[1].contains("\n") ? split[1].replaceFirst(".*(\\n)", replacementLine + "\n") : replacementLine;
        return split[0] + split[1];
    }

    // not empty
    public static boolean nz(String str) {
        return str != null && !str.isEmpty();
    }

    public static String getDaysFromToday(int days) {
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.DATE, days);
        return DATEF_YYYY_MM_DD.format(cal.getTime());
    }



    // Parse all tasks in (newline separated) tasks
    public static ArrayList<SttTaskWithParserInfo> parseTasksFromTextWithParserInfo(String text) {
        final ArrayList<SttTaskWithParserInfo> tasks = new ArrayList<>();
        final SttCommander stt = SttCommander.get();
        for (String task : text.split("\n")) {
            tasks.add(stt.parseTask(task));
        }
        return tasks;
    }

    public static String tasksToString(Collection<? extends SttTaskWithParserInfo> tasks) {
        final StringBuffer sb = new StringBuffer();
        for (SttTaskWithParserInfo task : tasks) {
            sb.append(task.getTaskLine());
            sb.append("\n");
        }
        return sb.toString();
    }

    // Sort tasks array and return it. Changes input array.
    public static List<? extends SttTask> sortTasks(List<? extends SttTask> tasks, String orderBy, Boolean descending) {
        Collections.sort(tasks, new SttCommander.SttTaskSimpleComparator(orderBy, descending));
        return tasks;
    }

    public static class SttTaskSimpleComparator implements Comparator<SttTask> {
        private String _orderBy;
        private boolean _descending;

        public static final String BY_PRIORITY = "priority";
        public static final String BY_CONTEXT = "context";
        public static final String BY_PROJECT = "project";
        public static final String BY_CREATION_DATE = "creation_date";
        public static final String BY_DUE_DATE = "due_date";
        public static final String BY_DESCRIPTION = "description";
        public static final String BY_LINE = "line_natural";

        public SttTaskSimpleComparator(String orderBy, Boolean descending) {
            _orderBy = orderBy;
            _descending = descending;
        }

        @Override
        public int compare(SttTask x, SttTask y) {
            int difference;
            switch (_orderBy) {
                case BY_PRIORITY: {
                    difference = compare(x.getPriority(), y.getPriority());
                    break;
                }
                case BY_CONTEXT: {
                    difference = compare(x.getContexts(), y.getContexts());
                    break;
                }
                case BY_PROJECT: {
                    difference = compare(x.getProjects(), y.getProjects());
                    break;
                }
                case BY_CREATION_DATE: {
                    difference = compare(x.getCreationDate(), y.getCreationDate());
                    break;
                }
                case BY_DUE_DATE: {
                    difference = compare(x.getDueDate(), y.getDueDate());
                    break;
                }
                case BY_DESCRIPTION: {
                    difference = compare(x.getDescription(), y.getDescription());
                    break;
                }
                case BY_LINE: {
                    if (x instanceof SttTaskParserInfoExtension && y instanceof SttTaskParserInfoExtension) {
                        difference = compare(((SttTaskParserInfoExtension) x).getTaskLine(), ((SttTaskParserInfoExtension) y).getTaskLine());
                    } else {
                        difference = compare(x.getDescription(), y.getDescription());
                    }
                    break;
                }
                default: {
                    return 0;
                }
            }
            if (_descending) {
                difference = -1 * difference;
            }
            return difference;
        }

        private int compareNull(Object o1, Object o2) {
            return ((o1 == null && o2 == null) || (o1 != null && o2 != null))
                    ? 0
                    : o1 == null ? -1 : 0;
        }

        private int compare(Character x, Character y) {
            return Character.compare(Character.toLowerCase(x), Character.toLowerCase(y));
        }

        private int compare(List<String> x, List<String> y) {
            if (x.isEmpty() & y.isEmpty()) {
                return 0;
            }
            if (x.isEmpty()) {
                return 1;
            }
            if (y.isEmpty()) {
                return -1;
            }
            return x.get(0).compareTo(y.get(0));

        }

        private int compare(String x, String y) {
            int n = compareNull(x, y);
            if (n != 0) {
                return n;
            } else {
                return x.trim().toLowerCase().compareTo(y.trim().toLowerCase());
            }
        }
    }
     */
}
