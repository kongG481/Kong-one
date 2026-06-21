package com.example.exception;

import com.example.entity.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 功能说明：
 * 1. 统一处理系统中所有异常，避免异常信息直接暴露给前端
 * 2. 将不同类型的异常转换为友好的错误提示
 * 3. 记录详细的错误日志，便于问题追踪
 * <p>
 * 异常处理优先级（从上到下）：
 * 1. BusinessException - 业务逻辑异常（最高优先级）
 * 2. MethodArgumentNotValidException - 参数校验异常
 * 3. BindException - 参数绑定异常
 * 4. MissingServletRequestParameterException - 缺少请求参数
 * 5. MaxUploadSizeExceededException - 文件上传超限
 * 6. NoResourceFoundException - 资源未找到
 * 7. IllegalArgumentException - 非法参数
 * 8. RuntimeException - 运行时异常
 * 9. Exception - 通用异常（最低优先级，兜底处理）
 *
 * @author System
 * @since 1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * <p>
     * 业务异常是系统在业务逻辑校验中主动抛出的异常
     * 例如：权限不足、数据不存在、状态不允许等
     * <p>
     * 返回HTTP 200状态码，由前端根据code字段判断成功或失败
     *
     * @param e 业务异常对象
     * @return 统一的错误响应结果
     */
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid注解触发）
     * <p>
     * 当前端传递的数据不符合后端校验规则时触发
     * 例如：必填字段为空、邮箱格式错误、长度超限等
     * <p>
     * 会收集所有字段的错误信息，用分号拼接后返回
     *
     * @param e 参数校验异常对象
     * @return 包含详细错误信息的响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return Result.error("参数错误: " + message);
    }

    /**
     * 处理参数绑定异常
     * <p>
     * 与MethodArgumentNotValidException类似，但适用于表单数据绑定场景
     * 例如：类型转换失败、格式不匹配等
     *
     * @param e 绑定异常对象
     * @return 包含错误信息的响应结果
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", message);
        return Result.error("参数错误: " + message);
    }

    /**
     * 处理缺少必要请求参数的异常
     * <p>
     * 当前端未传递接口要求的必填参数时触发
     * 例如：@RequestParam(required=true)的参数未传递
     *
     * @param e 缺少参数异常对象
     * @return 提示缺少哪个参数的响应结果
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleMissingParamException(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return Result.error("缺少必要参数: " + e.getParameterName());
    }

    /**
     * 处理文件上传大小超限异常
     * <p>
     * 当上传的文件大小超过application.yml中配置的限制时触发
     * 默认限制：spring.servlet.multipart.max-file-size
     *
     * @param e 文件超限异常对象
     * @return 提示文件过大的响应结果
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleMaxUploadSizeException(MaxUploadSizeExceededException e) {
        log.warn("文件上传大小超限");
        return Result.error("上传文件大小超出限制");
    }

    /**
     * 处理资源未找到异常
     * <p>
     * 当请求的静态资源或API路径不存在时触发
     * 例如：访问了未定义的接口URL
     *
     * @param e 资源未找到异常对象
     * @return 提示资源不存在的响应结果
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("资源未找到: {}", e.getResourcePath());
        return Result.error("请求的资源不存在");
    }

    /**
     * 处理非法参数异常
     * <p>
     * 当业务逻辑中检测到非法参数时手动抛出
     * 例如：ID为负数、状态值不在允许范围内等
     *
     * @param e 非法参数异常对象
     * @return 包含错误描述的响应结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 处理运行时异常
     * <p>
     * 捕获所有未预期的RuntimeException，包括：
     * - NullPointerException（空指针）
     * - IndexOutOfBoundsException（数组越界）
     * - NumberFormatException（数字格式错误）
     * - RuntimeException（其他运行时错误）
     * <p>
     * 记录完整堆栈信息，便于开发排查问题
     *
     * @param e 运行时异常对象
     * @return 通用错误提示，不暴露具体技术细节
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleRuntimeException(RuntimeException e) {
        log.error("运行时异常", e);
        return Result.error("服务器内部错误: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
    }

    /**
     * 处理所有未被捕获的异常（兜底处理）
     * <p>
     * 这是最后的防线，捕获前面所有处理器未处理的异常
     * 例如：Error、Checked Exception等
     * <p>
     * 为了安全起见，不向客户端暴露具体错误信息
     * 只返回通用提示，详细错误记录在服务器日志中
     *
     * @param e 异常对象
     * @return 通用错误提示
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统异常，请联系管理员");
    }
}
