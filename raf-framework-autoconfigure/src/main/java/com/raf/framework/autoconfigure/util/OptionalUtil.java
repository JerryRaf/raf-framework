package com.raf.framework.autoconfigure.util;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class OptionalUtil {
    private boolean isPresent;

    private OptionalUtil(boolean isPresent) {
        this.isPresent = isPresent;
    }

    public void orElse(Runnable runner) {
        if (!isPresent) {
            runner.run();
        }
    }

    public static <T> OptionalUtil ifPresent(Optional<T> opt, Consumer<? super T> consumer) {
        if (opt.isPresent()) {
            consumer.accept(opt.get());
            return new OptionalUtil(true);
        }
        return new OptionalUtil(false);
    }
}