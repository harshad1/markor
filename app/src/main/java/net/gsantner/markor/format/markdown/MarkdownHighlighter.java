/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;

import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;

import other.writeily.format.markdown.WrMarkdownHeaderSpanCreator;

public class MarkdownHighlighter extends Highlighter {
    public final String _fontType;
    public final Integer _fontSize;
    private final boolean _highlightLineEnding;
    private final boolean _highlightCodeChangeFont;
    private final boolean _highlightBiggerHeadings;
    private final boolean _highlightDisableCodeBlock;

    private static final int MD_COLOR_HEADING = 0xffef6D00;
    private static final int MD_COLOR_LINK = 0xff1ea3fe;
    private static final int MD_COLOR_LIST = 0xffdaa521;
    private static final int MD_COLOR_QUOTE = 0xff88b04c;
    private static final int MD_COLOR_CODEBLOCK = 0xff8c8c8c;

    public MarkdownHighlighter(HighlightingEditor hlEditor, Document document) {
        super(hlEditor, document);
        _highlightLinks = false;
        _fontType = _appSettings.getFontFamily();
        _fontSize = _appSettings.getFontSize();
        _highlightLineEnding = _appSettings.isMarkdownHighlightLineEnding();
        _highlightCodeChangeFont = _appSettings.isMarkdownHighlightCodeFontMonospaceAllowed();
        _highlightBiggerHeadings = _appSettings.isMarkdownBiggerHeadings();
        _highlightDisableCodeBlock = _appSettings.isMarkdownDisableCodeBlockHighlight();
        setTextModifier(new ListHandler(_appSettings.isMarkdownAutoUpdateList()));
    }

    @Override
    protected Editable run(final Editable editable) {
        try {
            clearSpans(editable);

            if (editable.length() == 0) {
                return editable;
            }

            generalHighlightRun(editable);

            if (_highlightBiggerHeadings) {
                createHeaderSpanForMatches(editable, MarkdownHighlighterPattern.HEADING, MD_COLOR_HEADING);
            } else {
                createColorSpanForMatches(editable, MarkdownHighlighterPattern.HEADING_SIMPLE.pattern, MD_COLOR_HEADING);
            }
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.LINK.pattern, MD_COLOR_LINK);
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.LIST_UNORDERED.pattern, MD_COLOR_LIST);
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.LIST_ORDERED.pattern, MD_COLOR_LIST);
            if (_highlightLineEnding) {
                createColorBackgroundSpan(editable, MarkdownHighlighterPattern.DOUBLESPACE_LINE_ENDING.pattern, MD_COLOR_CODEBLOCK);
            }
            createStyleSpanForMatches(editable, MarkdownHighlighterPattern.BOLD.pattern, Typeface.BOLD);
            createStyleSpanForMatches(editable, MarkdownHighlighterPattern.ITALICS.pattern, Typeface.ITALIC);
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.QUOTATION.pattern, MD_COLOR_QUOTE);
            createSpanWithStrikeThroughForMatches(editable, MarkdownHighlighterPattern.STRIKETHROUGH.pattern);
            if (_highlightCodeChangeFont) {
                createMonospaceSpanForMatches(editable, MarkdownHighlighterPattern.CODE.pattern);
            }
            if (!_highlightDisableCodeBlock) {
                createColorBackgroundSpan(editable, MarkdownHighlighterPattern.CODE.pattern, MD_COLOR_CODEBLOCK);
            }

        } catch (Exception ex) {
            // Ignoring errors
        }

        return editable;
    }

    private void createHeaderSpanForMatches(Editable editable, MarkdownHighlighterPattern pattern, int headerColor) {
        createSpanForMatches(editable, pattern.pattern, new WrMarkdownHeaderSpanCreator(this, editable, headerColor, _highlightBiggerHeadings));
    }

    @Override
    public InputFilter getAutoFormatter() {
        return new MarkdownAutoFormat();
    }

    @Override
    public int getHighlightingDelay(Context context) {
        return new AppSettings(context).getMarkdownHighlightingDelay();
    }
}

