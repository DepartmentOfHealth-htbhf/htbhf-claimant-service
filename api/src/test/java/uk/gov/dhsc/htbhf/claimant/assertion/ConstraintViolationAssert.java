package uk.gov.dhsc.htbhf.claimant.assertion;

import org.assertj.core.api.AbstractAssert;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;

/**
 * Adds custom assertions for {@link ConstraintViolation} when testing validation constraints.
 */
public final class ConstraintViolationAssert<T> extends AbstractAssert<ConstraintViolationAssert<T>, Set<ConstraintViolation<T>>> {

    private ConstraintViolationAssert(Set<ConstraintViolation<T>> constraintViolations) {
        super(constraintViolations, ConstraintViolationAssert.class);
    }

    /**
     * Fluent entry point for assertions.
     *
     * @param actual The set to be tested.
     * @param <T>    The type that the constraint violations will be applicable to.
     * @return The assert for chaining.
     */
    @SuppressWarnings("unchecked")
    public static <T> ConstraintViolationAssert assertThat(Set<ConstraintViolation<T>> actual) {
        return new ConstraintViolationAssert(actual);
    }

    /**
     * Assert that there are no {@link ConstraintViolation}s in the Set to be tested.
     *
     * @return The assert for chaining.
     */
    public ConstraintViolationAssert hasNoViolations() {
        return hasTotalViolations(0);
    }

    /**
     * Tests that the single constraint violation provided matches the one found in the set.
     *
     * @return The assert for chaining.
     */
    public ConstraintViolationAssert hasSingleConstraintViolation(String message, String path) {
        return hasTotalViolations(1).hasViolation(message, path);
    }

    /**
     * Tests that we have the correct number of constraint violations in the set.
     *
     * @return The assert for chaining.
     */
    public ConstraintViolationAssert hasTotalViolations(int expectedSize) {
        // check that actual Set<ConstraintViolation> we want to make assertions on is not null.
        isNotNull();

        // check that there the correct number of elements
        if (actual.size() != expectedSize) {
            failWithMessage("Expected <%s> Constraint Violations, there are actually <%s>.", expectedSize, actual.size());
        }

        return this;
    }

    /**
     * Asserts that the ConstraintViolation with the given message and path has been found.
     *
     * @param message The message to match
     * @param path    The path to match.
     * @return The assert for chaining.
     */
    public ConstraintViolationAssert hasViolation(String message, String path) {
        // check that actual Set<ConstraintViolation> we want to make assertions on is not null.
        isNotNull();

        Optional<ConstraintViolation<T>> foundViolation = actual.stream()
                .filter(constraintViolation -> constraintViolation.getMessage().equals(message)
                        && constraintViolation.getPropertyPath().toString().equals(path))
                .findFirst();

        if (!foundViolation.isPresent()) {
            failWithMessage("No ConstraintViolation found matching path: <%s> and message: <%s>, violations present are: <%s>",
                    path, message, listConstraintViolations());
        }

        return this;
    }

    private String listConstraintViolations() {
        return actual.stream().map(constraintViolation -> constraintViolation.getPropertyPath().toString() + " - " + constraintViolation.getMessage())
                .collect(Collectors.joining(", "));
    }


}
