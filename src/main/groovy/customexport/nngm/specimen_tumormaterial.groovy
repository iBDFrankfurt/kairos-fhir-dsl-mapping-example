package customexport.nngm

import de.kairos.fhir.centraxx.metamodel.*
import org.hl7.fhir.r4.model.Observation

import static de.kairos.fhir.centraxx.metamodel.AbstractSample.PARENT
import static de.kairos.fhir.centraxx.metamodel.RootEntities.abstractSample
import static de.kairos.fhir.centraxx.metamodel.RootEntities.sample

/**
 * Profile: http://uk-koeln.de/fhir/StructureDefinition/nNGM/Specimen
 */
specimen {

    final String sampleId = context.source[abstractSample().id()]
    final String sampleTypeCode = context.source[abstractSample().sampleType().code()] as String

    System.out.println("nNGM-Fhir-Specimen - Processing Sample ID: ${sampleId} (Type: ${sampleTypeCode})")

    // Filter out restricted types
    if (matchIgnoreCase(["TBL", "LES", "UBK", "ZZZ", "NRT"], sampleTypeCode)) {
        System.out.println("nNGM-Fhir-Specimen - Skipping Sample based on Type code: ${sampleTypeCode}")
        return
    }

    id = "Specimen/" + sampleId

    meta {
        source = "urn:centraxx"
        profile "http://uk-koeln.de/fhir/StructureDefinition/nNGM/Specimen"
    }

    // Identifiers (e.g. PSN)
    context.source[sample().idContainer()].each { final def idObj ->
        if (idObj) {
            identifier {
                value = idObj[AbstractIdContainer.PSN]
                system = "urn:centraxx"
            }
        }
    }

    // Biopsie ID
    if (context.source[abstractSample().histoNumber()]) {
        System.out.println("nNGM-Fhir-Specimen - Mapping Biopsie-ID (HistoNumber): " + context.source[abstractSample().histoNumber()])
        identifier {
            type {
                coding {
                    system = "http://terminology.hl7.org/CodeSystem/v2-0203"
                    code = "SID"
                    display = "Specimen ID"
                }
            }
            value = context.source[abstractSample().histoNumber()]
        }
    }

    status = context.source[abstractSample().restAmount().amount()] > 0 ? "available" : "unavailable"

    // Parent Link (Aliquots)
    if (context.source[PARENT] != null) {
        parent {
            reference = "Specimen/" + context.source[sample().parent().id()]
        }
    }

    // Specimen.type
    type {
        final String sampleKind = context.source[abstractSample().sampleType().kind()] as String
        final String stockType = context.source[abstractSample().stockType().code()] as String
        final String ncitCode = codeToSampleType(sampleTypeCode, stockType, sampleKind)

        if (ncitCode != null) {
            System.out.println("nNGM-Fhir-Specimen - Mapping NCIT Code: ${ncitCode}")
            coding {
                system = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl"
                code = ncitCode
            }
        }
        text = "Centraxx Type: " + sampleTypeCode
    }

    subject {
        reference = "Patient/" + context.source[abstractSample().patientContainer().id()]
    }

    // Entnahmedatum
    final def sDate = context.source[abstractSample().samplingDate().date()]
    if (sDate) {
        collection {
            collectedDateTime = normalizeDate(sDate as String)
        }
    }

    BigDecimal tumorCellValue = null

    // Deep search for finding based Tumor Cell Content
    context.source[abstractSample().laborMappings()]?.each { final mapping ->
        final def finding = mapping[LaborMapping.LABOR_FINDING]
        finding?.getAt(LaborFinding.LABOR_FINDING_LABOR_VALUES)?.each { final lflv ->
            final def lv = lflv[LaborFindingLaborValue.CRF_TEMPLATE_FIELD]?.getAt(CrfTemplateField.LABOR_VALUE) ?:
                    lflv[LaborFindingLaborValue.LABOR_VALUE]

            if (lv?.getAt(AbstractCode.CODE) == "Tumorzellgehalt") {
                tumorCellValue = lflv[LaborFindingLaborValue.NUMERIC_VALUE] as BigDecimal
            }
        }
    }

    if (tumorCellValue != null) {
        System.out.println("nNGM-Fhir-Specimen - Found Tumorzellgehalt: ${tumorCellValue}%")
        extension {
            url = "http://uk-koeln.de/fhir/StructureDefinition/Extension/nNGM/tumorzellgehalt"
            valueDecimal = tumorCellValue
        }
    }

    // Extension: Storage Temperature
    final def temperature = toTemperature(context)
    if (temperature) {
        System.out.println("nNGM-Fhir-Specimen - Mapping Storage Temperature: ${temperature}")
        extension {
            url = "http://uk-koeln.de/fhir/StructureDefinition/Extension/nNGM/specimen-tumormaterial-lagert-bei"
            valueCodeableConcept {
                coding {
                    system = "http://uk-koeln.de/fhir/StructureDefinition/Extension/nNGM/specimen-tumormaterial-lagert-bei"
                    code = temperature
                }
            }
        }
    }

    // Quantity / Container
    final def ucum = context.conceptMaps.builtin("centraxx_ucum")
    container {
        specimenQuantity {
            value = context.source[abstractSample().initialAmount().amount()] as Number
            unit = ucum.translate(context.source[abstractSample().initialAmount().unit()] as String)?.code
            system = "http://unitsofmeasure.org"
        }
    }
}

static String codeToSampleType(final String sampleTypeCode, final String stockType, final String sampleKindCode) {
    if (null == sampleTypeCode) return null
    if (matchIgnoreCase(["SER", "PLA", "BLD", "VBL", "Plasma", "Serum"], sampleTypeCode)) {
        return "C12434"
    }
    return "C12801" // Default to Tissue
}

static String normalizeDate(final String ds) {
    return (ds != null && ds.length() >= 10) ? ds.substring(0, 10) : ds
}

static boolean matchIgnoreCase(final List<String> stringList, final String stringToMatch) {
    return stringToMatch != null && stringList.any { it.equalsIgnoreCase(stringToMatch) }
}

static def toTemperature(final ctx) {
    final def temp = ctx.source[abstractSample().sampleLocation().temperature() as String]
    if (temp != null) {
        if (temp >= 2.0 && temp <= 10.0) return "temperature2to10"
        if (temp <= -18.0 && temp >= -35.0) return "temperature-18to-35"
        if (temp <= -60.0 && temp >= -85.0) return "temperature-60to-85"
    }
    return null
}