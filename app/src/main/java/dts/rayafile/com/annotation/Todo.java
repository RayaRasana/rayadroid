package dts.rayafile.com.annotation;


import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Target;

/**
 * In this project, this feature is not yet complete, and will continue to be maintained in the future
 */

@Target({TYPE, METHOD, CONSTRUCTOR, FIELD})
public @interface Todo {
}
