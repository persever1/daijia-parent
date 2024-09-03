package com.atguigu.daijia.common.result;


import lombok.Data;

/**
 * 全局统一返回结果类
 */
@Data
public class Result<T> {

    //返回码
    private Integer code;

    //返回消息
    private String message;

    //返回数据
    private T data;

    public Result() {
    }

    /**
     * 构建一个Result对象。
     * <p>
     * 该方法用于创建一个Result对象，并根据传入的数据决定是否设置该数据。
     * 如果传入的数据不为空，则将数据设置到Result对象中，否则Result对象中不包含数据。
     * 这种方式允许Result对象在不同情况下表示不同的状态，包括成功带有数据和失败不带数据。
     *
     * @param <T>  通用类型参数，表示Result对象中可能包含的任意类型的数据。
     * @param data 用于构建Result对象的数据。如果为null，则Result对象不包含数据。
     * @return 返回一个新的Result对象，可能包含传入的数据。
     */
    // 返回数据
    protected static <T> Result<T> build(T data) {
        Result<T> result = new Result<T>();
        if (data != null)
            result.setData(data);
        return result;
    }


    /**
     * 根据给定的参数构建一个Result对象。
     * Result对象用于封装方法的返回结果，包括数据体、状态码和消息。
     * 该方法允许调用者自定义Result的状态码和消息，以更灵活地表示方法的执行结果。
     *
     * @param body    方法的返回数据体，泛型T表示可以适应多种类型的数据。
     * @param code    状态码，用于表示方法执行的结果状态，例如成功、失败等。
     * @param message 结果的描述信息，用于详细说明方法执行的结果状态。
     * @param <T>     泛型参数，表示body参数的类型，允许该方法适应多种类型的数据。
     * @return 返回构建好的Result对象，包含给定的数据体、状态码和消息。
     */
    public static <T> Result<T> build(T body, Integer code, String message) {
        // 使用给定的数据体构建Result对象
        Result<T> result = build(body);
        // 设置Result的状态码和消息
        result.setCode(code);
        result.setMessage(message);
        // 返回构建完成的Result对象
        return result;
    }

    /**
     * 根据业务结果和结果代码枚举构建Result对象。
     * <p>
     * 此方法通过提供具体的业务数据和结果代码枚举，来构建一个包含业务数据和操作结果信息的Result对象。
     * 主要用于在业务操作完成后，包装操作结果和相关提示信息，以便上层调用者使用。
     *
     * @param body           业务操作的具体结果数据，可以是任意类型。
     * @param resultCodeEnum 表示操作结果的状态码枚举，包含操作结果的状态码和对应的消息。
     * @param <T>            Result中承载的业务数据的类型。
     * @return 构建完成的Result对象，包含业务数据和操作结果的状态码及消息。
     */
    public static <T> Result<T> build(T body, ResultCodeEnum resultCodeEnum) {
        Result<T> result = build(body);
        result.setCode(resultCodeEnum.getCode());
        result.setMessage(resultCodeEnum.getMessage());
        return result;
    }


    /**
     * 创建一个表示成功结果的对象。
     * <p>
     * 此方法用于生成一个表示操作成功的Result对象，其中包含的数据显示为null。
     * 这在某些情况下有用，例如，当只需要表示操作成功而不需要返回具体数据时。
     *
     * @param <T> 结果数据的泛型类型。
     * @return 返回一个包含null数据的成功Result对象。
     */
    public static <T> Result<T> ok() {
        return Result.ok(null);
    }

    /**
     * 构建操作成功结果对象。
     * <p>
     * 此方法用于封装一个成功的结果，其中包含业务数据。它适用于当操作成功且需要返回具体数据时的场景。
     *
     * @param data 操作成功时返回的数据，可以是任意类型。
     * @param <T>  数据的泛型类型，用于确保类型安全。
     * @return 返回一个包含成功状态和业务数据的结果对象。
     */
    public static <T> Result<T> ok(T data) {
        return build(data, ResultCodeEnum.SUCCESS);
    }


    /**
     * 创建一个失败的结果对象。
     * <p>
     * 此方法用于在无法提供具体错误信息的情况下，创建一个失败的结果对象。失败的结果对象表示业务操作未能成功完成。
     * 通过返回一个带有null错误信息的失败结果，调用方可以得知操作失败，而无需具体的错误详情。
     * 这在某些情况下对于简化错误处理流程特别有用，尤其是在错误信息不便于传递或不必要时。
     *
     * @param <T> 结果数据的泛型类型。
     * @return 返回一个失败的结果对象，其中错误信息为null。
     */
    public static <T> Result<T> fail() {
        return Result.fail(null);
    }

    /**
     * 创建一个操作失败的结果对象。
     * 该方法用于封装失败的操作结果，通常在业务逻辑处理中，当操作未能成功执行时调用。
     *
     * @param data 操作失败时返回的数据，可以是任何类型。此数据可以包含错误的详细信息，或者操作失败后的一些回滚信息等。
     * @param <T>  数据的泛型类型，用于确保类型安全。
     * @return 返回一个封装了失败结果的对象，其中包含操作失败时的数据。
     */
    public static <T> Result<T> fail(T data) {
        return build(data, ResultCodeEnum.FAIL);
    }


    public Result<T> message(String msg) {
        this.setMessage(msg);
        return this;
    }

    /**
     * 设置结果代码，并返回当前实例。
     * <p>
     * 该方法允许调用者设置结果的代码，通常表示某种操作的状态或结果。
     * 返回当前实例使得方法调用可以链式进行，提高了代码的可读性和简洁性。
     *
     * @param code 结果代码，用于表示操作的状态或结果。
     * @return 当前结果实例，允许链式调用。
     */
    public Result<T> code(Integer code) {
        this.setCode(code);
        return this;
    }
}
