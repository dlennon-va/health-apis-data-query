package gov.va.api.health.argonaut.service.mranderson.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gov.va.api.health.argonaut.service.controller.Parameters;
import gov.va.api.health.argonaut.service.mranderson.client.MrAndersonClient.BadRequest;
import gov.va.api.health.argonaut.service.mranderson.client.MrAndersonClient.NotFound;
import gov.va.api.health.argonaut.service.mranderson.client.MrAndersonClient.SearchFailed;
import gov.va.api.health.argonaut.service.mranderson.client.Query.Profile;
import gov.va.dvp.cdw.xsd.pojos.Patient103Root;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class RestMrAndersonClientTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Mock RestTemplate rt;

  RestMrAndersonClient client;

  @Before
  public void _init() {
    MockitoAnnotations.initMocks(this);
    client = new RestMrAndersonClient("https://example.com", rt);
  }

  @Test
  public void badRequestIsThrownFor400Status() {
    thrown.expect(BadRequest.class);
    mockResponse(HttpStatus.BAD_REQUEST, null);
    client.search(query());
  }

  private void mockResponse(HttpStatus status, Patient103Root body) {
    ResponseEntity response = mock(ResponseEntity.class);
    when(response.getStatusCode()).thenReturn(status);
    when(response.getBody()).thenReturn(body);
    when(rt.exchange(
            Mockito.anyString(),
            Mockito.eq(HttpMethod.GET),
            Mockito.any(HttpEntity.class),
            Mockito.any(ParameterizedTypeReference.class)))
        .thenReturn(response);
  }

  @Test
  public void notFoundIsThrownFor404Status() {
    thrown.expect(NotFound.class);
    mockResponse(HttpStatus.NOT_FOUND, null);
    client.search(query());
  }

  private Query<Patient103Root> query() {
    return Query.forType(Patient103Root.class)
        .resource("Patient")
        .profile(Profile.ARGONAUT)
        .version("123")
        .parameters(Parameters.forIdentity("456"))
        .build();
  }

  @Test
  public void responseBodyIsReturnedFor200Status() {
    thrown.expect(SearchFailed.class);
    mockResponse(HttpStatus.INTERNAL_SERVER_ERROR, null);
    client.search(query());
  }

  @Test
  public void searchFailedIsThrownForNonOkStatus() {
    Patient103Root root = new Patient103Root();
    mockResponse(HttpStatus.OK, root);
    Patient103Root actual = client.search(query());
    assertThat(actual).isSameAs(root);
    query().hashCode();
    query().equals(query());
  }
}