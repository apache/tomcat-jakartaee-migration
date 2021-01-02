/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tomcat.jakartaee;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.EmptyVisitor;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

public class XmlExclusionListBuilder extends EmptyVisitor {
    private final ConstantPoolGen constantPoolGen;
    private final Set<String> stayInJavax = new HashSet<>();

    public XmlExclusionListBuilder(final ConstantPoolGen constantPoolGen) {
        this.constantPoolGen = constantPoolGen;
    }

    public Set<String> getStayInJavax() {
        return stayInJavax;
    }

    @Override
    public void visitJavaClass(final JavaClass obj) {
        Stream.of(obj.getMethods()).forEach(method -> method.accept(this));
    }

    // note: this algorithm is not 100% good but it is way better than just using a blind regex
    //       (guess it follows Pareto law)
    @Override
    public void visitMethod(final Method obj) {
        stayInJavax.addAll(Stream.of(new InstructionList(obj.getCode().getCode()).getInstructions())
                .filter(it -> it.getOpcode() == Const.GETSTATIC)
                .map(GETSTATIC.class::cast)
                .flatMap(it -> {
                    final ReferenceType clazz = it.getReferenceType(constantPoolGen);
                    final Type type = it.getFieldType(constantPoolGen);
                    if (getClassName(clazz).startsWith("javax.xml.xpath.") &&
                            getClassName(type).startsWith("javax.xml."/*namespace likey*/)) {
                        return Stream.of(getClassName(type));
                    }
                    return Stream.empty();
                })
                .collect(toSet()));
    }

    private String getClassName(final Type clazz) {
        return ObjectType.class.isInstance(clazz) ? ObjectType.class.cast(clazz).getClassName() : "";
    }
}
