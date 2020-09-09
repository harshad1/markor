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

import net.gsantner.markor.format.general.FirstLineTopPaddedParagraphSpan;
import net.gsantner.markor.format.general.HorizontalLineBackgroundParagraphSpan;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;

public class TodoTxtHighlighter extends Highlighter {
    private final TodoTxtHighlighterColors colors;

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
            createParagraphStyleSpanForMatches(editable, TodoTxtHighlighterPattern.LINE_OF_TEXT.getPattern(),
                    (matcher, iM) -> new FirstLineTopPaddedParagraphSpan(2f));


            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.CONTEXT.getPattern(), colors.getContextColor());
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PROJECT.getPattern(), colors.getCategoryColor());
            createStyleSpanForMatches(editable, TodoTxtHighlighterPattern.PATTERN_KEY_VALUE.getPattern(), Typeface.ITALIC);

            // Priorities
            createStyleSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_ANY.getPattern(), Typeface.BOLD);
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_A.getPattern(), colors.getPriorityColor(1));
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_B.getPattern(), colors.getPriorityColor(2));
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_C.getPattern(), colors.getPriorityColor(3));
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_D.getPattern(), colors.getPriorityColor(4));
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_E.getPattern(), colors.getPriorityColor(5));
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_F.getPattern(), colors.getPriorityColor(6));

            // Date: Match Creation date before completition date
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.DATE.getPattern(), colors.getDateColor(isDarkBg));
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.DUE_DATE.getPattern(), colors.getPriorityColor(1), 1);
            //createColorSpanForMatches(editable, TodoTxtHighlighterPattern.CREATION_DATE.getPattern(), 0xff00ff00);
            //createColorSpanForMatches(editable, TodoTxtHighlighterPattern.COMPLETION_DATE.getPattern(), 0xff0000ff);


            // Paragraph divider
            createParagraphStyleSpanForMatches(editable, TodoTxtHighlighterPattern.LINE_OF_TEXT.getPattern(),
                    (matcher, iM) -> new HorizontalLineBackgroundParagraphSpan(_hlEditor.getCurrentTextColor(), 0.8f, _hlEditor.getTextSize() / 2f));

            // Strike out done tasks (apply no other to-do.txt span format afterwards)
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.DONE.getPattern(), colors.getDoneColor(isDarkBg));
            createSpanWithStrikeThroughForMatches(editable, TodoTxtHighlighterPattern.DONE.getPattern());

            // Fix for paragraph padding and horizontal rule
            /*
            createRelativeSizeSpanForMatches(editable, TodoTxtHighlighterPattern.LINESTART.getPattern(), 0.8f);
            createRelativeSizeSpanForMatches(editable, TodoTxtHighlighterPattern.LINESTART.getPattern(), 1.2f);*/
            createRelativeSizeSpanForMatches(editable, TodoTxtHighlighterPattern.LINESTART.getPattern(), 1.00001f);
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

