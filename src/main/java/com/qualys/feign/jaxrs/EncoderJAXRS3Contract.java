package com.qualys.feign.jaxrs;

import feign.AlwaysEncodeBodyContract;
import feign.MethodMetadata;
import feign.Request;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collections;

import static feign.Util.*;
import static feign.Util.removeValues;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;

/**
 * All this class is needed only for BeanParam support. This support is declared in feign-jakarta but not fully implemented.
 * This whole library can be removed once feign-jakarta fix BeanParam support.
 */
public class EncoderJAXRS3Contract extends AlwaysEncodeBodyContract {

    public EncoderJAXRS3Contract() {
        super.registerClassAnnotation(Path.class, (path, data) -> {
            if (path != null && !path.value().isEmpty()) {
                String pathValue = path.value();
                if (!pathValue.startsWith("/")) {
                    pathValue = "/" + pathValue;
                }
                if (pathValue.endsWith("/")) {
                    // Strip off any trailing slashes, since the template has already had slashes
                    // appropriately
                    // added
                    pathValue = pathValue.substring(0, pathValue.length() - 1);
                }
                // jax-rs allows whitespace around the param name, as well as an optional regex. The
                // contract
                // should
                // strip these out appropriately.
                pathValue = pathValue.replaceAll("\\{\\s*(.+?)\\s*(:.+?)?\\}", "\\{$1\\}");
                data.template().uri(pathValue);
            }
        });
        super.registerClassAnnotation(Consumes.class, this::handleConsumesAnnotation);
        super.registerClassAnnotation(Produces.class, this::handleProducesAnnotation);

        registerMethodAnnotation(methodAnnotation -> {
            final Class<? extends Annotation> annotationType = methodAnnotation.annotationType();
            final HttpMethod http = annotationType.getAnnotation(HttpMethod.class);
            return http != null;
        }, (methodAnnotation, data) -> {
            final Class<? extends Annotation> annotationType = methodAnnotation.annotationType();
            final HttpMethod http = annotationType.getAnnotation(HttpMethod.class);
            checkState(data.template().method() == null,
                    "Method %s contains multiple HTTP methods. Found: %s and %s", data.configKey(),
                    data.template().method(), http.value());
            data.template().method(Request.HttpMethod.valueOf(http.value()));
        });

        super.registerMethodAnnotation(Path.class, (path, data) -> {
            final String pathValue = emptyToNull(path.value());
            if (pathValue == null) {
                return;
            }
            String methodAnnotationValue = path.value();
            if (!methodAnnotationValue.startsWith("/") && !data.template().url().endsWith("/")) {
                methodAnnotationValue = "/" + methodAnnotationValue;
            }
            // jax-rs allows whitespace around the param name, as well as an optional regex. The contract
            // should
            // strip these out appropriately.
            methodAnnotationValue =
                    methodAnnotationValue.replaceAll("\\{\\s*(.+?)\\s*(:.+?)?\\}", "\\{$1\\}");
            data.template().uri(methodAnnotationValue, true);
        });
        super.registerMethodAnnotation(Consumes.class, this::handleConsumesAnnotation);
        super.registerMethodAnnotation(Produces.class, this::handleProducesAnnotation);

        // trying to minimize the diff
        registerParamAnnotations();


        // parameter with unsupported jax-rs annotations should not be passed as body params.
        // this will prevent interfaces from becoming unusable entirely due to single (unsupported)
        // endpoints.
        // https://github.com/OpenFeign/feign/issues/669
        super.registerParameterAnnotation(Suspended.class, (ann, data, i) -> data.ignoreParamater(i));
        super.registerParameterAnnotation(Context.class, (ann, data, i) -> data.ignoreParamater(i));
    }

    private void handleProducesAnnotation(Produces produces, MethodMetadata data) {
        final String[] serverProduces =
                removeValues(produces.value(), mediaType -> emptyToNull(mediaType) == null, String.class);
        checkState(serverProduces.length > 0, "Produces.value() was empty on %s", data.configKey());
        data.template().header(ACCEPT, Collections.emptyList()); // remove any previous produces
        data.template().header(ACCEPT, serverProduces);
    }

    private void handleConsumesAnnotation(Consumes consumes, MethodMetadata data) {
        final String[] serverConsumes =
                removeValues(consumes.value(), mediaType -> emptyToNull(mediaType) == null, String.class);
        checkState(serverConsumes.length > 0, "Consumes.value() was empty on %s", data.configKey());
        data.template().header(CONTENT_TYPE, serverConsumes);
    }

    protected void registerParamAnnotations() {
        registerParameterAnnotation(PathParam.class, (param, data, paramIndex) -> {
            final String name = param.value();
            checkState(emptyToNull(name) != null, "PathParam.value() was empty on parameter %s",
                    paramIndex);
            nameParam(data, name, paramIndex);
        });
        registerParameterAnnotation(QueryParam.class, (param, data, paramIndex) -> {
            final String name = param.value();
            checkState(emptyToNull(name) != null, "QueryParam.value() was empty on parameter %s",
                    paramIndex);
            final String query = addTemplatedParam(name);
            data.template().query(name, query);
            nameParam(data, name, paramIndex);
        });
        registerParameterAnnotation(HeaderParam.class, (param, data, paramIndex) -> {
            final String name = param.value();
            checkState(emptyToNull(name) != null, "HeaderParam.value() was empty on parameter %s",
                    paramIndex);
            final String header = addTemplatedParam(name);
            data.template().header(name, header);
            nameParam(data, name, paramIndex);
        });
        registerParameterAnnotation(FormParam.class, (param, data, paramIndex) -> {
            final String name = param.value();
            checkState(emptyToNull(name) != null, "FormParam.value() was empty on parameter %s",
                    paramIndex);
            data.formParams().add(name);
            nameParam(data, name, paramIndex);
        });

        registerParameterAnnotation(BeanParam.class, (param, data, paramIndex) -> {
            final Field[] aggregatedParams = data.method()
                    .getParameters()[paramIndex]
                    .getType()
                    .getDeclaredFields();

            for (Field aggregatedParam : aggregatedParams) {

                if (aggregatedParam.isAnnotationPresent(PathParam.class)) {
                    final String name = aggregatedParam.getAnnotation(PathParam.class).value();
                    checkState(
                            emptyToNull(name) != null,
                            "BeanParam parameter %s contains PathParam with empty .value() on field %s",
                            paramIndex,
                            aggregatedParam.getName());
                    nameParam(data, name, paramIndex);
                }

                if (aggregatedParam.isAnnotationPresent(QueryParam.class)) {
                    final String name = aggregatedParam.getAnnotation(QueryParam.class).value();
                    checkState(
                            emptyToNull(name) != null,
                            "BeanParam parameter %s contains QueryParam with empty .value() on field %s",
                            paramIndex,
                            aggregatedParam.getName());
                    final String query = addTemplatedParam(name);
                    data.template().query(name, query);
                    nameParam(data, name, paramIndex);
                }

                if (aggregatedParam.isAnnotationPresent(HeaderParam.class)) {
                    final String name = aggregatedParam.getAnnotation(HeaderParam.class).value();
                    checkState(
                            emptyToNull(name) != null,
                            "BeanParam parameter %s contains HeaderParam with empty .value() on field %s",
                            paramIndex,
                            aggregatedParam.getName());
                    final String header = addTemplatedParam(name);
                    data.template().header(name, header);
                    nameParam(data, name, paramIndex);
                }

                if (aggregatedParam.isAnnotationPresent(FormParam.class)) {
                    final String name = aggregatedParam.getAnnotation(FormParam.class).value();
                    checkState(
                            emptyToNull(name) != null,
                            "BeanParam parameter %s contains FormParam with empty .value() on field %s",
                            paramIndex,
                            aggregatedParam.getName());
                    data.formParams().add(name);
                    nameParam(data, name, paramIndex);
                }
            }
        });
    }

    // Not using override as the super-type's method is deprecated and will be removed.
    // Protected so JAXRS2Contract can make use of this
    protected String addTemplatedParam(String name) {
        return String.format("{%s}", name);
    }
}
