package ro.redeul.google.go.lang.psi.declarations;

import ro.redeul.google.go.lang.psi.GoPsiElement;
import ro.redeul.google.go.lang.psi.expressions.GoExpr;
import ro.redeul.google.go.lang.psi.expressions.literals.GoLiteralIdentifier;
import ro.redeul.google.go.lang.psi.types.GoPsiType;

public interface GoConstDeclaration extends GoPsiElement {

    boolean hasInitializers();

    GoLiteralIdentifier[] getIdentifiers();

    public GoPsiType getIdentifiersType();

    GoExpr[] getExpressions();

    GoExpr getExpression(GoLiteralIdentifier identifier);

    Integer getConstSpecIndex();

}
