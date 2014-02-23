package ro.redeul.google.go.lang.parser.parsing.statements;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import ro.redeul.google.go.lang.parser.GoElementTypes;
import ro.redeul.google.go.lang.parser.parsing.util.ParserUtils;

/**
 * User: mtoader
 * Date: Jul 25, 2010
 * Time: 8:07:21 PM
 */
class GotoStatement implements GoElementTypes {
    public static IElementType parse(PsiBuilder builder) {

        PsiBuilder.Marker marker = builder.mark();

        if (!ParserUtils.getToken(builder, kGOTO)) {
            marker.rollbackTo();
            return null;
        }

        PsiBuilder.Marker labelMarker = builder.mark();
        if ( ! ParserUtils.getToken(builder, mIDENT, "label.expected") ) {
            labelMarker.drop();
        } else {
            labelMarker.done(LITERAL_IDENTIFIER);
        }
        marker.done(GOTO_STATEMENT);
        return GOTO_STATEMENT;
    }
}
