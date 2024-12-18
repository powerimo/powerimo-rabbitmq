import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SamplePayload {
    private int intValue = 10;
    private String stringValue = "sample";
    private OffsetDateTime dateTimeValue = OffsetDateTime.now();
}
