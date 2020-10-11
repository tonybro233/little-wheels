package io.github.tonybro233.littlewheels.validation;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.charset.Charset;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 验证注解的String的字节长度在范围之内
 *
 * @author tony
 */
@Target({FIELD, PARAMETER, CONSTRUCTOR})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = {ByteSize.ByteSizeValidator.class})
public @interface ByteSize {

    String message() default "字符字节长度必须在{min}-{max}之间";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    int min() default 0;

    int max() default Integer.MAX_VALUE;

    String charset() default "UTF-8";

    class ByteSizeValidator implements ConstraintValidator<ByteSize, String>{

        private int min;
        private int max;
        private Charset charset;

        @Override
        public void initialize(ByteSize parameters) {
            min = parameters.min();
            max = parameters.max();
            validateParameters();
            charset = Charset.forName(parameters.charset());
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if ( value == null ) {
                return true;
            }
            int length = value.getBytes(charset).length;
            return length >= min && length <= max;
        }

        private void validateParameters() {
            if ( min < 0 ) {
                throw new IllegalArgumentException("The min parameter cannot be negative.");
            }
            if ( max < 0 ) {
                throw new IllegalArgumentException("The max parameter cannot be negative.");
            }
            if ( max < min ) {
                throw new IllegalArgumentException("Invalid range, the length cannot be negative.");
            }
        }
    }
}
