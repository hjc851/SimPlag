package me.haydencheers.simplag

import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTNode
import org.eclipse.jdt.core.dom.ASTParser
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path

object Parser {
    fun parse(path: Path): ASTNode {

        if (path.toString().contains("SENG2050_A1_2017_1AE653D8257DF9FB6CFF81F4619628BB"))
            Unit

        val buffer = StringBuilder()
        Files.lines(path, Charset.forName("ISO-8859-1"))
            .forEach { buffer.appendln(it) }

        // Convert all // and /* to /** (to make sure they dont dissapear)
        var i = 0
        var max = buffer.length - 2
        while (i < max) {
            if (buffer[i] == '/' && buffer[i + 1] == '/') {
                val start = i
                var end = i + 2

                while (true) {
                    while (end < max) {
                        if (buffer[end] == '\n')
                            break
                        else
                            end++
                    }

                    end++
                    if (end + 2 < max && buffer[end + 1] == '/' && buffer[end + 2] == '/')
                        continue
                    else
                        break
                }

                val substr = "/**" + buffer.substring(start, end - 1)
                    .replace("//", "") + "*/"

                buffer.replace(start, end, substr)
                max = buffer.length - 2
                i = end
            }

            if (buffer[i] == '/' && buffer[i + 1] == '*' && buffer[i + 2] != '*') {
                buffer.insert(i + 2, '*')
                var str = buffer.substring(i, i + 4)
                max = buffer.length - 2
                i++
            }

            i++
        }

        val src = buffer.toString()

        val parser = ASTParser.newParser(AST.JLS13)
        parser.setKind(ASTParser.K_COMPILATION_UNIT)
        parser.setSource(src.toCharArray())
        val ast = parser.createAST(null)

        return ast
    }
}