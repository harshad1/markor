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

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.util.Patterns;

import net.gsantner.markor.format.general.FirstLineTopPaddedParagraphSpan;
import net.gsantner.markor.format.general.HorizontalLineBackgroundParagraphSpan;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;

import java.util.regex.Pattern;

public class TodoTxtHighlighter extends Highlighter {
    private final TodoTxtHighlighterColors colors;

    private final Pattern LINK = Patterns.WEB_URL;
    private final Pattern NEWLINE_CHARACTER = Pattern.compile("(\\n|^)");
    private final Pattern LINESTART = Pattern.compile("(?m)^.");
    private final Pattern LINE_OF_TEXT = Pattern.compile("(?m)(.*)?");

    public TodoTxtHighlighter(HighlightingEditor hlEditor, Document document) {
        super(hlEditor, document);
        colors = new TodoTxtHighlighterColors();
    }

    @Override
    protected Editable run(final Editable editable) {
        try {
            clearSpans(editable);

            if (editable.length() == 0) {
                return editable;
            }

            final boolean isDarkBg = _appSettings.isDarkThemeEnabled();

            generalHighlightRun(editable);

            createParagraphStyleSpanForMatches(editable, LINE_OF_TEXT,
                    (matcher, iM) -> new FirstLineTopPaddedParagraphSpan(2f));

            createColorSpanForMatches(editable, TodoTxtTask.PATTERN_CONTEXTS, colors.getContextColor());
            createColorSpanForMatches(editable, TodoTxtTask.PATTERN_PROJECTS, colors.getCategoryColor());
            createStyleSpanForMatches(editable, TodoTxtTask.PATTERN_KEY_VALUE_PAIRS, Typeface.ITALIC);

            // Priorities
            createStyleSpanForMatches(editable, TodoTxtTask.PATTERN_PRIORITY_ANY, Typeface.BOLD);
            createColorSpanForMatches(editable, TodoTxtTask.PATTERN_PRIORITY_A, colors.getPriorityColor(1));
            createColorSpanForMatches(editable, TodoTxtTask.PATTERN_PRIORITY_B, colors.getPriorityColor(2));
            createColorSpanForMatches(editable, TodoTxtTask.PATTERN_PRIORITY_C, colors.getPriorityColor(3));
            createColorSpanForMatches(editable, TodoTxtTask.PATTERN_PRIORITY_D, colors.getPriorityColor(4));
            createColorSpanForMatches(editable, TodoTxtTask.PATTERN_PRIORITY_E, colors.getPriorityColor(5));
            createColorSpanForMatches(editable, TodoTxtTask.PATTERN_PRIORITY_F, colors.getPriorityColor(6));

            // Date: Match Creation date before completition date
            createColorSpanForMatches(editable, TodoTxtTask.PATTERN_DATE, colors.getDateColor(isDarkBg));
            createColorSpanForMatches(editable, TodoTxtTask.PATTERN_DUE_DATE, colors.getPriorityColor(1), 2, 3);
            //createColorSpanForMatches(editable, TodoTxtTask.CREATION_DATE, 0xff00ff00);
            //createColorSpanForMatches(editable, TodoTxtTask.COMPLETION_DATE, 0xff0000ff);


            // Paragraph divider
            createParagraphStyleSpanForMatches(editable, LINE_OF_TEXT,
                    (matcher, iM) -> new HorizontalLineBackgroundParagraphSpan(_hlEditor.getCurrentTextColor(), 0.8f, _hlEditor.getTextSize() / 2f));

            // Strike out done tasks (apply no other to-do.txt span format afterwards)
            createColorSpanForMatches(editable, TodoTxtTask.PATTERN_DONE, colors.getDoneColor(isDarkBg));
            createSpanWithStrikeThroughForMatches(editable, TodoTxtTask.PATTERN_DONE);

            // Fix for paragraph padding and horizontal rule
            createRelativeSizeSpanForMatches(editable, LINESTART, 1.00001f);
        } catch (Exception ex) {
            // Ignoring errors
        }

        return editable;
    }

    @Override
    public InputFilter getAutoFormatter() {
        return new TodoTxtAutoFormat();
    }

    @Override
    public int getHighlightingDelay(Context context) {
        return new AppSettings(context).getHighlightingDelayTodoTxt();
    }

}
