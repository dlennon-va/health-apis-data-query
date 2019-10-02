package gov.va.api.health.dataquery.service.controller.observation;

import static gov.va.api.health.dataquery.service.controller.observation.DatamartObservationSamples.Fhir.link;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import gov.va.api.health.argonaut.api.resources.Observation;
import gov.va.api.health.argonaut.api.resources.Observation.Bundle;
import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import gov.va.api.health.dataquery.service.controller.Bundler;
import gov.va.api.health.dataquery.service.controller.ConfigurableBaseUrlPageLinks;
import gov.va.api.health.dataquery.service.controller.ResourceExceptions;
import gov.va.api.health.dataquery.service.controller.WitnessProtection;
import gov.va.api.health.dataquery.service.controller.observation.DatamartObservation.Category;
import gov.va.api.health.dataquery.service.controller.observation.DatamartObservationSamples.Datamart;
import gov.va.api.health.dataquery.service.controller.observation.DatamartObservationSamples.Fhir;
import gov.va.api.health.dstu2.api.bundle.BundleLink.LinkRelation;
import gov.va.api.health.dstu2.api.datatypes.CodeableConcept;
import gov.va.api.health.dstu2.api.datatypes.Coding;
import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

@DataJpaTest
@RunWith(SpringRunner.class)
public class DatamartObservationControllerTest {

  private IdentityService ids = mock(IdentityService.class);

  @Autowired private ObservationRepository repository;

  @Autowired private TestEntityManager entityManager;

  @SneakyThrows
  private static ObservationEntity asEntity(DatamartObservation dm) {
    return ObservationEntity.builder()
        .cdwId(dm.cdwId())
        .icn(dm.subject().get().reference().get())
        .category(
            dm.category().equals(Category.vital_signs) ? "vital-signs" : dm.category().toString())
        .code(dm.code().get().coding().get().code().get())
        .epochTime(dm.effectiveDateTime().get().toEpochMilli())
        .payload(JacksonConfig.createMapper().writeValueAsString(dm))
        .build();
  }

  @SneakyThrows
  private static DatamartObservation toObject(String payload) {
    return JacksonConfig.createMapper().readValue(payload, DatamartObservation.class);
  }

  ObservationController controller() {
    return new ObservationController(
        true,
        null,
        null,
        new Bundler(new ConfigurableBaseUrlPageLinks("http://fonzy.com", "cool")),
        repository,
        WitnessProtection.builder().identityService(ids).build());
  }

  @SneakyThrows
  String json(Object o) {
    return JacksonConfig.createMapper().writerWithDefaultPrettyPrinter().writeValueAsString(o);
  }

  public void mockObservationIdentity(String publicId, String cdwId) {
    ResourceIdentity resourceIdentity =
        ResourceIdentity.builder().system("CDW").resource("OBSERVATION").identifier(cdwId).build();
    when(ids.lookup(publicId)).thenReturn(List.of(resourceIdentity));
    when(ids.register(Mockito.any()))
        .thenReturn(
            List.of(
                Registration.builder().uuid(publicId).resourceIdentity(resourceIdentity).build()));
  }

  private Multimap<String, Observation> populateData() {
    var fhir = Fhir.create();
    var datamart = Datamart.create();
    var observationsByPatient = LinkedHashMultimap.<String, Observation>create();
    var registrations = new ArrayList<Registration>(10);
    for (int i = 0; i < 10; i++) {
      var patientId = "p" + i % 2;
      var cdwId = "cdw-" + i;
      var publicId = "public" + i;
      var date = "2005-01-1" + i + "T07:57:00Z";
      DatamartObservation dm = datamart.observation(cdwId, patientId);
      if (i >= 5) {
        dm.category(Category.vital_signs);
      }
      if (i % 2 == 1) {
        dm.code().get().coding().get().code(Optional.of("8480-6"));
      }
      dm.effectiveDateTime(Optional.of(Instant.parse(date)));
      repository.save(asEntity(dm));
      Observation observation = fhir.observation(publicId, patientId);
      if (i >= 5) {
        observation.category(
            CodeableConcept.builder()
                .coding(
                    List.of(
                        Coding.builder()
                            .system("http://hl7.org/fhir/observation-category")
                            .code("vital-signs")
                            .display("Vital Signs")
                            .build()))
                .build());
      }
      if (i % 2 == 1) {
        observation.code().coding().get(0).code("8480-6");
      }
      observation.effectiveDateTime(date);
      observationsByPatient.put(patientId, observation);
      ResourceIdentity resourceIdentity =
          ResourceIdentity.builder()
              .system("CDW")
              .resource("OBSERVATION")
              .identifier(cdwId)
              .build();
      Registration registration =
          Registration.builder().uuid(publicId).resourceIdentity(resourceIdentity).build();
      registrations.add(registration);
      when(ids.lookup(publicId)).thenReturn(List.of(resourceIdentity));
    }
    when(ids.register(Mockito.any())).thenReturn(registrations);
    return observationsByPatient;
  }

  @Test
  public void read() {
    DatamartObservation dm = Datamart.create().observation();
    repository.save(asEntity(dm));
    mockObservationIdentity("x", dm.cdwId());
    Observation actual = controller().read("true", "x");
    assertThat(actual).isEqualTo(DatamartObservationSamples.Fhir.create().observation("x"));
  }

  @Test
  public void readRaw() {
    DatamartObservation dm = Datamart.create().observation();
    repository.save(asEntity(dm));
    mockObservationIdentity("x", dm.cdwId());
    String json = controller().readRaw("x");
    assertThat(toObject(json)).isEqualTo(dm);
  }

  @Test(expected = ResourceExceptions.NotFound.class)
  public void readRawThrowsNotFoundWhenDataIsMissing() {
    mockObservationIdentity("x", "x");
    controller().readRaw("x");
  }

  @Test(expected = ResourceExceptions.NotFound.class)
  public void readRawThrowsNotFoundWhenIdIsUnknown() {
    controller().readRaw("x");
  }

  @Test(expected = ResourceExceptions.NotFound.class)
  public void readThrowsNotFoundWhenDataIsMissing() {
    mockObservationIdentity("x", "x");
    controller().read("true", "x");
  }

  @Test(expected = ResourceExceptions.NotFound.class)
  public void readThrowsNotFoundWhenIdIsUnknown() {
    controller().readRaw("x");
  }

  @Test
  public void searchById() {
    DatamartObservation dm = Datamart.create().observation();
    repository.save(asEntity(dm));
    mockObservationIdentity("x", dm.cdwId());
    Bundle actual = controller().searchById("true", "x", 1, 1);
    Observation observation = Fhir.create().observation("x");
    assertThat(json(actual))
        .isEqualTo(
            json(
                Fhir.asBundle(
                    "http://fonzy.com/cool",
                    List.of(observation),
                    link(
                        LinkRelation.first, "http://fonzy.com/cool/Observation?identifier=x", 1, 1),
                    link(LinkRelation.self, "http://fonzy.com/cool/Observation?identifier=x", 1, 1),
                    link(
                        LinkRelation.last,
                        "http://fonzy.com/cool/Observation?identifier=x",
                        1,
                        1))));
    /* searchById and searchByIdentifier are the same */
    assertThat(controller().searchByIdentifier("true", "x", 1, 1)).isEqualTo(actual);
  }

  @Test(expected = ResourceExceptions.NotFound.class)
  public void searchByIdWhenIdIsUnknown() {
    controller().searchById("true", "x", 1, 1);
  }

  @Test
  public void searchByPatient() {
    Multimap<String, Observation> observationsByPatient = populateData();
    assertThat(json(controller().searchByPatient("true", "p0", 1, 10)))
        .isEqualTo(
            json(
                Fhir.asBundle(
                    "http://fonzy.com/cool",
                    observationsByPatient.get("p0"),
                    link(LinkRelation.first, "http://fonzy.com/cool/Observation?patient=p0", 1, 10),
                    link(LinkRelation.self, "http://fonzy.com/cool/Observation?patient=p0", 1, 10),
                    link(
                        LinkRelation.last,
                        "http://fonzy.com/cool/Observation?patient=p0",
                        1,
                        10))));
  }

  @Test
  public void searchByPatientAndCategory() {
    Multimap<String, Observation> observationsByPatient = populateData();
    assertThat(
            json(controller().searchByPatientAndCategory("true", "p0", "laboratory", null, 1, 10)))
        .isEqualTo(
            json(
                Fhir.asBundle(
                    "http://fonzy.com/cool",
                    observationsByPatient
                        .get("p0")
                        .stream()
                        .filter(
                            c -> "laboratory".equalsIgnoreCase(c.category().coding().get(0).code()))
                        .collect(Collectors.toList()),
                    link(
                        LinkRelation.first,
                        "http://fonzy.com/cool/Observation?category=laboratory&patient=p0",
                        1,
                        10),
                    link(
                        LinkRelation.self,
                        "http://fonzy.com/cool/Observation?category=laboratory&patient=p0",
                        1,
                        10),
                    link(
                        LinkRelation.last,
                        "http://fonzy.com/cool/Observation?category=laboratory&patient=p0",
                        1,
                        10))));
    assertThat(
            json(controller().searchByPatientAndCategory("true", "p1", "vital-signs", null, 1, 10)))
        .isEqualTo(
            json(
                Fhir.asBundle(
                    "http://fonzy.com/cool",
                    observationsByPatient
                        .get("p1")
                        .stream()
                        .filter(
                            c ->
                                "vital-signs".equalsIgnoreCase(c.category().coding().get(0).code()))
                        .collect(Collectors.toList()),
                    link(
                        LinkRelation.first,
                        "http://fonzy.com/cool/Observation?category=vital-signs&patient=p1",
                        1,
                        10),
                    link(
                        LinkRelation.self,
                        "http://fonzy.com/cool/Observation?category=vital-signs&patient=p1",
                        1,
                        10),
                    link(
                        LinkRelation.last,
                        "http://fonzy.com/cool/Observation?category=vital-signs&patient=p1",
                        1,
                        10))));
  }

  @Test
  public void searchByPatientAndCategoryAndOneDate() {
    /*
    This test exhaustively verifies all of the different date prefixes
    in combination with patient and category.

    Observation dates for p0
     2005-01-10T07:57:00Z -> laboratory
     2005-01-12T07:57:00Z -> laboratory
     2005-01-14T07:57:00Z -> laboratory
     2005-01-16T07:57:00Z -> vital-signs
     2005-01-18T07:57:00Z -> vital-signs

    Observation dates for p1
     2005-01-11T07:57:00Z -> laboratory
     2005-01-13T07:57:00Z -> laboratory
     2005-01-15T07:57:00Z -> vital-signs
     2005-01-17T07:57:00Z -> vital-signs
     2005-01-19T07:57:00Z -> vital-signs
    */
    Multimap<String, Observation> observationsByPatient = populateData();
    /*
    <criteria, expected dates>
    key: search criteria (prefix + date)
    value: expected date responses
     */
    Multimap<String, String> testDates = LinkedHashMultimap.create();
    testDates.putAll(
        "gt2004", List.of("2005-01-10T07:57:00Z", "2005-01-12T07:57:00Z", "2005-01-14T07:57:00Z"));
    testDates.putAll("eq2005-01-14", List.of("2005-01-14T07:57:00Z"));
    testDates.putAll("ne2005-01-14", List.of("2005-01-10T07:57:00Z", "2005-01-12T07:57:00Z"));
    testDates.putAll(
        "le2005-01-14",
        List.of("2005-01-10T07:57:00Z", "2005-01-12T07:57:00Z", "2005-01-14T07:57:00Z"));
    testDates.putAll("lt2005-01-14", List.of("2005-01-10T07:57:00Z", "2005-01-12T07:57:00Z"));
    testDates.putAll("eb2005-01-14", List.of("2005-01-10T07:57:00Z", "2005-01-12T07:57:00Z"));
    testDates.putAll("ge2005-01-14", List.of("2005-01-14T07:57:00Z"));
    testDates.putAll("gt2005-01-14", List.of());
    testDates.putAll("sa2005-01-14", List.of());
    for (var date : testDates.keySet()) {
      assertThat(
              json(
                  controller()
                      .searchByPatientAndCategory(
                          "true", "p0", "laboratory", new String[] {date}, 1, 10)))
          .isEqualTo(
              json(
                  Fhir.asBundle(
                      "http://fonzy.com/cool",
                      observationsByPatient
                          .get("p0")
                          .stream()
                          .filter(o -> testDates.get(date).contains(o.effectiveDateTime()))
                          .collect(Collectors.toList()),
                      link(
                          LinkRelation.first,
                          "http://fonzy.com/cool/Observation?category=laboratory&date="
                              + date
                              + "&patient=p0",
                          1,
                          10),
                      link(
                          LinkRelation.self,
                          "http://fonzy.com/cool/Observation?category=laboratory&date="
                              + date
                              + "&patient=p0",
                          1,
                          10),
                      link(
                          LinkRelation.last,
                          "http://fonzy.com/cool/Observation?category=laboratory&date="
                              + date
                              + "&patient=p0",
                          1,
                          10))));
    }
  }

  @Test
  public void searchByPatientAndCategoryAndTwoDates() {
    /*
    The single date test does exhaustive date-prefix searching. We won't do that here.

    Observation dates for p0
     2005-01-10T07:57:00Z -> laboratory
     2005-01-12T07:57:00Z -> laboratory
     2005-01-14T07:57:00Z -> laboratory
     2005-01-16T07:57:00Z -> vital-signs
     2005-01-18T07:57:00Z -> vital-signs

    Observation dates for p1
     2005-01-11T07:57:00Z -> laboratory
     2005-01-13T07:57:00Z -> laboratory
     2005-01-15T07:57:00Z -> vital-signs
     2005-01-17T07:57:00Z -> vital-signs
     2005-01-19T07:57:00Z -> vital-signs
     */
    Multimap<String, Observation> observationsByPatient = populateData();
    /*
    <criteria, expected dates>
    key: search criteria (prefix + date)
    value: expected date responses
     */
    Multimap<Pair<String, String>, String> testDates = LinkedHashMultimap.create();
    testDates.putAll(
        Pair.of("gt2004", "lt2006"),
        List.of("2005-01-10T07:57:00Z", "2005-01-12T07:57:00Z", "2005-01-14T07:57:00Z"));
    testDates.putAll(Pair.of("gt2005-01-13", "lt2005-01-15"), List.of("2005-01-14T07:57:00Z"));
    for (var date : testDates.keySet()) {
      assertThat(
              json(
                  controller()
                      .searchByPatientAndCategory(
                          "true",
                          "p0",
                          "laboratory",
                          new String[] {date.getLeft(), date.getRight()},
                          1,
                          10)))
          .isEqualTo(
              json(
                  Fhir.asBundle(
                      "http://fonzy.com/cool",
                      observationsByPatient
                          .get("p0")
                          .stream()
                          .filter(o -> testDates.get(date).contains(o.effectiveDateTime()))
                          .collect(Collectors.toList()),
                      link(
                          LinkRelation.first,
                          "http://fonzy.com/cool/Observation?category=laboratory&date="
                              + date.getLeft()
                              + "&date="
                              + date.getRight()
                              + "&patient=p0",
                          1,
                          10),
                      link(
                          LinkRelation.self,
                          "http://fonzy.com/cool/Observation?category=laboratory&date="
                              + date.getLeft()
                              + "&date="
                              + date.getRight()
                              + "&patient=p0",
                          1,
                          10),
                      link(
                          LinkRelation.last,
                          "http://fonzy.com/cool/Observation?category=laboratory&date="
                              + date.getLeft()
                              + "&date="
                              + date.getRight()
                              + "&patient=p0",
                          1,
                          10))));
    }
  }

  @Test
  public void searchByPatientAndCode() {
    Multimap<String, Observation> observationsByPatient = populateData();
    assertThat(json(controller().searchByPatientAndCode("true", "p0", "1989-3", 1, 10)))
        .isEqualTo(
            json(
                Fhir.asBundle(
                    "http://fonzy.com/cool",
                    observationsByPatient
                        .get("p0")
                        .stream()
                        .filter(c -> "1989-3".equalsIgnoreCase(c.code().coding().get(0).code()))
                        .collect(Collectors.toList()),
                    link(
                        LinkRelation.first,
                        "http://fonzy.com/cool/Observation?code=1989-3&patient=p0",
                        1,
                        10),
                    link(
                        LinkRelation.self,
                        "http://fonzy.com/cool/Observation?code=1989-3&patient=p0",
                        1,
                        10),
                    link(
                        LinkRelation.last,
                        "http://fonzy.com/cool/Observation?code=1989-3&patient=p0",
                        1,
                        10))));
    assertThat(json(controller().searchByPatientAndCode("true", "p1", "8480-6", 1, 10)))
        .isEqualTo(
            json(
                Fhir.asBundle(
                    "http://fonzy.com/cool",
                    observationsByPatient
                        .get("p1")
                        .stream()
                        .filter(c -> "8480-6".equalsIgnoreCase(c.code().coding().get(0).code()))
                        .collect(Collectors.toList()),
                    link(
                        LinkRelation.first,
                        "http://fonzy.com/cool/Observation?code=8480-6&patient=p1",
                        1,
                        10),
                    link(
                        LinkRelation.self,
                        "http://fonzy.com/cool/Observation?code=8480-6&patient=p1",
                        1,
                        10),
                    link(
                        LinkRelation.last,
                        "http://fonzy.com/cool/Observation?code=8480-6&patient=p1",
                        1,
                        10))));
  }

  @Test
  public void searchByPatientAndTwoCategoriesAndTwoDates() {
    /*
    Observation dates for p0
     2005-01-10T07:57:00Z -> laboratory
     2005-01-12T07:57:00Z -> laboratory
     2005-01-14T07:57:00Z -> laboratory
     2005-01-16T07:57:00Z -> vital-signs
     2005-01-18T07:57:00Z -> vital-signs

    Observation dates for p1
     2005-01-11T07:57:00Z -> laboratory
     2005-01-13T07:57:00Z -> laboratory
     2005-01-15T07:57:00Z -> vital-signs
     2005-01-17T07:57:00Z -> vital-signs
     2005-01-19T07:57:00Z -> vital-signs
    */
    Multimap<String, Observation> observationsByPatient = populateData();
    /*
    <criteria, expected dates>
    key: search criteria (prefix + date)
    value: expected date responses
     */
    Multimap<Pair<String, String>, String> testDates = LinkedHashMultimap.create();
    testDates.putAll(
        Pair.of("gt2004", "lt2006"),
        List.of(
            "2005-01-10T07:57:00Z",
            "2005-01-12T07:57:00Z",
            "2005-01-14T07:57:00Z",
            "2005-01-16T07:57:00Z",
            "2005-01-18T07:57:00Z"));
    testDates.putAll(
        Pair.of("gt2005-01-13", "lt2005-01-17"),
        List.of("2005-01-14T07:57:00Z", "2005-01-16T07:57:00Z"));
    for (var date : testDates.keySet()) {
      assertThat(
              json(
                  controller()
                      .searchByPatientAndCategory(
                          "true",
                          "p0",
                          "laboratory,vital-signs",
                          new String[] {date.getLeft(), date.getRight()},
                          1,
                          10)))
          .isEqualTo(
              json(
                  Fhir.asBundle(
                      "http://fonzy.com/cool",
                      observationsByPatient
                          .get("p0")
                          .stream()
                          .filter(o -> testDates.get(date).contains(o.effectiveDateTime()))
                          .collect(Collectors.toList()),
                      link(
                          LinkRelation.first,
                          "http://fonzy.com/cool/Observation?category=laboratory,vital-signs&date="
                              + date.getLeft()
                              + "&date="
                              + date.getRight()
                              + "&patient=p0",
                          1,
                          10),
                      link(
                          LinkRelation.self,
                          "http://fonzy.com/cool/Observation?category=laboratory,vital-signs&date="
                              + date.getLeft()
                              + "&date="
                              + date.getRight()
                              + "&patient=p0",
                          1,
                          10),
                      link(
                          LinkRelation.last,
                          "http://fonzy.com/cool/Observation?category=laboratory,vital-signs&date="
                              + date.getLeft()
                              + "&date="
                              + date.getRight()
                              + "&patient=p0",
                          1,
                          10))));
    }
  }

  /*
   TODO:
    - These tests
    - Updates to the toEntity() method to insert a set of mock ETLDate data.
   */

  @Test
  public void searchByIdandLastUpdated() {
    fail();
  }

  @Test
  public void searchByPatientandLastUpdated() {
    fail();
  }

  @Test
  public void searchByPatientAndCategoryAndDateAndLastUpdated() {
    fail();
  }

  @Test
  public void searchByPatientAndCodeAndLastUpdated() {
    fail();
  }
}
