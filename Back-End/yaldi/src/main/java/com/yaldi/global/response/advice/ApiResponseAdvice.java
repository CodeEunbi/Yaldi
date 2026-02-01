package com.yaldi.global.response.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import com.yaldi.global.response.ApiResponse;

@Slf4j
@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // Swagger/Springdoc 관련 패키지는 제외
        String typeName = returnType.getContainingClass().getName();
        return !typeName.startsWith("org.springdoc")
                && !typeName.startsWith("springfox.documentation")
                && !typeName.equals("com.yaldi.domain.preview.PreviewController")
                && !ByteArrayHttpMessageConverter.class.isAssignableFrom(converterType)
                && !ResourceHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        String uri = request.getURI().getPath();
        String method = request.getMethod().name();

        // Swagger/Springdoc 경로 및 에러 경로는 그대로 반환
        if (uri.startsWith("/swagger") || uri.startsWith("/api-docs")
                || uri.startsWith("/v3/api-docs") || uri.startsWith("/error")) {
            return body;
        }
        if (MediaType.APPLICATION_OCTET_STREAM.equals(selectedContentType)
                || MediaType.IMAGE_PNG.equals(selectedContentType)
                || MediaType.IMAGE_JPEG.equals(selectedContentType)
                || MediaType.IMAGE_GIF.equals(selectedContentType)) {
            return body;
        }

        if (body instanceof ApiResponse<?> apiResponse) {

            if (Boolean.FALSE.equals(apiResponse.isSuccess())) {
                log.error("[{} {}] Error Response - Code: {}, Message: {}, Result: {}",
                        method,
                        uri,
                        apiResponse.code(),
                        apiResponse.message(),
                        apiResponse.result() != null ? apiResponse.result() : "No additional data"
                );
            } else {
                log.info("[{} {}] Success Response - Code: {}, Message: {}, Result: {}",
                        method,
                        uri,
                        apiResponse.code(),
                        apiResponse.message(),
                        apiResponse.result());
            }

            return apiResponse;
        }

        if (body == null) {
            log.info("[{} {}] Success (no content)", method, uri);
            return ApiResponse.OK;
        }

        log.info("[{} {}] Success Response Body: {}", method, uri, body);
        return ApiResponse.onSuccess(body);
    }
}
