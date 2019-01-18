package gov.va.health.api.sentinel;

import static gov.va.health.api.sentinel.ResourceRequest.assertRequest;

import gov.va.api.health.argonaut.api.resources.Encounter;
import gov.va.api.health.argonaut.api.resources.OperationOutcome;
import java.util.Arrays;
import java.util.List;
import org.junit.runners.Parameterized.Parameters;

public class EncounterIT {

  @Parameters(name = "{index}: {0} {2}")
  public static List<Object[]> parameters() {
    ResourceRequest resourceRequest = new ResourceRequest();
    TestIds ids = IdRegistrar.of(Sentinel.get().system()).registeredIds();
    return Arrays.asList(
        assertRequest(200, Encounter.class, "/api/Encounter/{id}", ids.encounter()),
        assertRequest(404, OperationOutcome.class, "/api/Encounter/{id}", ids.unknown()),
        assertRequest(200, Encounter.Bundle.class, "/api/Encounter?_id={id}", ids.encounter()),
        assertRequest(
            200, Encounter.Bundle.class, "/api/Encounter?identifier={id}", ids.encounter()),
        assertRequest(404, OperationOutcome.class, "/api/Encounter?_id={id}", ids.unknown()));
  }
}
