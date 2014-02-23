package ro.redeul.google.go.lang.parser.parsing.expressions;

import com.intellij.lang.PsiBuilder;
import ro.redeul.google.go.lang.parser.GoElementTypes;
import ro.redeul.google.go.lang.parser.GoParser;
import ro.redeul.google.go.lang.parser.parsing.util.ParserUtils;

/**
 * Created by IntelliJ IDEA.
 * User: mtoader
 * Date: Jul 25, 2010
 * Time: 2:18:28 AM
 * To change this template use File | Settings | File Templates.
 */
class ParenthesizedExpression implements GoElementTypes {

    public static boolean parse(PsiBuilder builder, GoParser parser) {

        PsiBuilder.Marker expression = builder.mark();

        ParserUtils.getToken(builder, pLPAREN);

        parser.parseExpression(builder);

        ParserUtils.getToken(builder, pRPAREN, "right.parenthesis.expected");
        expression.done(PARENTHESISED_EXPRESSION);

        return true;
    }
}
