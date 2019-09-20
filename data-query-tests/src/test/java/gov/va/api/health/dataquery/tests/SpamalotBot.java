package gov.va.api.health.dataquery.tests;

import static gov.va.api.health.sentinel.LabBot.allUsers;

import com.google.common.collect.Streams;
import gov.va.api.health.sentinel.LabBot;
import gov.va.api.health.sentinel.LabBot.LabBotUserResult;
import gov.va.api.health.sentinel.selenium.VaOauthRobot.TokenExchange;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Builder
@Slf4j
public class SpamalotBot {

  @Singular List<String> urls;
  int times;
  ExecutorService executor = Executors.newFixedThreadPool(10);
  final List<String> failures = new LinkedList<>();

  public static void main(String[] args) {
    SpamalotBot spamalot =
        SpamalotBot.builder()
            .times(3)
            .url("https://dev-api.va.gov/services/fhir/v0/dstu2/Patient/{icn}")
            .url("https://dev-api.va.gov/services/fhir/v0/dstu2/Immunization?patient={icn}")
            .build();

    spamalot.spam();

    if (!spamalot.failures().isEmpty()) {
      log.error("OH NOES!");
      spamalot.failures().forEach(f -> log.error(f));
      System.exit(1);
    }
  }

  void failedToAuthenticate(LabBotUserResult result) {
    // TODO whatever authentication failure reporting you want
    failures.add("Failed to authenticate:" + result);
    throw new RuntimeException("OH NOES!");
  }

  private String failedToMakeRequest(String url, TokenExchange t) {
    // TODO whatever error reporting you might want here
    failures.add("Failed to make request: " + url);
    return "FAILED: " + url + " " + t;
  }

  private void failedToMakeRequestBecauseOfExceptions(Exception e) {
    // TODO whatever error reporting you want
    failures.add("Exception while making request: " + e.getMessage());
    log.error("FAILED: {}", e);
  }

  private Callable<String> makeRequest(LabBot labbot, String url, TokenExchange t) {
    return () -> {
      log.info("Making request {}", url);
      try {
        return labbot.request(url, t.accessToken());
      } catch (Exception e) {
        return failedToMakeRequest(url, t);
      }
    };
  }

  private Stream<Callable<String>> makeRequests(LabBot labbot, List<LabBotUserResult> tokens) {
    return tokens.stream()
        .map(LabBotUserResult::tokenExchange)
        .flatMap(
            t ->
                urls().stream()
                    .map(u -> u.replace("{icn}", t.patient()))
                    .map(u -> makeRequest(labbot, u, t)));
  }

  private void spam() {
    LabBot labbot =
        LabBot.builder()
            .userIds(userIds())
            .scopes(DataQueryScopes.labResources())
            .configFile("config/lab.properties")
            .build();
    List<LabBotUserResult> tokens = labbot.tokens();
    tokens.stream()
        .filter(t -> t.tokenExchange().isError())
        .findFirst()
        .ifPresent(this::failedToAuthenticate);
    Stream<Future<String>> requests = Stream.of();
    for (int i = 0; i < times; i++) {
      requests =
          Streams.concat(requests, makeRequests(labbot, tokens).map(r -> executor.submit(r)));
    }
    List<Future<String>> responses = requests.collect(Collectors.toList());

    responses.forEach(
        f -> {
          try {
            log.info("{}", f.get());
          } catch (InterruptedException | ExecutionException e) {
            failedToMakeRequestBecauseOfExceptions(e);
          }
        });
  }

  private List<String> userIds() {
    return allUsers().subList(0, 3);
  }
}
