/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface KeyValueTypes {
    Class<?> keyType();

    CustomSerializer keySerializer() default @CustomSerializer();

    Class<?> valueType();

    CustomSerializer valueSerializer() default @CustomSerializer();
}
