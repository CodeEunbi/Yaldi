package ssafy.yaldi.global.response.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import ssafy.yaldi.global.response.ApiResponse;

@Slf4j
@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
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
