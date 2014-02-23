package ro.redeul.google.go.intentions.statements;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import ro.redeul.google.go.intentions.Intention;
import ro.redeul.google.go.lang.psi.GoPsiElement;
import ro.redeul.google.go.lang.psi.expressions.GoExpr;
import ro.redeul.google.go.lang.psi.expressions.binary.GoLogicalAndExpression;
import ro.redeul.google.go.lang.psi.expressions.binary.GoLogicalOrExpression;
import ro.redeul.google.go.lang.psi.expressions.binary.GoRelationalExpression;
import ro.redeul.google.go.lang.psi.expressions.literals.GoLiteralIdentifier;
import ro.redeul.google.go.lang.psi.expressions.primary.GoLiteralExpression;
import ro.redeul.google.go.lang.psi.statements.GoExpressionStatement;
import ro.redeul.google.go.lang.psi.types.GoPsiType;
import ro.redeul.google.go.lang.psi.types.GoPsiTypeName;
import ro.redeul.google.go.lang.psi.typing.GoType;
import ro.redeul.google.go.lang.psi.typing.GoTypePsiBacked;
import ro.redeul.google.go.lang.psi.utils.GoTypeUtils;
import ro.redeul.google.go.util.GoUtil;

import static ro.redeul.google.go.lang.psi.utils.GoPsiUtils.findParentOfType;

public abstract class BaseBoolStatement extends Intention {

    protected GoExpressionStatement statement;
    protected GoExpr expr;

    @Override
    protected boolean satisfiedBy(PsiElement element) {
        statement = element instanceof GoExpressionStatement ? (GoExpressionStatement) element : findParentOfType(element, GoExpressionStatement.class);
        if (statement == null && element instanceof PsiWhiteSpace && element.getPrevSibling() instanceof GoExpressionStatement) {
            statement = (GoExpressionStatement) element.getPrevSibling();
        }
        if (statement != null) {
            expr = statement.getExpression();
            if (expr != null) {
                if (expr instanceof GoRelationalExpression
                        || expr instanceof GoLogicalAndExpression
                        || expr instanceof GoLogicalOrExpression)
                    return true;

                for (GoType goType : expr.getType()) {
                    if (goType != null) {
                        if (goType instanceof GoTypePsiBacked) {
                            GoPsiType psiType = GoTypeUtils.resolveToFinalType(((GoTypePsiBacked) goType).getPsiType());
                            if (psiType instanceof GoPsiTypeName)
                                return psiType.getText().equals("bool") && ((GoPsiTypeName) psiType).isPrimitive();
                        }
                    }
                }
                if (expr instanceof GoLiteralExpression) {
                    PsiElement literal = ((GoLiteralExpression) expr).getLiteral();
                    if (literal instanceof GoLiteralIdentifier) {
                        literal = GoUtil.ResolveTypeOfVarDecl((GoPsiElement) literal);
                        if (literal.getText().equals("true") || literal.getText().equals("false")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
