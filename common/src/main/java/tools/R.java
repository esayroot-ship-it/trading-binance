package tools;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class R<T> {

    private int code;
    private String message;
    private T data;

    // 1. 对于不需要返回数据的成功 (泛型指定为 Void)
    public static <T> R<T> ok(String message) {
        return new R<>(1, message, null);
    }


    public static <T> R<T> ok(String message, T data) {
        return new R<>(1, message, data);
    }

    // 3. 失败的情况通常不需要带数据，可以保持泛型或者返回 R<Void>
    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> fail(String message) {
        return new R<>(0, message, null);
    }
}
