package io.github.tonybro233.littlewheels.web;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.ValueConstants;

import java.lang.annotation.*;

/**
 * Annotation which indicates that a method parameter should be bound to
 * a sub-value(child node) of json-formatted web request body. The body
 * of the request is parsed into tree expressed data, then traversal to
 * the specified child node by name and resolve the node value to the method
 * argument. Optionally, automatic validation can be applied by annotating
 * the argument with {@code @Valid} (if the argument is a POJO).
 *
 * @see org.springframework.web.bind.annotation.RequestParam
 * @see org.springframework.web.bind.annotation.RequestBody
 *
 * @author tony
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestBodyParam {

    /**
     * Alias for {@link #name}.
     */
    @AliasFor("name")
    String value() default "";

    /**
     * If name start with '/', resolver will compile it to a
     * <a href="http://tools.ietf.org/html/rfc6901">JSON Pointer</a>
     * (Used to locate specified JSON node), else resolver
     * will treat it as JSON root's direct child name.
     * <p>For example:
     * <pre>
     *     Given the JSON body:
     *
     *     {
     *       "order" : {
     *         "id": 123,
     *         "amount" : 999.99,
     *         "buyer": ["Jack", "Tom", "Alice"]
     *       },
     *       "foo": ["bar", "baz"],
     *       "": 0,
     *       "a/b": 1,
     *       "c%d": 2,
     *       "e^f": 3,
     *       "g|h": 4,
     *       "i\\j": 5,
     *       "k\"l": 6,
     *       " ": 7,
     *       "m~n": 8
     *    }
     *
     *    The following name evaluate to the accompanying values:
     *
     *     "order"              { "id": 123, ... }
     *     "foo"                ["bar", "baz"]
     *     "/order/amount"      999.99
     *     "/order/buyer/0"     "Jack"
     *     "/foo"               ["bar", "baz"]
     *     "/foo/0"             "bar"
     *     "/"                  0
     *     "/a~1b"              1
     *     "/c%d"               2
     *     "/e^f"               3
     *     "/g|h"               4
     *     "/i\\j"              5
     *     "/k\"l"              6
     *     "/ "                 7
     *     "/m~0n"              8
     *
     * </pre>
     */
    @AliasFor("value")
    String name() default "";

    /**
     * Whether the parameter is required.
     * <p>Defaults to {@code true}, leading to an exception being thrown
     * if the parameter is missing in the request body. Switch this to
     * {@code false} if you prefer a {@code null} value if the parameter is
     * not present in the request.
     * <p>Alternatively, provide a {@link #defaultValue}, which implicitly
     * sets this flag to {@code false}.
     */
    boolean required() default true;

    /**
     * The default value to use as a fallback when the request parameter is
     * not provided or has an empty value.
     * <p>Supplying a default value implicitly sets {@link #required} to
     * {@code false}.
     */
    String defaultValue() default ValueConstants.DEFAULT_NONE;

}
