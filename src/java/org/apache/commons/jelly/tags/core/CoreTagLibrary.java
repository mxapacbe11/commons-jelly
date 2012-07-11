/*
 * Copyright 2002,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jelly.tags.core;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.JellyException;
import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.TagLibrary;
import org.apache.commons.jelly.XMLOutput;
import org.apache.commons.jelly.impl.ExpressionAttribute;
import org.apache.commons.jelly.impl.TagScript;
import org.xml.sax.Attributes;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;

/**
  * This is the core tag library for jelly and contains commonly
  * used tags.
  * This class could be generated by XDoclet
  *
  * @author <a href="mailto:jstrachan@apache.org">James Strachan</a>
  * @version $Revision: 155420 $
  */
public class CoreTagLibrary extends TagLibrary {

    public CoreTagLibrary() {
        registerTag("jelly", JellyTag.class);

        // core tags
        registerTag("out", ExprTag.class);
        registerTag("catch", CatchTag.class);
        registerTag("forEach", ForEachTag.class);
        registerTag("set", SetTag.class);
        registerTag("remove", RemoveTag.class);
        registerTag("while", WhileTag.class);

        // conditional tags
        registerTag("if", IfTag.class);
        registerTag("choose", ChooseTag.class);
        registerTag("when", WhenTag.class);
        registerTag("otherwise", OtherwiseTag.class);
        registerTag("switch", SwitchTag.class);
        registerTag("case", CaseTag.class);
        registerTag("default", DefaultTag.class);

        // other tags
        registerTag("include", IncludeTag.class);
        registerTag("import", ImportTag.class);
        registerTag("mute", MuteTag.class);

        // extensions to JSTL
        registerTag("arg", ArgTag.class);
        registerTag("break", BreakTag.class);
        registerTag("expr", ExprTag.class);
        registerTag("file", FileTag.class);
        registerTag("getStatic", GetStaticTag.class);
        registerTag("invoke", InvokeTag.class);
        registerTag("invokeStatic", InvokeStaticTag.class);
        registerTag("new", NewTag.class);
        registerTag("parse", ParseTag.class);
        registerTag("scope", ScopeTag.class);
        registerTag("setProperties", SetPropertiesTag.class);
        registerTag("thread", ThreadTag.class);
        registerTag("useBean", UseBeanTag.class);
        registerTag("useList", UseListTag.class);
        registerTag("whitespace", WhitespaceTag.class);
    }

    @Override
    public TagScript createTagScript(String name, Attributes attributes) throws JellyException {
        //
        // in normal parse, these TagScript implementations are used in favor of the Tag implementations above,
        // making scripts run faster and with less memory.
        //

        if (name.equals("if"))
            return new TagScript() {
                public void run(JellyContext context, XMLOutput output) throws JellyTagException {
                    if (getAttribute("test").evaluateAsBoolean(context))
                        getTagBody().run(context,output);
                }
            };

        if (name.equals("jelly"))
            return new TagScript() {
                public void run(JellyContext context, XMLOutput output) throws JellyTagException {
                    getTagBody().run(context,output);
                }
            };

        if (name.equals("set")) {
            return new TagScript() {
                public void run(JellyContext context, XMLOutput output) throws JellyTagException {
                    String var = getAttribute("var").evaluateAsString(context);
                    Object target = getAttribute("target").evaluate(context);
                    String property = getAttribute("property").evaluateAsString(context);

                    // perform validation up front to fail fast
                    if (var != null) {
                        if (target != null || property != null) {
                            throw new JellyTagException("The 'target' and 'property' attributes cannot be used in combination with the 'var' attribute");
                        }
                    } else {
                        if (target == null) {
                            throw new JellyTagException("Either a 'var' or a 'target' attribute must be defined for this tag");
                        }
                        if (property == null) {
                            throw new JellyTagException("The 'target' attribute requires the 'property' attribute");
                        }
                    }

                    ExpressionAttribute value = attributes.get("value");

                    Object answer;
                    if (value != null) {
                        answer = value.exp.evaluate(context);
                        ExpressionAttribute defaultValue = attributes.get("defaultValue");

                        if (defaultValue != null && isEmpty(answer)) {
                            answer = defaultValue.exp.evaluate(context);
                        }
                    } else {
                        answer = getBodyText(context,getAttribute("encode").evaluateAsBoolean(context));
                    }

                    if (var != null) {
                        String scope = getAttribute("scope").evaluateAsString(context);
                        if (scope != null) {
                            context.setVariable(var, scope, answer);
                        } else {
                            context.setVariable(var, answer);
                        }
                    } else {
                        setPropertyValue(target, property, answer);
                    }
                }

                protected void setPropertyValue(Object target, String property, Object value) {
                    try {
                        if (target instanceof Map) {
                            Map map = (Map) target;
                            map.put(property, value);
                        } else {
                            BeanUtils.setProperty(target, property, value);
                        }
                    } catch (InvocationTargetException e) {
                        LOGGER.log(WARNING, "Failed to set the property: " + property + " on bean: " + target + " to value: " + value + " due to exception: " + e, e);
                    } catch (IllegalAccessException e) {
                        LOGGER.log(WARNING, "Failed to set the property: " + property + " on bean: " + target + " to value: " + value + " due to exception: " + e, e);
                    }
                }

                /**
                 * @param value
                 * @return true if the given value is null or an empty String
                 */
                protected boolean isEmpty(Object value) {
                    if (value == null) {
                        return true;
                    }
                    if (value instanceof String) {
                        String s = (String) value;
                        return s.length() == 0;
                    }
                    return false;
                }
            };
        }

        return super.createTagScript(name,attributes);
    }

    private static final Logger LOGGER = Logger.getLogger(CoreTagLibrary.class.getName());
}
