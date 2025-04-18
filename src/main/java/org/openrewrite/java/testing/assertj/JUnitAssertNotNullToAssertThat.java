/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.testing.assertj;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.List;

public class JUnitAssertNotNullToAssertThat extends Recipe {

    private static final MethodMatcher ASSERT_NOT_NULL_MATCHER = new MethodMatcher("org.junit.jupiter.api.Assertions assertNotNull(..)", true);

    @Override
    public String getDisplayName() {
        return "JUnit `assertNotNull` to AssertJ";
    }

    @Override
    public String getDescription() {
        return "Convert JUnit-style `assertNotNull()` to AssertJ's `assertThat().isNotNull()`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(ASSERT_NOT_NULL_MATCHER), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                if (!ASSERT_NOT_NULL_MATCHER.matches(mi)) {
                    return mi;
                }

                maybeAddImport("org.assertj.core.api.Assertions", "assertThat", false);
                maybeRemoveImport("org.junit.jupiter.api.Assertions");

                List<Expression> args = mi.getArguments();
                Expression actual = args.get(0);
                if (args.size() == 1) {
                    return JavaTemplate.builder("assertThat(#{any()}).isNotNull();")
                            .staticImports("org.assertj.core.api.Assertions.assertThat")
                            .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "assertj-core-3"))
                            .build()
                            .apply(getCursor(), mi.getCoordinates().replace(), actual);

                }

                Expression message = args.get(1);
                return JavaTemplate.builder("assertThat(#{any()}).as(#{any()}).isNotNull();")
                        .staticImports("org.assertj.core.api.Assertions.assertThat")
                        .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "assertj-core-3"))
                        .build()
                        .apply(getCursor(), mi.getCoordinates().replace(), actual, message);
            }
        });
    }
}
