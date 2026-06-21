package com.example.entity;
import lombok.Data;

/**
 * 统一响应结果类（Lombok精简版）
 * <p>
 * 功能说明：
 * 1. 统一后端API的响应格式，方便前端处理
 * 2. 使用泛型支持任意类型的响应数据
 * 3. 提供静态工厂方法快速构建响应对象
 * <p>
 * 响应格式示例：
 * 成功：{"code": 0, "message": "操作成功", "data": {...}}
 * 失败：{"code": 1, "message": "错误信息", "data": null}
 * <p>
 * 使用方式：
 * - Result.success() - 无数据成功响应
 * - Result.success(data) - 带数据成功响应
 * - Result.error(message) - 失败响应
 *
 * @param <T> 响应数据的泛型类型
 * @author System
 * @since 1.0
 */
@Data // 自动生成getter、setter、toString、equals、hashCode、无参构造方法
public class Result<T> {
    /**
     * 业务状态码
     * 0-成功，1-失败
     */
    private Integer code;

    /**
     * 提示信息
     * 成功时："操作成功"
     * 失败时：具体错误描述
     */
    private String message;

    /**
     * 响应数据
     * 成功时携带业务数据，失败时为null
     */
    private T data;

    /**
     * 快速返回操作成功响应结果(带响应数据)
     * <p>
     * 使用示例：
     * <pre>
     * return Result.success(userList); // 返回用户列表
     * return Result.success("注册成功"); // 返回提示消息
     * </pre>
     *
     * @param data 响应数据
     * @param <E> 数据类型
     * @return 成功的响应结果
     */
    public static <E> Result<E> success(E data) {
        Result<E> result = new Result<>();
        result.setCode(0);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 快速返回操作成功响应结果（无数据）
     * <p>
     * 适用于不需要返回数据的操作，如：删除、更新等
     * <p>
     * 使用示例：
     * <pre>
     * return Result.success(); // 仅返回成功状态
     * </pre>
     *
     * @return 成功的响应结果（data为null）
     */
    public static Result success() {
        Result result = new Result<>();
        result.setCode(0);
        result.setMessage("操作成功");
        result.setData(null);
        return result;
    }

    /**
     * 快速返回操作失败响应结果
     * <p>
     * 适用于业务逻辑校验失败、权限不足等场景
     * <p>
     * 使用示例：
     * <pre>
     * return Result.error("用户名已存在");
     * return Result.error("无权限访问");
     * </pre>
     *
     * @param message 错误提示信息
     * @return 失败的响应结果
     */
    public static Result error(String message) {
        Result result = new Result<>();
        result.setCode(1);
        result.setMessage(message);
        result.setData(null);
        return result;
    }
}