package gov.va.api.health.dataquery.tests;

import static gov.va.api.health.sentinel.SentinelProperties.magicAccessToken;

import gov.va.api.health.dataquery.tests.TestIds.DiagnosticReports;
import gov.va.api.health.dataquery.tests.TestIds.Observations;
import gov.va.api.health.dataquery.tests.TestIds.PersonallyIdentifiableInformation;
import gov.va.api.health.dataquery.tests.TestIds.Procedures;
import gov.va.api.health.dataquery.tests.TestIds.Range;
import gov.va.api.health.sentinel.Environment;
import gov.va.api.health.sentinel.SentinelProperties;
import gov.va.api.health.sentinel.ServiceDefinition;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * {@link SystemDefinition}s for different environments. {@link #systemDefinition()} method provides
 * the appropriate implementation for the current environment.
 */
@UtilityClass
public final class SystemDefinitions {

  private static DiagnosticReports diagnosticReports() {
    return DiagnosticReports.builder()
        .loinc1("10000-8")
        .loinc2("10001-6")
        .badLoinc("99999-9")
        .onDate("eq1970-01-01")
        .fromDate("gt1970-01-01")
        .toDate("lt2038-01-01")
        .dateYear("ge1970")
        .dateYearMonth("ge1970-01")
        .dateYearMonthDay("ge1970-01-01")
        .dateYearMonthDayHour("ge1970-01-01T07")
        .dateYearMonthDayHourMinute("ge1970-01-01T07:00")
        .dateYearMonthDayHourMinuteSecond("ge1970-01-01T07:00:00")
        .dateYearMonthDayHourMinuteSecondTimezone("ge1970-01-01T07:00:00+05:00")
        .dateYearMonthDayHourMinuteSecondZulu("ge1970-01-01T07:00:00Z")
        .dateGreaterThan("ge1970-01-01")
        .dateNotEqual("ne1970-01-01")
        .dateStartsWith("sa1970-01-01")
        .dateNoPrefix("1970-01-01")
        .dateEqual("1970-01-01")
        .dateLessOrEqual("le2038-01-19")
        .dateLessThan("lt2038-01-19")
        .build();
  }

  /** Return definitions for the lab environment. */
  private static SystemDefinition lab() {
    String url = "https://dev-api.va.gov";
    return SystemDefinition.builder()
        .ids(serviceDefinition("ids", url, 443, null, "/not-available/"))
        .mrAnderson(serviceDefinition("mr-anderson", url, 443, null, "/not-available/"))
        .dataQuery(
            serviceDefinition("argonaut", url, 443, magicAccessToken(), "/services/argonaut/v0/"))
        .cdwIds(labMitreIds())
        .build();
  }

  private static TestIds labMitreIds() {
    return TestIds.builder()
        .publicIds(true)
        .allergyIntolerance("2f7241a3-2f43-58f0-a6e7-ebb85fdf3f84")
        .condition("0a812fa6-318b-5f8e-84fe-828ed8448be4")
        .diagnosticReport("580973dd-2f9a-57ac-b2e4-5897ad1c4322")
        .diagnosticReports(diagnosticReports())
        .immunization("1b350f07-a1ce-5078-bb60-5d0122fbec50")
        .medication("30c08673-77e0-5acd-b334-cd5ba153d86d")
        .medicationOrder("0e4d47c4-dbf1-514b-b0ec-1f29bacaa13b")
        .medicationStatement("08578f3e-17ea-5454-b88a-4706ab54a95f")
        .observation("02ef60c8-ab65-5322-9cbb-db01083ec245")
        .observations(observations())
        .patient("1011537977V693883")
        .procedure("02b9078b-9665-52ff-b360-9d618ac34df0")
        .procedures(procedures())
        .location("unused")
        .appointment("unused")
        .medicationDispense("unused")
        .encounter("unused")
        .organization("unused")
        .practitioner("unused")
        .unknown("5555555555555")
        .build();
  }

  /**
   * Return system definitions for local running applications as started by the Maven build process.
   */
  private static SystemDefinition local() {
    String url = "https://localhost";
    return SystemDefinition.builder()
        .ids(serviceDefinition("ids", url, 8089, null, "/api/"))
        .mrAnderson(serviceDefinition("mr-anderson", url, 8088, null, "/api/"))
        .dataQuery(serviceDefinition("argonaut", url, 8090, null, "/"))
        .cdwIds(localIds())
        .build();
  }

  private static TestIds localIds() {
    return TestIds.builder()
        .publicIds(false)
        .allergyIntolerance("1000001782544")
        .appointment("1200438317388")
        .condition("1400007575530:P")
        .diagnosticReport("1000000031384:L")
        .encounter("1200753214085")
        .diagnosticReports(diagnosticReports())
        .immunization("1000000043979")
        .location("166365:L")
        .medication("212846")
        .medicationDispense("1200738474343:R")
        .medicationOrder("1200389904206:O")
        .medicationStatement("1400000182116")
        .observation("1201051417263:V")
        .observations(observations())
        .organization("1000025431:C")
        .pii(
            PersonallyIdentifiableInformation.builder()
                .gender("male")
                .birthdate("1970-01-01")
                .given("JOHN Q")
                .name("VETERAN,JOHN")
                .family("VETERAN")
                .build())
        .patient("185601V825290")
        .practitioner("10092125")
        .procedure("1400000140034")
        .procedures(procedures())
        .unknown("5555555555555")
        .build();
  }

  private static Observations observations() {
    return Observations.builder()
        .loinc1("72166-2")
        .loinc2("777-3")
        .badLoinc("99999-9")
        .onDate("2015-04-15")
        .dateRange(Range.allTime())
        .build();
  }

  private static Procedures procedures() {
    return Procedures.builder().fromDate("ge2009").onDate("ge2009").toDate("le2010").build();
  }

  /** Return definitions for the production environment. */
  private static SystemDefinition prod() {
    // Mr Anderson not accessible in this environment
    String url = "https://argonaut.lighthouse.va.gov";
    return SystemDefinition.builder()
        .ids(serviceDefinition("ids", url, 443, null, "/not-available/"))
        .mrAnderson(serviceDefinition("mr-anderson", url, 443, null, "/not-available/"))
        .dataQuery(serviceDefinition("argonaut", url, 443, magicAccessToken(), "/"))
        .cdwIds(productionCdwIds())
        .build();
  }

  private static TestIds productionCdwIds() {
    return TestIds.builder()
        .publicIds(true)
        .allergyIntolerance("3be00408-b0ff-598d-8ba1-1e0bbfb02b99")
        .appointment("f7721341-03ad-56cf-b0e5-e96fded23a1b")
        .condition("ea59bc29-d507-571b-a4c6-9ac0d2146c45")
        .diagnosticReport("0bca2c42-8d23-5d36-90b8-81a8b12bb1b5")
        .diagnosticReports(diagnosticReports())
        .encounter("05d66afc-3a1a-5277-8b26-a8084ac46a08")
        .immunization("00f4000a-b1c9-5190-993a-644569d2722b")
        .location("a146313b-9a77-5337-a442-bee6ceb4aa5c")
        .medication("89a46bce-8b95-5a91-bbef-1fb5f8a2a292")
        .medicationDispense("773bb1ab-4430-5012-b203-a88c41c5dde9")
        .medicationOrder("91f4a9d2-e7fa-5b34-a875-6d75761221c7")
        .medicationStatement("e4573ebc-40e4-51bb-9da1-20a91b31ff24")
        .observation("40e2ced6-32e2-503e-85b8-198690f6611b")
        .observations(observations())
        .organization("3e5dbe7a-72ca-5441-9287-0b639ae7a1bc")
        .patient("1011537977V693883")
        .practitioner("7b4c6b83-2c5a-5cbf-836c-875253fb9bf9")
        .procedure("c416df15-fc1d-5a04-ab11-34d7bf453d15")
        .procedures(procedures())
        .unknown("5555555555555")
        .build();
  }

  /** Return definitions for the qa environment. */
  private static SystemDefinition qa() {
    // ID service and Mr Anderson not accessible in this environment
    String url = "https://blue.qa.lighthouse.va.gov";
    return SystemDefinition.builder()
        .ids(serviceDefinition("ids", url, 443, null, "/not-available/"))
        .mrAnderson(serviceDefinition("mr-anderson", url, 443, null, "/not-available/"))
        .dataQuery(serviceDefinition("argonaut", url, 443, magicAccessToken(), "/"))
        .cdwIds(productionCdwIds())
        .build();
  }

  private static ServiceDefinition serviceDefinition(
      String name, String url, int port, String accessToken, String apiPath) {
    return ServiceDefinition.builder()
        .url(SentinelProperties.optionUrl(name, url))
        .port(port)
        .accessToken(() -> Optional.ofNullable(accessToken))
        .apiPath(SentinelProperties.optionApiPath(name, apiPath))
        .build();
  }

  /** Return definitions for the staging environment. */
  private static SystemDefinition staging() {
    // ID service and Mr Anderson not accessible in this environment
    String url = "https://blue.staging.lighthouse.va.gov";
    return SystemDefinition.builder()
        .ids(serviceDefinition("ids", url, 443, null, "/not-available/"))
        .mrAnderson(serviceDefinition("mr-anderson", url, 443, null, "/not-available/"))
        .dataQuery(serviceDefinition("argonaut", url, 443, magicAccessToken(), "/"))
        .cdwIds(productionCdwIds())
        .build();
  }

  /** Return definitions for the lab environment. */
  private static SystemDefinition stagingLab() {
    String url = "https://blue.staging-lab.lighthouse.va.gov";
    return SystemDefinition.builder()
        .ids(serviceDefinition("ids", url, 443, null, "/not-available/"))
        .mrAnderson(serviceDefinition("mr-anderson", url, 443, null, "/not-available/"))
        .dataQuery(
            serviceDefinition("argonaut", url, 443, magicAccessToken(), "/services/argonaut/v0/"))
        .cdwIds(labMitreIds())
        .build();
  }

  /** Return the applicable system definition for the current environment. */
  public static SystemDefinition systemDefinition() {
    switch (Environment.get()) {
      case LAB:
        return lab();
      case LOCAL:
        return local();
      case PROD:
        return prod();
      case QA:
        return qa();
      case STAGING:
        return staging();
      case STAGING_LAB:
        return stagingLab();
      default:
        throw new IllegalArgumentException("Unknown sentinel environment: " + Environment.get());
    }
  }
}