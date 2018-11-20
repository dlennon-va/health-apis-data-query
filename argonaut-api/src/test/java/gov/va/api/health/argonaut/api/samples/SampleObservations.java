package gov.va.api.health.argonaut.api.samples;

import static java.util.Collections.singletonList;

import gov.va.api.health.argonaut.api.resources.Observation;
import gov.va.api.health.argonaut.api.resources.Observation.Code;
import gov.va.api.health.argonaut.api.resources.Observation.ObservationComponent;
import gov.va.api.health.argonaut.api.resources.Observation.ObservationReferenceRange;
import gov.va.api.health.argonaut.api.resources.Observation.ObservationRelated;
import gov.va.api.health.argonaut.api.resources.Observation.Type;
import java.util.Arrays;
import lombok.NoArgsConstructor;
import lombok.experimental.Delegate;

/**
 * This class provides data structures that are populated with dummy values, suitable for testing
 * serialization.
 */
@SuppressWarnings("WeakerAccess")
@NoArgsConstructor(staticName = "get")
public class SampleObservations {

  @Delegate SampleDataTypes dataTypes = SampleDataTypes.get();

  public Observation observation() {
    return Observation.builder()
        .id("1234")
        .resourceType("Observation")
        .meta(meta())
        .implicitRules("http://HelloRules.com")
        .language("Hello Language")
        .text(narrative())
        .contained(singletonList(resource()))
        .extension(Arrays.asList(extension(), extension()))
        .modifierExtension(
            Arrays.asList(extension(), extensionWithQuantity(), extensionWithRatio()))
        .identifier(singletonList(identifier()))
        .status(Code.registered)
        .category(codeableConcept())
        .code(codeableConcept())
        .subject(reference())
        .encounter(reference())
        .effectiveDateTime("2000-01-01T00:00:00-00:00")
        .issued("2000-01-01T00:00:00-00:00")
        .performer(reference())
        .valueSampledData(sampledData())
        .dataAbsentReason(codeableConcept())
        .interpretation(codeableConcept())
        .comments("HelloText")
        .bodySite(codeableConcept())
        .method(codeableConcept())
        .specimen(reference())
        .device(reference())
        .referenceRange(singletonList(referenceRange()))
        .related(singletonList(related()))
        .component(singletonList(component()))
        .build();
  }

  public ObservationComponent component() {
    return ObservationComponent.builder()
        .id("0000")
        .extension(singletonList(extension()))
        .modifierExtension(singletonList(extension()))
        .code(codeableConcept())
        .valueCodeableConcept(codeableConcept())
        .dataAbsentReason(codeableConcept())
        .referenceRange(singletonList(referenceRange()))
        .build();
  }

  public ObservationReferenceRange referenceRange() {
    return ObservationReferenceRange.builder()
        .id("0000")
        .extension(singletonList(extension()))
        .modifierExtension(singletonList(extension()))
        .low(simpleQuantity())
        .high(simpleQuantity())
        .meaning(codeableConcept())
        .age(range())
        .text("HelloText")
        .build();
  }

  public ObservationRelated related() {
    return ObservationRelated.builder()
        .id("0000")
        .extension(singletonList(extension()))
        .modifierExtension(singletonList(extension()))
        .type(Type.has_member)
        .target(reference())
        .build();
  }
}
