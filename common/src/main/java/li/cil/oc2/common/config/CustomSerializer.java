/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CustomSerializer {
    String serializer() default "";

    String deserializer() default "";
}
