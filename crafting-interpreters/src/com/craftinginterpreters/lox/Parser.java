package com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenType.*;
import java.util.ArrayList;
import java.util.List;

/* (From Crafting Interpreters, 2021)
 * This class is a representation of the following expression grammar,
 * which encodes precedence:
 *
 * expression → equality ;
 * equality   → comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
 * term       → factor ( ( "-" | "+" ) factor )* ;
 * factor     → unary ( ( "/" | "*" ) unary )* ;
 * unary      → ( "!" | "-" ) unary
              | primary ;
 * primary    → NUMBER | STRING | "true" | "false" | "nil"
              | "(" expression ")" ;
 */

class Parser {

  private static class ParseError extends RuntimeException {}
  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());
    }

    return statements;
  }

  // ===== Statements  =====

  private Stmt statement() {
    if (match(PRINT)) return printStatement();

    return expressionStatement();
  }

  private Stmt printStatement() {

    Expr value = expression();

    consume(SEMICOLON, "Expect ';' after value.");

    return new Stmt.Print(value);
  }

  private Stmt expressionStatement() {

    Expr expr = expression();

    consume(SEMICOLON, "Expect ';' after expression.");

    return new Stmt.Expression(expr);
  }

  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(RIGHT_BRACE, "Except '}' after block.");

    return statements;
  }

  private Expr assignment() {
    Expr expr = equality();

    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();
      
       if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        return new Expr.Assign(name, value);
       }

       error(equals, "Invalid assignment target.");
    }

    return expr;
  }

  private Stmt declaration() {
    try {
      if (match(VAR)) return varDeclaration();
      
      return statement();
    } catch (ParseError error) {
      synchronise();
      return null;
    }
  }

  // if 'var' is found, we must be declaring a new variable
  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expect variable name.");

    // Set variable's value to null (it might not be initialised)
    Expr initialiser = null;

    if (match(EQUAL)) {
      initialiser = expression();
    }

    consume(SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, initialiser);
  }

  // =======================

  // ===== Expressions =====

  private Expr expression() {
      return assignment();
  }

  private Expr ternary() {
    Expr expr = equality();

    if (match(QUESTION)) {
      // middle branch can be any expression
      Expr thenBranch = expression();

      consume(COLON, "Expect ':' after then branch of ternary operator.");

      // right branch calls ternary() for right-associativity
      Expr elseBranch = ternary();

      expr = new Expr.Ternary(expr, thenBranch, elseBranch);
    }

    return expr;
  }

  private Expr equality() {

    // Always at least one comparison expression
    Expr expr = comparison();

    // Then zero or more sequences of "!=" | "==" comparison
    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      // If there are sequences, put our current expr as the
      // first argument of the new expr to "chain" it.
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr comparison() {
    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr term() {
    Expr expr = factor();

    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr factor() {
    Expr expr = unary();

    while (match(SLASH, STAR)) {
      Token operator = previous();

      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr unary() {

    // If the token is ! or -, it must be a unary expression
    if (match(BANG, MINUS)) {
      Token operator = previous();
      
      Expr right = unary();

      return new Expr.Unary(operator, right);
    }

    // Otherwise we ascend the precedence tree
    return primary();
  }

  private Expr primary() {

    // === Catch productions of binary operators missing left operand
    if (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      equality();
      throw error(operator, "Missing left-hand operand.");
    }

    if (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      comparison();
      throw error(operator, "Missing left-hand operand.");
    }

    if (match(PLUS)) {
      Token operator = previous();
      term();
      throw error(operator, "Missing left-hand operand.");
    }

    if (match(SLASH, STAR)) {
      Token operator = previous();
      factor();
      throw error(operator, "Missing left-hand operand.");
    }

    // === End catch

    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match (IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expect expression.");
  }

  // =======================

  // Helper methods

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }

  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();

    throw error(peek(), message);
  }

  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  // Method to escape errors and synchronise
  // Discard tokens until it thinks it finds a statement boundary
  private void synchronise() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }
}