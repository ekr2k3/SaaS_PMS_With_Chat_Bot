package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.EXCEPTION;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.RES.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // =========================================================
    // COMMON RESPONSE
    // =========================================================

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        String finalMessage = StringUtils.hasText(message)
                ? message
                : status.getReasonPhrase();

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                finalMessage,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Log các lỗi HTTP 4xx.
     * Không ghi toàn bộ Request Body để tránh lộ dữ liệu nhạy cảm.
     */
    private void logClientError(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        log.warn(
                "HTTP {} | {} {} | {}",
                status.value(),
                request.getMethod(),
                request.getRequestURI(),
                message
        );
    }

    /**
     * Giới hạn độ dài message lấy từ exception.
     */
    private String normalizeMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "Đã xảy ra lỗi trong quá trình xử lý";
        }

        String normalized = message.trim();

        if (normalized.length() > 500) {
            return normalized.substring(0, 500) + "...";
        }

        return normalized;
    }

    // =========================================================
    // RESPONSE STATUS EXCEPTION
    // =========================================================

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.resolve(
                ex.getStatusCode().value()
        );

        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String message = normalizeMessage(
                ex.getReason() != null
                        ? ex.getReason()
                        : status.getReasonPhrase()
        );

        if (status.is5xxServerError()) {
            log.error(
                    "HTTP {} | {} {} | {}",
                    status.value(),
                    request.getMethod(),
                    request.getRequestURI(),
                    message,
                    ex
            );
        } else {
            logClientError(status, message, request);
        }

        return buildErrorResponse(status, message, request);
    }

    // =========================================================
    // DTO VALIDATION: @Valid
    // =========================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": "
                                + normalizeMessage(error.getDefaultMessage())
                )
                .collect(Collectors.toList());

        ex.getBindingResult()
                .getGlobalErrors()
                .forEach(error ->
                        errors.add(
                                error.getObjectName() + ": "
                                        + normalizeMessage(error.getDefaultMessage())
                        )
                );

        String message = errors.isEmpty()
                ? "Dữ liệu đầu vào không hợp lệ"
                : String.join("; ", errors);

        logClientError(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }

    // =========================================================
    // CONSTRAINT VALIDATION
    // =========================================================

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        String message = ex.getConstraintViolations()
                .stream()
                .map(this::formatConstraintViolation)
                .collect(Collectors.joining("; "));

        if (!StringUtils.hasText(message)) {
            message = "Dữ liệu không thỏa mãn các điều kiện hợp lệ";
        }

        logClientError(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }

    private String formatConstraintViolation(
            ConstraintViolation<?> violation
    ) {
        return violation.getPropertyPath()
                + ": "
                + normalizeMessage(violation.getMessage());
    }

    // =========================================================
    // INVALID JSON / REQUEST BODY
    // =========================================================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        String detail = ex.getMostSpecificCause().getMessage();

        String message = StringUtils.hasText(detail)
                ? "Request Body không hợp lệ: " + normalizeMessage(detail)
                : "Request Body bị thiếu hoặc không đúng định dạng JSON";

        logClientError(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }

    // =========================================================
    // MISSING REQUEST PARAMETER
    // =========================================================

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        String message = String.format(
                "Thiếu Request Parameter '%s' (kiểu dữ liệu yêu cầu: %s)",
                ex.getParameterName(),
                ex.getParameterType()
        );

        logClientError(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }

    // =========================================================
    // INVALID PARAMETER TYPE
    // =========================================================

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String requiredType = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "không xác định";

        String message = String.format(
                "Tham số '%s' không đúng kiểu dữ liệu. Kiểu dữ liệu yêu cầu: %s",
                ex.getName(),
                requiredType
        );

        logClientError(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }

    // =========================================================
    // ACCESS DENIED
    // =========================================================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        String message = "Bạn không có quyền thực hiện thao tác này. "
                + "Vui lòng kiểm tra Role và Permission của tài khoản.";

        logClientError(
                HttpStatus.FORBIDDEN,
                message,
                request
        );

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                message,
                request
        );
    }

    // =========================================================
    // DATABASE CONSTRAINT
    // =========================================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        String message = "Dữ liệu không thể lưu do vi phạm ràng buộc Database. "
                + "Có thể dữ liệu đã tồn tại, đang được tham chiếu "
                + "hoặc không thỏa mãn điều kiện của hệ thống.";

        log.error(
                "Database integrity violation | {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex
        );

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                message,
                request
        );
    }

    // =========================================================
    // HTTP METHOD NOT SUPPORTED
    // =========================================================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        String supportedMethods = ex.getSupportedHttpMethods() != null
                ? ex.getSupportedHttpMethods()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "))
                : "Không xác định";

        String message = String.format(
                "HTTP Method '%s' không được hỗ trợ tại endpoint này. "
                        + "Các Method được hỗ trợ: %s",
                ex.getMethod(),
                supportedMethods
        );

        logClientError(
                HttpStatus.METHOD_NOT_ALLOWED,
                message,
                request
        );

        return buildErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                message,
                request
        );
    }

    // =========================================================
    // UNSUPPORTED MEDIA TYPE
    // =========================================================

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {
        String supportedTypes = ex.getSupportedMediaTypes()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));

        String message = String.format(
                "Content-Type '%s' không được hỗ trợ. "
                        + "Content-Type được hỗ trợ: %s",
                ex.getContentType(),
                supportedTypes
        );

        logClientError(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                message,
                request
        );

        return buildErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                message,
                request
        );
    }

    // =========================================================
    // ENDPOINT NOT FOUND
    // =========================================================

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NoHandlerFoundException ex,
            HttpServletRequest request
    ) {
        String message = String.format(
                "Không tìm thấy endpoint '%s' với HTTP Method '%s'",
                request.getRequestURI(),
                request.getMethod()
        );

        logClientError(
                HttpStatus.NOT_FOUND,
                message,
                request
        );

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                message,
                request
        );
    }

    // =========================================================
    // UNEXPECTED EXCEPTION
    // =========================================================

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleGenericException(
//            Exception ex,
//            HttpServletRequest request
//    ) {
//        String message = "Đã xảy ra lỗi nội bộ hệ thống. "
//                + "Vui lòng thử lại sau hoặc liên hệ quản trị viên.";
//
//        log.error(
//                "UNHANDLED EXCEPTION | {} {} | Exception: {} | Message: {}",
//                request.getMethod(),
//                request.getRequestURI(),
//                ex.getClass().getName(),
//                ex.getMessage(),
//                ex
//        );
//
//        return buildErrorResponse(
//                HttpStatus.INTERNAL_SERVER_ERROR,
//                message,
//                request
//        );
//    }

    // Xử lý các lỗi không dự kiến
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        // Ghi đầy đủ Exception và stack trace vào backend console
        log.error(
                "Unhandled Exception | Method: {} | URI: {}",
                request.getMethod(),
                request.getRequestURI(),
                ex
        );

        // Lấy message thực tế từ Exception
        String message = ex.getMessage();

        // Chỉ dùng thông báo dự phòng khi Exception không có message
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                message,
                request
        );
    }
}