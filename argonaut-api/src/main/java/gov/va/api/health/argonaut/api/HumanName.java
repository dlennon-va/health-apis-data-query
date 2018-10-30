package gov.va.api.health.argonaut.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Schema(description = "http://hl7.org/fhir/DSTU2/datatypes.html#HumanName")
public class HumanName implements Element {
  @Pattern(regexp = Fhir.ID)
  String id;

  @Valid List<Extension> extension;

  NameUse use;
  String text;
  List<String> family;
  List<String> given;
  List<String> prefix;
  List<String> suffix;
  @Valid Period period;

  public enum NameUse {
    usual,
    official,
    temp,
    nickname,
    anonymous,
    old,
    maiden
  }
}