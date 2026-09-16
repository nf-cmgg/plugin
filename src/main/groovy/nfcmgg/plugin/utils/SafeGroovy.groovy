/*
 * Copyright 2026, Center for Medical Genetics Ghent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nfcmgg.plugin.utils

import groovy.transform.CompileStatic

import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.SecureASTCustomizer

/**
 * Evaluate trusted worksheet expressions with a restricted Groovy compiler.
 * Allows property access, basic operators, and a fixed set of method names
 * on common value types. Disallows imports, closures, method definitions,
 * constructors, and static method calls.
 */
@CompileStatic
class SafeGroovy {

    static GroovyShell shell(Binding binding) {
        return new GroovyShell(binding, CONFIG)
    }

    static void parse(String expression) {
        new GroovyShell(CONFIG).parse(expression)
    }

    static Object evaluate(String expression, Binding binding) {
        return shell(binding).evaluate(expression)
    }

    private static final Set<String> ALLOWED_METHODS = [
        'toLowerCase',
        'toUpperCase',
        'trim',
        'strip',
        'contains',
        'startsWith',
        'endsWith',
        'matches',
        'replace',
        'replaceAll',
        'substring',
        'size',
        'length',
        'isEmpty',
        'toLong',
        'toInteger',
        'toDouble',
        'toFloat',
        'toBoolean',
        'toBigDecimal',
        'toString',
        'equals',
        'compareTo',
        'abs',
        'getAt',
        'get',
        'asBoolean',
        'asType',
    ] as Set

    private static final CompilerConfiguration CONFIG = makeConfig()

    private static CompilerConfiguration makeConfig() {
        final SecureASTCustomizer secure = new SecureASTCustomizer()
        secure.methodDefinitionAllowed = false
        secure.packageAllowed = false
        // Rely on receiversClassesWhiteList for method/ctor checks; empty import
        // whitelists still block explicit import statements.
        secure.indirectImportCheckEnabled = false
        secure.importsWhitelist = []
        secure.starImportsWhitelist = []
        secure.staticImportsWhitelist = []
        secure.staticStarImportsWhitelist = []
        // Object is required for dynamically typed property chains like input.tag.toLowerCase()
        secure.receiversClassesWhiteList = [
            Object,
            Script,
            String,
            GString,
            Boolean,
            Number,
            Integer,
            Long,
            Short,
            Byte,
            Float,
            Double,
            BigDecimal,
            BigInteger,
            Character,
            Map,
            LinkedHashMap,
            HashMap,
            List,
            ArrayList,
            Set,
            LinkedHashSet,
            HashSet,
            Collection,
        ]
        secure.addExpressionCheckers({ Expression expr ->
            if (expr in MethodCallExpression) {
                return ALLOWED_METHODS.contains(((MethodCallExpression) expr).methodAsString)
            }
            // Worksheet expressions do not need `new ...` or static calls like System.exit
            return !(expr in ConstructorCallExpression || expr in StaticMethodCallExpression)
        } as SecureASTCustomizer.ExpressionChecker)
        final CompilerConfiguration config = new CompilerConfiguration()
        config.addCompilationCustomizers(secure)
        return config
    }

}
