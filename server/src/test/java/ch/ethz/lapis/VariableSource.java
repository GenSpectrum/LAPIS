package ch.ethz.lapis;

import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.*;


// From https://www.baeldung.com/parameterized-tests-junit-5 - thanks!
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(VariableArgumentsProvider.class)
public @interface VariableSource {

    /**
     * The name of the static variable
     */
    String value();
}
