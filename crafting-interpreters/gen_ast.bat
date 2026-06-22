@echo off
echo Compiling GenerateAst...
javac -d bin src/com/craftinginterpreters/tool/GenerateAst.java

echo Running GenerateAst...
java -cp bin com.craftinginterpreters.tool.GenerateAst src/com/craftinginterpreters/lox

echo AST Generation Complete