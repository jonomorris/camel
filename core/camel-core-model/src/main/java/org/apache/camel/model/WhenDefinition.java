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
package org.apache.camel.model;

import jakarta.xml.bind.annotation.XmlRootElement;

import org.apache.camel.Predicate;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.spi.AsPredicate;
import org.apache.camel.spi.Metadata;

/**
 * Triggers a route when the expression evaluates to true
 */
@Metadata(label = "eip,routing")
@AsPredicate
@XmlRootElement(name = "when")
public class WhenDefinition extends OutputExpressionNode {

    // TODO: Make special for <choice>

    public WhenDefinition() {
    }

    protected WhenDefinition(WhenDefinition source) {
        super(source);
    }

    public WhenDefinition(Predicate predicate) {
        super(predicate);
    }

    public WhenDefinition(ExpressionDefinition expression) {
        super(expression);
    }

    @Override
    public WhenDefinition copyDefinition() {
        return new WhenDefinition(this);
    }

    @Override
    public String toString() {
        return "When[" + description() + " -> " + getOutputs() + "]";
    }

    protected String description() {
        StringBuilder sb = new StringBuilder(256);
        if (getExpression() != null) {
            String language = getExpression().getLanguage();
            if (language != null) {
                sb.append(language).append("{");
            }
            sb.append(getExpression().getLabel());
            if (language != null) {
                sb.append("}");
            }
        }
        return sb.toString();
    }

    @Override
    public String getShortName() {
        return "when";
    }

    @Override
    public String getLabel() {
        return "when[" + description() + "]";
    }

    /**
     * Expression used as the predicate to evaluate whether this when should trigger and route the message or not.
     */
    @Override
    public void setExpression(ExpressionDefinition expression) {
        // override to include javadoc what the expression is used for
        super.setExpression(expression);
    }

    @Override
    public ProcessorDefinition<?> endParent() {
        // when using when in the DSL we don't want to end back to this when,
        // but instead
        // the parent of this, so return the parent
        return this.getParent();
    }
}
