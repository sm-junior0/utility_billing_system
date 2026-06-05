package org.smj.utilitybilling.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
/**
 * Validated request DTO for BillGenerationRequest API input.
 */

@Getter
@Setter
@Schema(
        description = "Generate a bill from a captured meter reading ID returned by POST /api/readings.",
        example = """
                {
                  "readingId": 4
                }
                """)
public class BillGenerationRequest {

    @NotNull(message = "Reading ID is required")
    @Positive(message = "Reading ID must be a positive number")
    @Schema(example = "4", description = "Meter reading ID to convert into a monthly bill. Use the id from POST /api/readings.")
    private Long readingId;
}
