/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.todotxt;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.View;

import net.gsantner.markor.R;
import net.gsantner.markor.format.general.CommonTextActions;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.SearchOrCustomTextDialogCreator;
import net.gsantner.markor.ui.hleditor.TextActions;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.DocumentIO;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.format.todotxt.SttCommander;
import net.gsantner.opoc.util.FileUtils;
import net.gsantner.opoc.util.StringUtils;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TODO
public class TodoTxtTextActions extends TextActions {

    public TodoTxtTextActions(Activity activity, Document document) {
        super(activity, document);
    }

    @Override
    public boolean runAction(String action, boolean modLongClick, String anotherArg) {
        return runCommonTextAction(action);
    }

    @Override
    protected ActionCallback getActionCallback(@StringRes int keyId) {
        return new TodoTxtTextActionsImpl(keyId);
    }

    @Override
    public List<ActionItem> getActiveActionList() {

        final ActionItem[] TMA_ACTIONS = {
                new ActionItem(R.string.tmaid_todotxt_toggle_done, R.drawable.ic_check_box_black_24dp, R.string.toggle_done),
                new ActionItem(R.string.tmaid_todotxt_add_context, R.drawable.gs_email_sign_black_24dp, R.string.add_context),
                new ActionItem(R.string.tmaid_todotxt_add_project, R.drawable.ic_local_offer_black_24dp, R.string.add_project),
                new ActionItem(R.string.tmaid_todotxt_priority, R.drawable.ic_star_border_black_24dp, R.string.priority),
                new ActionItem(R.string.tmaid_common_delete_lines, CommonTextActions.ACTION_DELETE_LINES_ICON, R.string.delete_lines),
                new ActionItem(R.string.tmaid_common_open_link_browser, CommonTextActions.ACTION_OPEN_LINK_BROWSER__ICON, R.string.open_link),
                new ActionItem(R.string.tmaid_common_attach_something, R.drawable.ic_attach_file_black_24dp, R.string.attach),
                new ActionItem(R.string.tmaid_common_special_key, CommonTextActions.ACTION_SPECIAL_KEY__ICON, R.string.special_key),
                new ActionItem(R.string.tmaid_todotxt_archive_done_tasks, R.drawable.ic_archive_black_24dp, R.string.archive_completed_tasks),
                new ActionItem(R.string.tmaid_todotxt_sort_todo, R.drawable.ic_sort_by_alpha_black_24dp, R.string.sort_alphabetically),
                new ActionItem(R.string.tmaid_todotxt_current_date, R.drawable.ic_date_range_black_24dp, R.string.current_date),
                new ActionItem(R.string.tmaid_common_next_line, R.drawable.ic_baseline_keyboard_return_24, R.string.next_line)
        };

        return Arrays.asList(TMA_ACTIONS);
    }

    @Override
    protected @StringRes
    int getFormatActionsKey() {
        return R.string.pref_key__todotxt__action_keys;
    }

    private class TodoTxtTextActionsImpl extends ActionCallback {
        private int _action;

        TodoTxtTextActionsImpl(int action) {
            _action = action;
        }

        @SuppressWarnings("StatementWithEmptyBody")
        @Override
        public void onClick(View view) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            final String origText = _hlEditor.getText().toString();
            final int origSelectionStart = _hlEditor.getSelectionStart();
            final CommonTextActions commonTextActions = new CommonTextActions(_activity, _hlEditor);

            switch (_action) {
                case R.string.tmaid_todotxt_toggle_done: {
                    final String replaceDone = String.format("x %s ", SttCommander.getToday());
                    ReplacePattern[] patterns = {
                            new ReplacePattern(SttCommander.PATTERN_PRIORITY_ANY, replaceDone),
                            new ReplacePattern(SttCommander.PATTERN_COMPLETION_DATE, ""),
                            new ReplacePattern("^", replaceDone),
                    };
                    runRegexReplaceAction(Arrays.asList(patterns));
                    trimLeadingWhiteSpace();
                    return;
                }
                case R.string.tmaid_todotxt_add_context: {
                    SearchOrCustomTextDialogCreator.showSttContextDialog(_activity, sttcmd.parseContexts(origText), origTask.getContexts(), (context) -> {
                        context = (context.charAt(0) == '@') ? context : "@" + context;
                        if (insertAtEnd()) {
                            runRegexReplaceAction("\\s+$", " " + context);
                        } else {
                            insertInline(context);
                        }
                    });
                    return;
                }
                case R.string.tmaid_todotxt_add_project: {
                    SearchOrCustomTextDialogCreator.showSttProjectDialog(_activity, sttcmd.parseProjects(origText), origTask.getProjects(), (project) -> {
                        project = (project.charAt(0) == '+') ? project : "+" + project;
                        if (insertAtEnd()) {
                            runRegexReplaceAction("\\s+$", " " + project);
                        } else {
                            insertInline(project);
                        }
                    });
                    return;
                }
                case R.string.tmaid_todotxt_priority: {
                    SearchOrCustomTextDialogCreator.showPriorityDialog(_activity, origTask.getPriority(), (priority) -> {
                        ArrayList<ReplacePattern> patterns = new ArrayList<>();
                        if (priority.charAt(0) == 'N') {
                            patterns.add(new ReplacePattern(SttCommander.PATTERN_PRIORITY_ANY, ""));
                        } else {
                            final String _priority = String.format("(%c) ", priority.charAt(0));
                            patterns.add(new ReplacePattern(SttCommander.PATTERN_PRIORITY_ANY, _priority));
                            patterns.add(new ReplacePattern("^\\s*", _priority));
                        }
                        runRegexReplaceAction(patterns);
                        trimLeadingWhiteSpace();
                    });
                    return;
                }
                case R.string.tmaid_todotxt_current_date: {
                    setDate();
                    return;
                }
                case R.string.tmaid_common_delete_lines: {
                    final int[] sel = StringUtils.getSelection(_hlEditor);
                    _hlEditor.getText().delete(StringUtils.getLineStart(origText, sel[0]), StringUtils.getLineEnd(origText, sel[1]));
                    return;
                }
                case R.string.tmaid_todotxt_archive_done_tasks: {
                    SearchOrCustomTextDialogCreator.showSttArchiveDialog(_activity, (callbackPayload) -> {
                        // Don't do parse tasks in this case, performance wise
                        ArrayList<String> keep = new ArrayList<>();
                        ArrayList<String> move = new ArrayList<>();

                        String newCursorTarget = origTask.getTaskLine();
                        if (origTask.isDone()) {
                            int pos = origTask.getLineOffsetInText() + origTask.getTaskLine().length() + 1;
                            while (pos < origText.length()) {
                                SttTaskWithParserInfo task = sttcmd.parseTask(origText, pos);
                                if (!task.isDone()) {
                                    newCursorTarget = task.getTaskLine();
                                    break;
                                }
                                pos += task.getTaskLine().length() + 1;
                            }
                        }

                        for (String task : origText.split("\n")) {
                            if (task.startsWith("x ")) {
                                move.add(task);
                            } else {
                                keep.add(task);
                            }
                        }
                        if (!move.isEmpty()) {
                            File todoFile = _document.getFile();
                            if (todoFile != null && (todoFile.getParentFile().exists() || todoFile.getParentFile().mkdirs())) {
                                File doneFile = new File(todoFile.getParentFile(), callbackPayload);
                                String doneFileContents = "";
                                if (doneFile.exists() && doneFile.canRead()) {
                                    doneFileContents = FileUtils.readTextFileFast(doneFile).trim() + "\n";
                                }
                                doneFileContents += TextUtils.join("\n", move).trim() + "\n";

                                // Write to do done file
                                if (DocumentIO.saveDocument(new Document(doneFile), doneFileContents, new ShareUtil(_activity), getContext())) {
                                    // All went good
                                    _hlEditor.setText(TextUtils.join("\n", keep));
                                    int newIndex = _hlEditor.getText().toString().indexOf(newCursorTarget);
                                    if (newIndex < 0 || newIndex >= _hlEditor.length()) {
                                        newIndex = _hlEditor.length();
                                    }
                                    _hlEditor.setSelection(newIndex);
                                }
                            }
                        }
                        new AppSettings(_activity).setLastTodoUsedArchiveFilename(callbackPayload);
                    });
                    return;
                }
                /*
                case R.string.tmaid_todotxt_sort_todo: {
                    SearchOrCustomTextDialogCreator.showSttSortDialogue(_activity, (orderBy, descending) -> new Thread() {
                        @Override
                        public void run() {
                            ArrayList<SttTaskWithParserInfo> tasks = SttCommander.parseTasksFromTextWithParserInfo(origText);
                            SttCommander.sortTasks(tasks, orderBy, descending);
                            setEditorTextAsync(SttCommander.tasksToString(tasks));
                        }
                    }.start());
                    break;
                }
                 */
                case R.string.tmaid_common_open_link_browser: {
                    commonTextActions.runAction(CommonTextActions.ACTION_OPEN_LINK_BROWSER);
                    break;
                }
                case R.string.tmaid_common_special_key: {
                    commonTextActions.runAction(CommonTextActions.ACTION_SPECIAL_KEY);
                    break;
                }
                default:
                    runAction(_context.getString(_action));
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (_hlEditor.getText() == null) {
                return false;
            }
            final CommonTextActions commonTextActions = new CommonTextActions(_activity, _hlEditor);

            switch (_action) {
                /*
                case R.string.tmaid_todotxt_add_context: {
                    SearchOrCustomTextDialogCreator.showSttContextListDialog(_activity, sttcmd.parseContexts(origText), origTask.getContexts(), origText, (callbackPayload) -> {
                        int cursor = origText.indexOf(callbackPayload);
                        _hlEditor.setSelection(Math.min(_hlEditor.length(), Math.max(0, cursor)));
                    });
                    return true;
                }
                case R.string.tmaid_todotxt_add_project: {
                    SearchOrCustomTextDialogCreator.showSttProjectListDialog(_activity, sttcmd.parseProjects(origText), origTask.getContexts(), origText, (callbackPayload) -> {
                        int cursor = origText.indexOf(callbackPayload);
                        _hlEditor.setSelection(Math.min(_hlEditor.length(), Math.max(0, cursor)));
                    });
                    return true;
                }
                 */
                case R.string.tmaid_common_special_key: {
                    commonTextActions.runAction(CommonTextActions.ACTION_JUMP_BOTTOM_TOP);
                    return true;
                }

                case R.string.tmaid_common_open_link_browser: {
                    commonTextActions.runAction(CommonTextActions.ACTION_SEARCH);
                    return true;
                }
                case R.string.tmaid_todotxt_current_date: {

                    setDueDate(3);
                    return true;
                }
            }
            return false;
        }
    }

    private void trimLeadingWhiteSpace() {
        runRegexReplaceAction("^\\s*", "");
    }

    private void insertInline(String thing) {
        final int[] sel = StringUtils.getSelection(_hlEditor);
        final CharSequence text = _hlEditor.getText();
        if (sel[0] > 0) {
            final char before = text.charAt(sel[0] - 1);
            if (before != ' ' && before != '\n') {
                thing = " " + thing;
            }
        }
        if (sel[1] < text.length()) {
            final char after = text.charAt(sel[1]);
            if (after != ' ' && after != '\n') {
                thing = thing + " ";
            }
        }
        _hlEditor.insertOrReplaceTextOnCursor(thing);
    }

    private boolean selIsSingleLine() {
        final Editable text = _hlEditor.getText();
        final int[] sel = StringUtils.getSelection(_hlEditor);
        return StringUtils.getLineStart(text, sel[0]) != StringUtils.getLineStart(text, sel[1]);
    }

    private boolean insertAtEnd() {
        return selIsSingleLine() || _appSettings.isTodoAppendProConOnEndEnabled();
    }

    private String[] getLines() {
        final CharSequence text = _hlEditor.getText();
        final int[] sel = StringUtils.getSelection(_hlEditor);
        final CharSequence selLines = text.subSequence(
                StringUtils.getLineStart(text, sel[0]),
                StringUtils.getLineStart(text, sel[1])
        );
        return selLines.toString().split("\n");
    }

    private static Calendar parseDateString(String dateString, Calendar fallback) {
        if (dateString == null || dateString.length() != SttCommander.DATEF_YYYY_MM_DD_LEN) {
            return fallback;
        }

        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(SttCommander.DATEF_YYYY_MM_DD.parse(dateString));
            return calendar;
        } catch (ParseException e) {
            return fallback;
        }
    }

    private void setDate() {
        final int[] sel = StringUtils.getSelection(_hlEditor);
        final Editable text = _hlEditor.getText();
        final String selStr = text.subSequence(sel[0], sel[1]).toString();
        Calendar initDate = parseDateString(selStr, Calendar.getInstance());

        DatePickerDialog.OnDateSetListener listener = (_view, year, month, day) -> {
            Calendar fmtCal = Calendar.getInstance();
            fmtCal.set(year, month, day);
            final String newDate = SttCommander.DATEF_YYYY_MM_DD.format(fmtCal.getTime());
            text.replace(sel[0], sel[1], newDate);
        };

        new DateFragment()
                .setActivity(_activity)
                .setListener(listener)
                .setCalendar(initDate)
                .setMessage(getContext().getString(R.string.insert_replace_date))
                .show(((FragmentActivity) _activity).getSupportFragmentManager(), "date");
    }


    private void setDueDate(int offset) {
        final String dueString = SttCommander.parseDueDate(getLines()[0], SttCommander.getToday());
        Calendar initDate = parseDateString(dueString, Calendar.getInstance());
        initDate.add(Calendar.DAY_OF_MONTH, dueString == null ? offset : 0);

        DatePickerDialog.OnDateSetListener listener = (_view, year, month, day) -> {
            Calendar fmtCal = Calendar.getInstance();
            fmtCal.set(year, month, day);
            final String newDue = "due:" + SttCommander.DATEF_YYYY_MM_DD.format(fmtCal.getTime());
            ReplacePattern[] patterns = {
                    // Replace due date
                    new ReplacePattern(SttCommander.PATTERN_DUE_DATE, newDue),
                    // Add due date to end if none already exists. Will correctly handle trailing whitespace.
                    new ReplacePattern("(\\s)*$", " " + newDue),
            };
            runRegexReplaceAction(Arrays.asList(patterns));
        };

        new DateFragment()
                .setActivity(_activity)
                .setListener(listener)
                .setCalendar(initDate)
                .setMessage(getContext().getString(R.string.due_date))
                .show(((FragmentActivity) _activity).getSupportFragmentManager(), "date");
    }

    /**
     * A DialogFragment to manage showing a DatePicker
     * Must be public and have default constructor.
     */
    public static class DateFragment extends DialogFragment {

        private DatePickerDialog.OnDateSetListener _listener;
        private Activity _activity;
        private int _year;
        private int _month;
        private int _day;
        private String _message;

        public DateFragment() {
            super();
            setCalendar(Calendar.getInstance());
        }

        public DateFragment setListener(DatePickerDialog.OnDateSetListener listener) {
            _listener = listener;
            return this;
        }

        public DateFragment setActivity(Activity activity) {
            _activity = activity;
            return this;
        }

        public DateFragment setYear(int year) {
            _year = year;
            return this;
        }

        public DateFragment setMonth(int month) {
            _month = month;
            return this;
        }

        public DateFragment setDay(int day) {
            _day = day;
            return this;
        }

        public DateFragment setMessage(String message) {
            _message = message;
            return this;
        }

        public DateFragment setCalendar(Calendar calendar) {
            setYear(calendar.get(Calendar.YEAR));
            setMonth(calendar.get(Calendar.MONTH));
            setDay(calendar.get(Calendar.DAY_OF_MONTH));
            return this;
        }

        @Override
        public DatePickerDialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);

            DatePickerDialog dialog = new DatePickerDialog(_activity, _listener, _year, _month, _day);
            if (_message != null) {
                dialog.setMessage(_message);
            }
            return dialog;
        }
    }
}
