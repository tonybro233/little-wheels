package io.github.tonybro233.littlewheels.web;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.Conventions;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @see org.springframework.web.method.annotation.RequestParamMethodArgumentResolver
 * @see org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor
 * @see org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter
 *
 * @author tony
 */
public class RequestBodyParamArgumentResolver implements HandlerMethodArgumentResolver {

    private ObjectMapper objectMapper;

    private static final String ROOT_ATTR_NAME = "request.body.root";

    @Nullable
    private final ConfigurableBeanFactory configurableBeanFactory;

    @Nullable
    private final BeanExpressionContext expressionContext;

    private final Map<MethodParameter, NamedValueInfo> namedValueInfoCache = new ConcurrentHashMap<>(256);

    private final Map<String, JsonPointer> jsonPointerCache = new ConcurrentHashMap<>(256);

    public RequestBodyParamArgumentResolver(ObjectMapper objectMapper,
                                            @Nullable ConfigurableBeanFactory beanFactory) {
        this.configurableBeanFactory = beanFactory;
        this.objectMapper = objectMapper;
        this.expressionContext =
                (beanFactory != null ? new BeanExpressionContext(beanFactory, new RequestScope()) : null);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestBodyParam.class);
    }

    @Override
    public final Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
                                        NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
        NamedValueInfo namedValueInfo = getNamedValueInfo(parameter);
        MethodParameter nestedParameter = parameter.nestedIfOptional();

        Object resolvedName = resolveStringValue(namedValueInfo.name);
        if (resolvedName == null) {
            throw new IllegalArgumentException(
                    "Specified name must not resolve to null: [" + namedValueInfo.name + "]");
        }

        Object arg = resolveName(resolvedName.toString(), nestedParameter, webRequest);
        if (arg == null) {
            if (namedValueInfo.defaultValue != null) {
                arg = resolveStringValue(namedValueInfo.defaultValue);
            }
            else if (namedValueInfo.required && !nestedParameter.isOptional()) {
                handleMissingValue(namedValueInfo.name, nestedParameter, webRequest);
            }
            arg = handleNullValue(namedValueInfo.name, arg, nestedParameter.getNestedParameterType());
        }
        else if ("".equals(arg) && namedValueInfo.defaultValue != null) {
            arg = resolveStringValue(namedValueInfo.defaultValue);
        }

        if (binderFactory != null) {
            String name = Conventions.getVariableNameForParameter(parameter);
            WebDataBinder binder = binderFactory.createBinder(webRequest, arg, name);
            if (arg != null) {
                validateIfApplicable(binder, parameter);
                if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
                    throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
                }
            }
            if (mavContainer != null) {
                mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
            }
        }

        return arg;
    }

    /**
     * Validate the binding target if applicable.
     * <p>The default implementation checks for {@code @javax.validation.Valid},
     * Spring's {@link org.springframework.validation.annotation.Validated},
     * and custom annotations whose name starts with "Valid".
     * @param binder the DataBinder to be used
     * @param parameter the method parameter descriptor
     * @since 4.1.5
     * @see #isBindExceptionRequired
     */
    protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
        Annotation[] annotations = parameter.getParameterAnnotations();
        for (Annotation ann : annotations) {
            Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
            if (validatedAnn != null || ann.annotationType().getSimpleName().startsWith("Valid")) {
                Object hints = (validatedAnn != null ? validatedAnn.value() : AnnotationUtils.getValue(ann));
                Object[] validationHints = (hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
                binder.validate(validationHints);
                break;
            }
        }
    }

    /**
     * Whether to raise a fatal bind exception on validation errors.
     * @param binder the data binder used to perform data binding
     * @param parameter the method parameter descriptor
     * @return {@code true} if the next method argument is not of type {@link Errors}
     * @since 4.1.5
     */
    protected boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
        int i = parameter.getParameterIndex();
        Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
        boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
        return !hasBindingResult;
    }

    /**
     * Resolve the given parameter type and value name into an argument value.
     * @param name the name of the value being resolved
     * @param parameter the method parameter to resolve to an argument value
     * (pre-nested in case of a {@link java.util.Optional} declaration)
     * @param request the current request
     * @return the resolved argument (may be {@code null})
     * @throws Exception in case of errors
     */
    protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
        HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
        Assert.state(servletRequest != null, "No HttpServletRequest");

        JsonNode root = (JsonNode) servletRequest.getAttribute(ROOT_ATTR_NAME);
        if (null == root) {
            try {
                root = objectMapper.readTree(servletRequest.getInputStream());
            } catch (JsonParseException ex) {
                throw new HttpMessageNotReadableException("JSON parse error: " + ex.getOriginalMessage(),
                        ex, new ServletServerHttpRequest(servletRequest));
            }
            servletRequest.setAttribute(ROOT_ATTR_NAME, root);
        }

        Class<?> contextClass = parameter.getContainingClass();
        Type targetType = parameter.getNestedGenericParameterType();
        JavaType javaType = getJavaType(targetType, contextClass);

        // support RFC 6901
        JsonNode targetNode = name.startsWith("/") ?
                root.at(jsonPointerCache.computeIfAbsent(name, JsonPointer::compile)) :
                root.path(name);
        JsonNodeType nodeType = targetNode.getNodeType();
        Object arg = null;

        switch (nodeType) {
            case OBJECT:
            case ARRAY:
            case NUMBER:
            case BOOLEAN:
            case STRING:
                arg = readJavaType(javaType, targetNode, name, parameter);
                break;
            case MISSING:
            case NULL:
                // empty
                break;
            case BINARY:
            case POJO:
                throw new IllegalStateException("Unsupported JsonNode Type: " + nodeType);
            default:
                throw new IllegalStateException("Unhandled JsonNode Type: " + nodeType);
        }

        return arg;
    }

    private Object readJavaType(JavaType javaType, JsonNode node, String name, MethodParameter parameter) throws ServletException {
        try {
            return this.objectMapper.convertValue(node, javaType);
        } catch (IllegalArgumentException ex) {
            throw new ServletRequestBindingException(String.format(
                    "Cannot bind argument '%s' for method parameter of type %s as JSON parsed failed: %s",
                    name, parameter.getNestedParameterType().getSimpleName(), ex.getMessage()), ex);
        }

    }

    /**
     * count number of {@link RequestBodyParam} annotation in method parameters
     */
    private int countAnnotatedParam(MethodParameter parameter) {
        int count = 0;
        if (null == parameter.getMethod()) {
            throw new IllegalArgumentException("Cannot bind to constructor parameter");
        }
        for (Parameter param : parameter.getMethod().getParameters()) {
            if (null != param.getAnnotation(RequestBodyParam.class)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Return the Jackson {@link JavaType} for the specified type and context class.
     * @param type the generic type to return the Jackson JavaType for
     * @param contextClass a context class for the target type, for example a class
     * in which the target type appears in a method signature (can be {@code null})
     * @return the Jackson JavaType
     */
    protected JavaType getJavaType(Type type, @Nullable Class<?> contextClass) {
        TypeFactory typeFactory = this.objectMapper.getTypeFactory();
        return typeFactory.constructType(GenericTypeResolver.resolveType(type, contextClass));
    }


    /**
     * A {@code null} results in a {@code false} value for {@code boolean}s or an exception for other primitives.
     */
    private Object handleNullValue(String name, @Nullable Object value, Class<?> paramType) {
        if (value == null) {
            if (Boolean.TYPE.equals(paramType)) {
                return Boolean.FALSE;
            }
            else if (paramType.isPrimitive()) {
                throw new IllegalStateException("Optional " + paramType.getSimpleName() + " parameter '" + name +
                        "' is present but cannot be translated into a null value due to being declared as a " +
                        "primitive type. Consider declaring it as object wrapper for the corresponding primitive type.");
            }
        }
        return value;
    }

    /**
     * Invoked when a named value is required, but {@link #resolveName(String, MethodParameter, NativeWebRequest)}
     * returned {@code null} and there is no default value. Subclasses typically throw an exception in this case.
     * @param name the name for the value
     * @param parameter the method parameter
     * @param request the current request
     * @since 4.3
     */
    protected void handleMissingValue(String name, MethodParameter parameter, NativeWebRequest request)
            throws Exception {

        handleMissingValue(name, parameter);
    }

    /**
     * Invoked when a named value is required, but {@link #resolveName(String, MethodParameter, NativeWebRequest)}
     * returned {@code null} and there is no default value. Subclasses typically throw an exception in this case.
     * @param name the name for the value
     * @param parameter the method parameter
     */
    protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
        throw new ServletRequestBindingException("Missing argument '" + name +
                "' for method parameter of type " + parameter.getNestedParameterType().getSimpleName());
    }

    /**
     * Resolve the given annotation-specified value,
     * potentially containing placeholders and expressions.
     */
    private Object resolveStringValue(String value) {
        if (this.configurableBeanFactory == null) {
            return value;
        }
        String placeholdersResolved = this.configurableBeanFactory.resolveEmbeddedValue(value);
        BeanExpressionResolver exprResolver = this.configurableBeanFactory.getBeanExpressionResolver();
        if (exprResolver == null || this.expressionContext == null) {
            return value;
        }
        return exprResolver.evaluate(placeholdersResolved, this.expressionContext);
    }

    /**
     * Obtain the named value for the given method parameter.
     */
    protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
        RequestBodyParam ann = parameter.getParameterAnnotation(RequestBodyParam.class);
        return new NamedValueInfo(ann.name(), ann.required(), ann.defaultValue());
    }

    /**
     * Create the {@link NamedValueInfo} object for the given method parameter. Implementations typically
     * retrieve the method annotation by means of {@link MethodParameter#getParameterAnnotation(Class)}.
     * @param parameter the method parameter
     * @return the named value information
     */
    private NamedValueInfo getNamedValueInfo(MethodParameter parameter) {
        NamedValueInfo namedValueInfo = this.namedValueInfoCache.get(parameter);
        if (namedValueInfo == null) {
            namedValueInfo = createNamedValueInfo(parameter);
            namedValueInfo = updateNamedValueInfo(parameter, namedValueInfo);
            this.namedValueInfoCache.put(parameter, namedValueInfo);
        }
        return namedValueInfo;
    }

    /**
     * Create a new NamedValueInfo based on the given NamedValueInfo with sanitized values.
     */
    private NamedValueInfo updateNamedValueInfo(MethodParameter parameter, NamedValueInfo info) {
        String name = info.name;
        if (info.name.isEmpty()) {
            name = parameter.getParameterName();
            if (name == null) {
                throw new IllegalArgumentException(
                        "Name for argument type [" + parameter.getNestedParameterType().getName() +
                                "] not available, and parameter name information not found in class file either.");
            }
        }
        String defaultValue = (ValueConstants.DEFAULT_NONE.equals(info.defaultValue) ? null : info.defaultValue);
        return new NamedValueInfo(name, info.required, defaultValue);
    }


    /**
     * Represents the information about a named value, including name, whether it's required and a default value.
     */
    protected static class NamedValueInfo {

        private final String name;

        private final boolean required;

        @Nullable
        private final String defaultValue;

        public NamedValueInfo(String name, boolean required, @Nullable String defaultValue) {
            this.name = name;
            this.required = required;
            this.defaultValue = defaultValue;
        }
    }
}
