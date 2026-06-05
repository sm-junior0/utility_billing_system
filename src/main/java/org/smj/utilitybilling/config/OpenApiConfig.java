package org.smj.utilitybilling.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.Comparator;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * Spring configuration for OpenApiConfig.
 */

@Configuration
public class OpenApiConfig {

    private static final List<String> TAG_ORDER = List.of(
            "01 - Authentication",
            "02 - Admin User Management",
            "03 - Customer Setup",
            "04 - Meter Setup",
            "05 - Billing Configuration",
            "06 - Meter Reading Operations",
            "07 - Billing Operations",
            "08 - Payment Operations",
            "09 - Customer Self-Service",
            "10 - Notifications",
            "11 - Legacy Prepaid Support"
    );

    @Bean
    public OpenAPI openAPI() {
        String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Utility Billing System API")
                        .version("1.0")
                        .description("""
                                Enterprise backend APIs for customer onboarding, role-based administration,
                                meter operations, tariff configuration, postpaid billing, payment processing,
                                customer self-service, notifications, and legacy prepaid support.

                                Testing order: authentication -> admin users -> customer setup -> meter setup ->
                                billing configuration -> meter readings -> billing -> payments ->
                                customer self-service -> notifications -> legacy prepaid.

                                Protected endpoints require a JWT:
                                1) POST /api/auth/login (no token needed)
                                2) Copy the token value from the response
                                3) Click Authorize and paste only the token (do not type Bearer)
                                4) After an admin role change, login again for a new token.
                                """))
                        .tags(List.of(
                        new Tag().name("01 - Authentication")
                                .description("Public signup, OTP verification, OTP resend, and login. No JWT is required for this section."),
                        new Tag().name("02 - Admin User Management")
                                .description("ROLE_ADMIN user review, status management, and role promotion or demotion after signup."),
                        new Tag().name("03 - Customer Setup")
                                .description("ROLE_ADMIN setup of customer records, customer-owner user links, and active/inactive customer status."),
                        new Tag().name("04 - Meter Setup")
                                .description("ROLE_ADMIN utility meter registration, customer meter ownership, and meter active/inactive status."),
                        new Tag().name("05 - Billing Configuration")
                                .description("ROLE_ADMIN configuration of future billing rules: tariffs, tariff tiers, taxes, penalties, and service charges."),
                        new Tag().name("06 - Meter Reading Operations")
                                .description("ROLE_OPERATOR monthly meter reading capture and reading review before bills are generated."),
                        new Tag().name("07 - Billing Operations")
                                .description("ROLE_FINANCE and ROLE_ADMIN bill generation, bill approval, and bill review."),
                        new Tag().name("08 - Payment Operations")
                                .description("ROLE_FINANCE payment recording, balance updates, partial/full payment handling, and payment review."),
                        new Tag().name("09 - Customer Self-Service")
                                .description("ROLE_CUSTOMER access to only their linked customer profile, meters, bills, payments, prepaid history, and notifications."),
                        new Tag().name("10 - Notifications")
                                .description("ROLE_ADMIN and ROLE_FINANCE notification audit, customer message review, and optional email sending."),
                        new Tag().name("11 - Legacy Prepaid Support")
                                .description("Small legacy support area for electricity prepaid top-ups while the system transitions to postpaid billing.")
                ))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public OpenApiCustomizer enforceSwaggerTagOrder() {
        return openApi -> {
            if (openApi.getTags() == null) {
                return;
            }
            openApi.getTags().sort(Comparator.comparingInt(tag -> {
                int index = TAG_ORDER.indexOf(tag.getName());
                return index >= 0 ? index : Integer.MAX_VALUE;
            }));
        };
    }
}
