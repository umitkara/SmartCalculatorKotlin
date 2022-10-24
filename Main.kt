/**
 * A REPL calculator implementation.
 * All basic operations and variables are supported.
 * Variable names should be only letters.
 * @author Ümit Kara
 */
package calculator

import kotlin.math.pow
import kotlin.system.exitProcess
import java.math.BigInteger

var PRINT_FLAG = true

fun main() {
    while (true) {
        try {
            // print("Enter an expression: ")
            val input = readln()
            if (input.isEmpty()) {
                continue
            }
            val tokens = Lexer.tokenize(input)
            // Debug print tokens
            // println(tokens)
            val parsed = Parser.parse(tokens)
            // Debug print AST (Expression Tree)
            // println(parsed)
            val result = Evaluator.eval(parsed)
            if (PRINT_FLAG) {
                println(result)
            } else {
                PRINT_FLAG = true
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }
}

/**
 * Enum class to represent the different type of inputs as tokens.
 */
enum class TokenType {
    NUMBER,
    PLUS,
    MINUS,
    MUL,
    DIV,
    POW,
    LPAREN,
    RPAREN,
    ASSIGN,
    VARIABLE,
    EOF,
    COMMAND
}

/**
 * Enum class to represent different operations as expression tree nodes
 */
enum class NodeType {
    NUMBER,
    PLUS,
    MINUS,
    MUL,
    DIV,
    POW,
    VARIABLE,
    ASSIGN,
    UNARY_MINUS,
    UNARY_PLUS,
    COMMAND
}

/**
 * Class to represent a token.
 * @param type Type of the token
 * @param value Value of the token
 */
data class Token(val type: TokenType, val value: String) {
    override fun toString(): String {
        return "Token($type, $value)"
    }
}

/**
 * Class to represent a node in the expression tree.
 * @param type Type of the node
 * @param value Value of the node
 * @param left Left child of the node
 * @param right Right child of the node
 */
class Node(val type: NodeType, val value: String) {
    var left: Node? = null
    var right: Node? = null

    override fun toString(): String {
        // TODO: Extend capability of to string function. Return different string for different types of nodes.
        return "Node(type=$type, value=$value, left=$left, right=$right)"
    }
}

/**
 * Lexer or Tokenizer object. This object gets the input string and
 * converts it into a list of tokens.
 */
object Lexer {
    private var text: String = ""
    private var pos = 0
    private var currentChar: Char? = null
    private var parenthesesCount = 0

    /**
     * Function to create a list of tokens from the input string.
     * @param text Input expression string
     * @return List of tokens
     */
    fun tokenize(text: String): List<Token> {
        // TODO: Throw exception if the input contains invalid variable names, also throw exception if the input is an invalid expression.
        this.text = text
        pos = 0
        currentChar = text[pos]
        val tokens = mutableListOf<Token>()
        while (currentChar != null) {
            if (currentChar!!.isWhitespace()) {
                skipWhitespace()
                continue
            }
            if (currentChar!!.isDigit()) {
                // throw exception if previous token is a letter
                if (tokens.isNotEmpty() && tokens.last().type == TokenType.VARIABLE) {
                    // if any previous token is an assignment, then throw Invalid assignment exception
                    if (tokens.any { it.type == TokenType.ASSIGN }) {
                        throw InvalidAssignmentException("Invalid assignment")
                    } else {
                        throw InvalidIdentifierException("Invalid identifier")
                    }
                }
                tokens.add(number())
                continue
            }
            if (currentChar!!.isLetter()) {
                // throw exception if previous token is a number
                if (tokens.isNotEmpty() && tokens.last().type == TokenType.NUMBER) {
                    // if any previous token is an assignment, then throw Invalid assignment exception
                    if (tokens.any { it.type == TokenType.ASSIGN }) {
                        throw InvalidAssignmentException("Invalid assignment")
                    } else {
                        throw InvalidIdentifierException("Invalid identifier")
                    }
                }
                val newVariable = variable()
                // if previous token is a DIV, and DIV token is the first token
                if (tokens.isNotEmpty() && tokens.last().type == TokenType.DIV && tokens.size == 1) {
                    // if the variable is not a command, then throw Invalid assignment exception
                    if (newVariable.value !in listOf("exit", "help")) {
                        throw UnknownCommandException("Unknown command")
                    } else {
                        // remove the DIV token
                        tokens.removeAt(tokens.lastIndex)
                        // add the new command token
                        tokens.add(Token(TokenType.COMMAND, newVariable.value))
                    }
                } else {
                    tokens.add(newVariable)
                }
                //tokens.add(variable())
                continue
            }
            when (currentChar) {
                '+' -> {
                    tokens.add(Token(TokenType.PLUS, "+"))
                    advance()
                }

                '-' -> {
                    tokens.add(Token(TokenType.MINUS, "-"))
                    advance()
                }

                '*' -> {
                    tokens.add(Token(TokenType.MUL, "*"))
                    advance()
                }

                '/' -> {
                    tokens.add(Token(TokenType.DIV, "/"))
                    advance()
                }

                '(' -> {
                    tokens.add(Token(TokenType.LPAREN, "("))
                    parenthesesCount++
                    advance()
                }

                ')' -> {
                    tokens.add(Token(TokenType.RPAREN, ")"))
                    parenthesesCount--
                    advance()
                }

                '=' -> {
                    // if any previous token is an assignment, then throw Invalid assignment exception
                    if (tokens.any { it.type == TokenType.ASSIGN }) {
                        throw InvalidAssignmentException("Invalid assignment")
                    }
                    tokens.add(Token(TokenType.ASSIGN, "="))
                    advance()
                }

                '^' -> {
                    tokens.add(Token(TokenType.POW, "^"))
                    advance()
                }

                else -> {
                    throw Exception("Invalid character: $currentChar")
                }
            }
        }
        tokens.add(Token(TokenType.EOF, ""))
        if (parenthesesCount != 0) {
            parenthesesCount = 0
            throw InvalidExpressionException("Invalid expression")
        }
        return tokens
    }

    private fun advance() {
        pos++
        currentChar = if (pos > text.length - 1) {
            null
        } else {
            text[pos]
        }
    }

    private fun skipWhitespace() {
        while (currentChar != null && currentChar!!.isWhitespace()) {
            advance()
        }
    }

    private fun number(): Token {
        var result = ""
        while (currentChar != null && currentChar!!.isDigit()) {
            result += currentChar
            advance()
        }
        return Token(TokenType.NUMBER, result)
    }

    private fun variable(): Token {
        var result = ""
        while (currentChar != null && currentChar!!.isLetter()) {
            // match the variable name with the regex
            if (currentChar!!.toString().matches(Regex("[a-zA-Z]"))) {
                result += currentChar
                advance()
            } else {
                throw InvalidIdentifierException("Invalid identifier")
            }
        }
        return Token(TokenType.VARIABLE, result)
    }
}

object Parser {
    private var tokens = listOf<Token>()
    private var pos = 0
    private var currentToken: Token? = null

    /**
     * Function to parse the list of tokens and create an expression tree.
     * @param tokens List of tokens
     * @return Root node of the expression tree
     */
    fun parse(tokens: List<Token>): Node {
        this.tokens = tokens
        pos = 0
        currentToken = this.tokens[pos]
        return expr()
    }

    private fun advance() {
        pos++
        currentToken = if (pos > tokens.size - 1) {
            null
        } else {
            tokens[pos]
        }
    }

    private fun expr(): Node {
        var node = term()
        while (currentToken != null && (currentToken!!.type == TokenType.PLUS || currentToken!!.type == TokenType.MINUS)) {
            val token = currentToken
            if (token!!.type == TokenType.PLUS) {
                advance()
                node = Node(NodeType.PLUS, "+").apply {
                    left = node
                    right = term()
                }
            } else if (token.type == TokenType.MINUS) {
                advance()
                node = Node(NodeType.MINUS, "-").apply {
                    left = node
                    right = term()
                }
            }
        }
        return node
    }

    private fun term(): Node {
        var node = factor()
        while (currentToken != null && (currentToken!!.type == TokenType.MUL || currentToken!!.type == TokenType.DIV) || currentToken!!.type == TokenType.POW) {
            val token = currentToken
            if (token!!.type == TokenType.MUL) {
                advance()
                node = Node(NodeType.MUL, "*").apply {
                    left = node
                    right = factor()
                }
            } else if (token.type == TokenType.DIV) {
                advance()
                node = Node(NodeType.DIV, "/").apply {
                    left = node
                    right = factor()
                }
            } else if (token.type == TokenType.POW) {
                advance()
                node = Node(NodeType.POW, "^").apply {
                    left = node
                    right = factor()
                }
            }
        }
        return node
    }

    private fun factor(): Node {
        val token = currentToken
        return when {
            token!!.type == TokenType.NUMBER -> {
                advance()
                Node(NodeType.NUMBER, token.value)
            }

            token.type == TokenType.VARIABLE -> {
                advance()
                //Node(NodeType.VARIABLE, token.value)
                if (currentToken != null && currentToken!!.type == TokenType.ASSIGN) {
                    advance()
                    Node(NodeType.ASSIGN, "=").apply {
                        left = Node(NodeType.VARIABLE, token.value)
                        right = expr()
                    }
                } else {
                    Node(NodeType.VARIABLE, token.value)
                }
            }

            token.type == TokenType.ASSIGN -> {
                advance()
                Node(NodeType.ASSIGN, token.value).apply {
                    left = factor()
                    right = expr()
                }
            }

            token.type == TokenType.LPAREN -> {
                advance()
                val node = expr()
                if (currentToken!!.type != TokenType.RPAREN) {
                    throw InvalidExpressionException("Invalid expression")
                }
                advance()
                node
            }

            token.type == TokenType.PLUS -> {
                advance()
                Node(NodeType.UNARY_PLUS, "+").apply {
                    right = factor()
                }
            }

            token.type == TokenType.MINUS -> {
                advance()
                Node(NodeType.UNARY_MINUS, "-").apply {
                    right = factor()
                }
            }

            token.type == TokenType.COMMAND -> {
                advance()
                Node(NodeType.COMMAND, token.value)
            }

            else -> {
                throw Exception("Invalid syntax")
            }
        }
    }
}

// TODO: Instead of returning and Integer in eval function, return a generic type, Also throw and catch exceptions based on specs
object Evaluator {
    private var variables = mutableMapOf<String, BigInteger>()

    /**
     * Function to interpret the expression tree and return the result.
     * @param node Root node of the expression tree
     * @return Result of the expression
     */
    fun eval(node: Node): BigInteger {
        return when (node.type) {
            NodeType.NUMBER -> {
                BigInteger(node.value)
            }
            NodeType.VARIABLE -> {
                if (variables.containsKey(node.value)) {
                    variables[node.value]!!
                } else {
                    throw UnknownVariableException("Unknown variable")
                }
            }

            NodeType.ASSIGN -> {
                // if right node is variable, check if it is assigned a value, if not throw exception
                if (node.right!!.type == NodeType.VARIABLE && variables[node.right!!.value] == null) {
                    throw UnknownVariableException("Unknown variable")
                }
                PRINT_FLAG = false
                variables[node.left!!.value] = eval(node.right!!)
                variables[node.left!!.value] ?: BigInteger.ZERO
            }

            NodeType.COMMAND -> {
                when (node.value) {
                    "help" -> {
                        helpCommand()
                        PRINT_FLAG = false
                        return BigInteger.ZERO
                    }

                    "exit" -> {
                        exitCommand()
                        return BigInteger.ZERO
                    }

                    else -> throw UnknownCommandException("Unknown command")
                }
            }

            NodeType.PLUS -> eval(node.left!!) + eval(node.right!!)
            NodeType.MINUS -> eval(node.left!!) - eval(node.right!!)
            NodeType.MUL -> eval(node.left!!) * eval(node.right!!)
            NodeType.DIV -> eval(node.left!!) / eval(node.right!!)
            NodeType.POW -> eval(node.left!!).pow(eval(node.right!!).toInt())
            NodeType.UNARY_PLUS -> eval(node.right!!)
            NodeType.UNARY_MINUS -> -eval(node.right!!)
        }
    }

    private fun helpCommand() {
        println("Commands:")
        println("help - Display this help message")
        println("exit - Exit the program")
    }

    private fun exitCommand() {
        println("Bye!")
        exitProcess(0)
    }

    fun printVariables() {
        println("Variables:")
        variables.forEach { (key, value) ->
            println("$key = $value")
        }
    }
}

class InvalidIdentifierException(message: String) : Exception(message)

class InvalidAssignmentException(message: String) : Exception(message)

class UnknownVariableException(message: String) : Exception(message)

class UnknownCommandException(message: String) : Exception(message)

class InvalidExpressionException(message: String) : Exception(message)