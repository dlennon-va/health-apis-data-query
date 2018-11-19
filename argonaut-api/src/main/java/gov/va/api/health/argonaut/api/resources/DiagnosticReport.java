package gov.va.api.health.argonaut.api.resources;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gov.va.api.health.argonaut.api.Fhir;
import gov.va.api.health.argonaut.api.bundle.AbstractBundle;
import gov.va.api.health.argonaut.api.bundle.AbstractEntry;
import gov.va.api.health.argonaut.api.bundle.BundleLink;
import gov.va.api.health.argonaut.api.datatypes.*;
import gov.va.api.health.argonaut.api.elements.*;
import gov.va.api.health.argonaut.api.validation.ZeroOrOneOf;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Schema(
    description =
        "http://www.fhir.org/guides/argonaut/r2/StructureDefinition-argo-diagnosticreport.html")
@ZeroOrOneOf(
    fields = {"effectiveDateTime", "effectivePeriod"},
    message = "Only one effective value may be specified")
public class DiagnosticReport implements Resource {

  @Pattern(regexp = Fhir.ID)
  String id;

  @NotBlank String resourceType;
  @Valid Meta meta;

  @Pattern(regexp = Fhir.URI)
  String implicitRules;

  @Pattern(regexp = Fhir.CODE)
  String language;

  @Valid Narrative text;
  @Valid List<SimpleResource> contained;
  @Valid List<Extension> extension;
  @Valid List<Extension> modifierExtension;
  @Valid List<Identifier> identifier;

  @NotNull Code status;
  @NotNull @Valid CodeableConcept category;
  @NotNull @Valid CodeableConcept code;
  @NotNull @Valid Reference subject;

  @Valid Reference encounter;

  @Pattern(regexp = Fhir.DATETIME)
  String effectiveDateTime;

  @Valid Period effectivePeriod;

  @Pattern(regexp = Fhir.INSTANT)
  @NotNull
  @Valid
  String issued;

  @NotNull @Valid Reference performer;

  @Valid List<Reference> request;
  @Valid List<Reference> specimen;

  @NotEmpty @Valid List<Reference> result;

  @Valid List<Reference> imagingStudy;
  @Valid List<Image> image;

  String conclusion;

  @Valid List<CodeableConcept> codedDiagnosis;
  @Valid List<Attachment> presentedForm;

  @JsonIgnore
  @AssertTrue(message = "Category Coding is not valid.")
  private boolean isValidCategory() {
      if(category == null) {
          return true;
      }
      return StringUtils.equals("http://hl7.org/fhir/ValueSet/diagnostic-service-sections", (category.coding().get(0).system()));
  }

  @Data
  @Builder
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  @AllArgsConstructor
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public static class Image implements BackboneElement {

    @Pattern(regexp = Fhir.ID)
    String id;

    @Valid List<Extension> modifierExtension;
    @Valid List<Extension> extension;
    String comment;

    @Valid @NotNull Reference link;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  @JsonDeserialize(builder = DiagnosticReport.Bundle.BundleBuilder.class)
  public static class Bundle extends AbstractBundle<DiagnosticReport.Entry> {

    @Builder
    public Bundle(
        @Pattern(regexp = Fhir.ID) String id,
        @Valid Meta meta,
        @Pattern(regexp = Fhir.URI) String implicitRules,
        @Pattern(regexp = Fhir.CODE) String language,
        @NotNull BundleType type,
        @Min(0) Integer total,
        @Valid List<BundleLink> link,
        @Valid List<Entry> entry,
        @NotBlank String resourceType,
        @Valid Signature signature) {
      super(id, meta, implicitRules, language, type, total, link, entry, resourceType, signature);
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  @JsonDeserialize(builder = DiagnosticReport.Entry.EntryBuilder.class)
  public static class Entry extends AbstractEntry<DiagnosticReport> {

    @Builder
    public Entry(
        @Pattern(regexp = Fhir.ID) String id,
        @Valid List<Extension> extension,
        @Valid List<Extension> modifierExtension,
        @Valid List<BundleLink> link,
        @Pattern(regexp = Fhir.URI) String fullUrl,
        @Valid DiagnosticReport resource,
        @Valid Search search,
        @Valid Request request,
        @Valid Response response) {
      super(id, extension, modifierExtension, link, fullUrl, resource, search, request, response);
    }
  }

  public enum Code {
    partial,
    @JsonProperty("final")
    _final,
    corrected,
    appended,
    cancelled,
    @JsonProperty("entered-in-error")
    entered_in_error
  }
}
