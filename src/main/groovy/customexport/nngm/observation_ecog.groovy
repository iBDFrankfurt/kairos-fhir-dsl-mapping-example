package customexport.nngm

import de.kairos.fhir.centraxx.metamodel.*
import de.kairos.fhir.centraxx.metamodel.enums.LaborValueDType
import org.hl7.fhir.r4.model.Observation

import static de.kairos.fhir.centraxx.metamodel.RootEntities.laborFindingLaborValue

/**
 * Profile: http://uk-koeln.de/fhir/StructureDefinition/Observation/nNGM/ecog
 * Root Entity: LaborFindingLaborValue
 */
observation {

    final def lvCode = context.source[laborFindingLaborValue().laborValueInteger().code()]
    if (lvCode != "ECOG") {
        System.out.println("nNGM-Fhir-Ecog - Skipping LaborValue: " + lvCode)
        return
    }

    final def creationDate = context.source[laborFindingLaborValue().creationDate()]
    if (!creationDate) {
        System.out.println("nNGM-Fhir-Ecog - [nNGM-ECOG-SKIP]: Missing mandatory creationDate for ID: " + context.source[laborFindingLaborValue().id()])
        return
    }

    System.out.println("nNGM-Fhir-Ecog - Mapping Observation for ECOG ID: " + context.source[laborFindingLaborValue().id()])

    id = "Observation/Ecog-" + context.source[laborFindingLaborValue().id()]

    meta {
        source = "urn:centraxx"
        profile "http://uk-koeln.de/fhir/StructureDefinition/Observation/nNGM/ecog"
    }

    basedOn {
        reference = "ServiceRequest/Anforderung-" + context.source[laborFindingLaborValue().laborFinding().id()]
    }

    status = Observation.ObservationStatus.FINAL

    category {
        coding {
            system = "http://terminology.hl7.org/CodeSystem/observation-category"
            code = "survey"
        }
    }

    code {
        coding {
            system = "http://loinc.org"
            code = "89247-1"
            display = "ECOG Performance Status score"
        }
    }

    // Subject mapping
    final def laborMappingWithPatient = context.source[laborFindingLaborValue().laborFinding().laborMappings()].find {
        it[LaborMapping.RELATED_PATIENT] != null
    }
    if (laborMappingWithPatient) {
        System.out.println("nNGM-Fhir-Ecog - Mapping Subject reference for Patient ID: " + laborMappingWithPatient[LaborMapping.RELATED_PATIENT][PatientContainer.ID])
        subject {
            reference = "Patient/" + laborMappingWithPatient[LaborMapping.RELATED_PATIENT][PatientContainer.ID]
        }
    }

    effectiveDateTime = normalizeDate(creationDate as String)

    // Map 0-5 to LA-codes
    final def dType = context.source[laborFindingLaborValue().laborValue().dType()] as LaborValueDType

    // Logic to extract the raw value (0, 1, 2, 3, 4, or 5)
    String rawValue = ""
    if (dType == LaborValueDType.CATALOG) {
        rawValue = context.source[laborFindingLaborValue().catalogEntryValue()]?.find()?.getAt(CatalogEntry.CODE)
    } else if (dType == LaborValueDType.INTEGER || dType == LaborValueDType.DECIMAL) {
        rawValue = context.source[laborFindingLaborValue().numericValue()].toString()
    }

    System.out.println("nNGM-Fhir-Ecog - Processing raw value: " + rawValue)

    // Perform the mapping to nNGM LOINC Answer Codes
    final def nngmCoding = getEcogCoding(rawValue)

    if (nngmCoding) {
        System.out.println("nNGM-Fhir-Ecog - Mapped to LOINC Answer Code: " + nngmCoding.code)
        valueCodeableConcept {
            coding {
                system = "http://loinc.org"
                code = nngmCoding.code
                display = nngmCoding.display
            }
        }
    } else {
        System.out.println("nNGM-Fhir-Ecog - WARNING: No mapping found for value: " + rawValue + ". Setting dataAbsentReason.")
        dataAbsentReason {
            coding {
                system = "http://terminology.hl7.org/CodeSystem/data-absent-reason"
                code = "unknown"
            }
        }
    }
}

/**
 * Maps simple ECOG digits to nNGM specific LOINC Answer codes
 */
static Map getEcogCoding(String value) {
    switch (value) {
        case "0": return [code: "LA9622-7", display: "ECOG 0 - normale, uneingeschränkte Aktivität"]
        case "1": return [code: "LA9623-5", display: "ECOG 1 - leichte körperliche Arbeit möglich"]
        case "2": return [code: "LA9624-3", display: "ECOG 2 - mehr als 50% der Wachzeit aufstehen"]
        case "3": return [code: "LA9625-0", display: "ECOG 3 - 50% oder mehr der Wachzeit an Bett/Stuhl gebunden"]
        case "4": return [code: "LA9626-8", display: "ECOG 4 - völlig pflegebedürftig"]
        case "5": return [code: "LA9627-6", display: "ECOG 5 - tot"]
        default: return null
    }
}

static String normalizeDate(final String ds) {
    return (ds != null && ds.length() >= 10) ? ds.substring(0, 10) : ds
}