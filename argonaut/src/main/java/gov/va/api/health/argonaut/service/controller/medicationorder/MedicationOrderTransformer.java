package gov.va.api.health.argonaut.service.controller.medicationorder;

import static gov.va.api.health.argonaut.service.controller.Transformers.allNull;
import static gov.va.api.health.argonaut.service.controller.Transformers.asDateTimeString;
import static gov.va.api.health.argonaut.service.controller.Transformers.convert;
import static gov.va.api.health.argonaut.service.controller.Transformers.convertAll;
import static gov.va.api.health.argonaut.service.controller.Transformers.ifPresent;
import static org.apache.commons.lang3.StringUtils.isBlank;

import gov.va.api.health.argonaut.api.datatypes.CodeableConcept;
import gov.va.api.health.argonaut.api.datatypes.Coding;
import gov.va.api.health.argonaut.api.datatypes.Duration;
import gov.va.api.health.argonaut.api.datatypes.SimpleQuantity;
import gov.va.api.health.argonaut.api.datatypes.Timing;
import gov.va.api.health.argonaut.api.elements.Reference;
import gov.va.api.health.argonaut.api.resources.MedicationOrder;
import gov.va.api.health.argonaut.api.resources.MedicationOrder.DispenseRequest;
import gov.va.api.health.argonaut.api.resources.MedicationOrder.DosageInstruction;
import gov.va.api.health.argonaut.api.resources.MedicationOrder.Status;
import gov.va.api.health.argonaut.service.controller.EnumSearcher;
import gov.va.dvp.cdw.xsd.model.CdwCodeableConcept;
import gov.va.dvp.cdw.xsd.model.CdwCoding;
import gov.va.dvp.cdw.xsd.model.CdwDuration;
import gov.va.dvp.cdw.xsd.model.CdwMedicationOrder103Root.CdwMedicationOrders.CdwMedicationOrder;
import gov.va.dvp.cdw.xsd.model.CdwMedicationOrder103Root.CdwMedicationOrders.CdwMedicationOrder.CdwDispenseRequest;
import gov.va.dvp.cdw.xsd.model.CdwMedicationOrder103Root.CdwMedicationOrders.CdwMedicationOrder.CdwDosageInstructions;
import gov.va.dvp.cdw.xsd.model.CdwMedicationOrder103Root.CdwMedicationOrders.CdwMedicationOrder.CdwDosageInstructions.CdwDosageInstruction;
import gov.va.dvp.cdw.xsd.model.CdwMedicationOrder103Root.CdwMedicationOrders.CdwMedicationOrder.CdwDosageInstructions.CdwDosageInstruction.CdwRoute;
import gov.va.dvp.cdw.xsd.model.CdwMedicationOrder103Root.CdwMedicationOrders.CdwMedicationOrder.CdwDosageInstructions.CdwDosageInstruction.CdwTiming;
import gov.va.dvp.cdw.xsd.model.CdwReference;
import gov.va.dvp.cdw.xsd.model.CdwSimpleQuantity;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MedicationOrderTransformer implements MedicationOrderController.Transformer {
  CodeableConcept additionalInstructions(CdwCodeableConcept source) {
    if (source == null) {
      return null;
    }
    if (source.getCoding().isEmpty() && isBlank(source.getText())) {
      return null;
    }
    return CodeableConcept.builder()
        .coding(additionalInstructionsCodings(source.getCoding()))
        .text(source.getText())
        .build();
  }

  List<Coding> additionalInstructionsCodings(List<CdwCoding> source) {
    List<Coding> codings = convertAll(source, this::additionalInstructionsCoding);
    return codings == null || codings.isEmpty() ? null : codings;
  }

  private Coding additionalInstructionsCoding(CdwCoding cdw) {
    if (cdw == null || allNull(cdw.getCode(), cdw.getDisplay(), cdw.getSystem())) {
      return null;
    }
    return Coding.builder()
        .system(cdw.getSystem())
        .code(cdw.getCode())
        .display(cdw.getDisplay())
        .build();
  }

  @Override
  public MedicationOrder apply(CdwMedicationOrder source) {
    return medicationOrder(source);
  }

  DispenseRequest dispenseRequest(CdwDispenseRequest cdw) {
    if (cdw == null
        || allNull(
            cdw.getNumberOfRepeatsAllowed(), cdw.getQuantity(), cdw.getExpectedSupplyDuration())) {
      return null;
    }
    return convert(
        cdw,
        source ->
            DispenseRequest.builder()
                .numberOfRepeatsAllowed(numberOfRepeatsAllowed(source.getNumberOfRepeatsAllowed()))
                .quantity(quantity(source.getQuantity()))
                .expectedSupplyDuration(expectedSupplyDuration(source.getExpectedSupplyDuration()))
                .build());
  }

  DosageInstruction dosageInstruction(CdwDosageInstruction cdw) {
    if (cdw == null
        || allNull(
            cdw.getAdditionalInstructions(),
            cdw.getAsNeededBoolean(),
            cdw.getDoseQuantity(),
            cdw.getRoute(),
            cdw.getText(),
            cdw.getTiming())) {
      return null;
    }
    return convert(
        cdw,
        source ->
            DosageInstruction.builder()
                .text(source.getText())
                .additionalInstructions(additionalInstructions(source.getAdditionalInstructions()))
                .doseQuantity(doseQuantity(source.getDoseQuantity()))
                .timing(timing(source.getTiming()))
                .asNeededBoolean(Boolean.valueOf(source.getAsNeededBoolean()))
                .route(route(source.getRoute()))
                .build());
  }

  List<DosageInstruction> dosageInstructions(CdwDosageInstructions cdw) {
    if (cdw == null || cdw.getDosageInstruction().isEmpty()) {
      return null;
    }
    return convertAll(
        ifPresent(cdw, CdwDosageInstructions::getDosageInstruction), this::dosageInstruction);
  }

  SimpleQuantity doseQuantity(CdwSimpleQuantity source) {
    if (source == null
        || allNull(source.getCode(), source.getSystem(), source.getUnit(), source.getValue())) {
      return null;
    }
    return SimpleQuantity.builder()
        .value(doseQuantityValue(source.getValue()))
        .unit(source.getUnit())
        .code(source.getCode())
        .system(source.getSystem())
        .build();
  }

  Double doseQuantityValue(String source) {
    Double value;
    if (source == null || isBlank(source)) {
      return null;
    }
    try {
      value = Double.valueOf(source);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Cannot create double value from " + source, e);
    }
    return value;
  }

  Duration expectedSupplyDuration(CdwDuration cdw) {
    if (cdw == null || allNull(cdw.getUnit(), cdw.getSystem(), cdw.getCode(), cdw.getValue())) {
      return null;
    }
    return convert(
        cdw,
        source ->
            Duration.builder()
                .value(source.getValue().doubleValue())
                .unit(source.getUnit())
                .system(source.getSystem())
                .code(source.getCode())
                .build());
  }

  MedicationOrder medicationOrder(CdwMedicationOrder source) {
    return MedicationOrder.builder()
        .id(source.getCdwId())
        .resourceType("MedicationOrder")
        .patient(reference(source.getPatient()))
        .dateWritten(asDateTimeString(source.getDateWritten()))
        .status(status(source.getStatus()))
        .dateEnded(asDateTimeString(source.getDateEnded()))
        .prescriber(reference(source.getPrescriber()))
        .medicationReference(reference(source.getMedicationReference()))
        .dosageInstruction(dosageInstructions(source.getDosageInstructions()))
        .dispenseRequest(dispenseRequest(source.getDispenseRequest()))
        .build();
  }

  Integer numberOfRepeatsAllowed(Integer source) {
    if (source == null || source <= 0) {
      return null;
    }
    return source;
  }

  SimpleQuantity quantity(String source) {
    if (source == null || isBlank(source)) {
      return null;
    }
    Double value;
    try {
      value = Double.valueOf(source);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Cannot create double value from " + source, e);
    }
    return SimpleQuantity.builder().value(value).build();
  }

  Reference reference(CdwReference maybeSource) {
    if (maybeSource == null || allNull(maybeSource.getDisplay(), maybeSource.getReference())) {
      return null;
    }
    return convert(
        maybeSource,
        source ->
            Reference.builder()
                .display(source.getDisplay())
                .reference(source.getReference())
                .build());
  }

  CodeableConcept route(CdwRoute source) {
    if (source == null || source.getText() == null) {
      return null;
    }
    return CodeableConcept.builder().text(source.getText()).build();
  }

  Status status(String source) {
    if (source == null || isBlank(source)) {
      return null;
    }
    return convert(source, status -> EnumSearcher.of(MedicationOrder.Status.class)).find(source);
  }

  Timing timing(CdwTiming source) {
    if (source == null || source.getCode() == null) {
      return null;
    }
    return Timing.builder().code(timingCode(source.getCode())).build();
  }

  CodeableConcept timingCode(CdwCodeableConcept source) {
    if (source == null) {
      return null;
    }
    if (source.getCoding().isEmpty() && isBlank(source.getText())) {
      return null;
    }
    return CodeableConcept.builder()
        .text(source.getText())
        .coding(timeCodeCodings(source.getCoding()))
        .build();
  }

  List<Coding> timeCodeCodings(List<CdwCoding> source) {
    List<Coding> codings = convertAll(source, this::timingCodeCoding);
    if (source == null) {
      return null;
    }
    return source.isEmpty() ? null : codings;
  }

  private Coding timingCodeCoding(CdwCoding cdw) {
    if (cdw == null || allNull(cdw.getCode(), cdw.getDisplay(), cdw.getSystem())) {
      return null;
    }
    return Coding.builder()
        .system(cdw.getSystem())
        .code(cdw.getCode())
        .display(cdw.getDisplay())
        .build();
  }
}