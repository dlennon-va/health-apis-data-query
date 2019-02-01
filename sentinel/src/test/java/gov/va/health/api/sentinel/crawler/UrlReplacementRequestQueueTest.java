package gov.va.health.api.sentinel.crawler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class UrlReplacementRequestQueueTest {

  UrlReplacementRequestQueue rq =
      UrlReplacementRequestQueue.builder()
          .replaceUrl("https://dev-api.va.gov/services/argonaut/v0/")
          .withUrl("https://staging-argonaut.lighthouse.va.gov/api/")
          .requestQueue(new ConcurrentRequestQueue())
          .build();

  @Test(expected = IllegalStateException.class)
  public void emptyBaseUrlThrowsIllegalStateException() {
    UrlReplacementRequestQueue emptyBaseUrl =
        UrlReplacementRequestQueue.builder()
            .replaceUrl("")
            .withUrl("https://staging-argonaut.lighthouse.va.gov/api/")
            .requestQueue(new ConcurrentRequestQueue())
            .build();
    emptyBaseUrl.add("Empty forceUrl");
  }

  @Test(expected = IllegalStateException.class)
  public void emptyForceUrlThrowsIllegalStateException() {
    UrlReplacementRequestQueue emptyBaseUrl =
        UrlReplacementRequestQueue.builder()
            .replaceUrl("https://dev-api.va.gov/services/argonaut/v0/")
            .withUrl("")
            .requestQueue(new ConcurrentRequestQueue())
            .build();
    emptyBaseUrl.add("Empty forceUrl");
  }

  @Test(expected = IllegalStateException.class)
  public void exceptionIsThrownWhenAttemptingToGetNextFromEmptyQueue() {
    rq.add("x");
    rq.next();
    rq.next();
  }

  @Test(expected = IllegalStateException.class)
  public void exceptionIsThrownWhenAttemptingToGetNextQueueThatWasNeverUsed() {
    rq.next();
  }

  @Test
  public void hasNextReturnsFalseForEmptyQueue() {
    assertThat(rq.hasNext()).isFalse();
    rq.add("x");
    rq.next();
    assertThat(rq.hasNext()).isFalse();
  }

  @Test
  public void itemsAreRemovedFromQueueInOrderOfAddition() {
    rq.add("a");
    rq.add("b");
    rq.add("c");
    assertThat(rq.hasNext()).isTrue();
    assertThat(rq.next()).isEqualTo("a");
    assertThat(rq.hasNext()).isTrue();
    assertThat(rq.next()).isEqualTo("b");
    assertThat(rq.hasNext()).isTrue();
    assertThat(rq.next()).isEqualTo("c");
    assertThat(rq.hasNext()).isFalse();
  }

  @Test
  public void forceUrlReplacesBaseUrl() {
    rq.add(
        "https://dev-api.va.gov/services/argonaut/v0/AllergyIntolerance/3be00408-b0ff-598d-8ba1-1e0bbfb02b99");
    String expected =
        "https://staging-argonaut.lighthouse.va.gov/api/AllergyIntolerance/3be00408-b0ff-598d-8ba1-1e0bbfb02b99";
    assertThat(rq.next()).isEqualTo(expected);
  }

  @Test(expected = IllegalStateException.class)
  public void nullBaseUrlThrowsIllegalStateException() {
    UrlReplacementRequestQueue emptyBaseUrl =
        UrlReplacementRequestQueue.builder()
            .replaceUrl(null)
            .withUrl("https://staging-argonaut.lighthouse.va.gov/api/")
            .requestQueue(new ConcurrentRequestQueue())
            .build();
    emptyBaseUrl.add("Empty baseUrl");
  }

  @Test(expected = IllegalStateException.class)
  public void nullForceUrlThrowsIllegalStateException() {
    UrlReplacementRequestQueue emptyBaseUrl =
        UrlReplacementRequestQueue.builder()
            .replaceUrl("https://dev-api.va.gov/services/argonaut/v0/")
            .withUrl(null)
            .requestQueue(new ConcurrentRequestQueue())
            .build();
    emptyBaseUrl.add("Empty forceUrl");
  }

  @Test
  public void theSameItemCannotBeAddedToTheQueueTwice() {
    rq.add("a");
    rq.add("b");
    rq.add("c");
    rq.add("a"); // ignored
    assertThat(rq.hasNext()).isTrue();
    assertThat(rq.next()).isEqualTo("a");
    rq.add("a"); // ignored
    assertThat(rq.hasNext()).isTrue();
    assertThat(rq.next()).isEqualTo("b");
    assertThat(rq.hasNext()).isTrue();
    assertThat(rq.next()).isEqualTo("c");
    assertThat(rq.hasNext()).isFalse();
  }
}