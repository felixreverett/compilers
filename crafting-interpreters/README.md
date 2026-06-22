# Crafting Interpreters

My code for the crafting interpreters compiler implementation book. The book is [freely available online](https://craftinginterpreters.com/contents.html) and guides you through the process of creating both an interpreter in Java and a full bytecode virtual machine in C.

## 1 Command Line Build Instructions

Ensure your current directory is set to crafting-interpreters/

`cd crafting-interpreters`

If it doesn't exist, create the `Expr.java` file by compiling and executing the GenerateAst script (or alternatively executing `./gen_ast.bat` if you are on Windows):

`javac -d bin src/com/craftinginterpreters/tool/GenerateAst.java`

`java -cp bin com.craftinginterpreters.tool.GenerateAst src/com/craftinginterpreters/lox`

Next, compile all files into bin using the directory flag -d:

`javac -d bin src/com/craftinginterpreters/lox/*.java`

Run REPL:

`java -cp bin com.craftinginterpreters.lox.Lox`