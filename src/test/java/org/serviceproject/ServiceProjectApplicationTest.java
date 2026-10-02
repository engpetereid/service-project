package org.serviceproject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Basic smoke test verifying the application class is loadable.
 * <p>
 * This is NOT a Spring context integration test ({@code @SpringBootTest} is
 * intentionally not used).  The project only uses unit tests per requirement #110.
 */
class ServiceProjectApplicationTest {

    @Test
    void main_classIsLoadable() {
        assertDoesNotThrow(() -> Class.forName("org.serviceproject.ServiceProjectApplication"));
    }
}
