package customexport.nngm

import de.kairos.fhir.centraxx.metamodel.*
import org.hl7.fhir.r4.model.Observation
import static de.kairos.fhir.centraxx.metamodel.RootEntities.laborMapping

/**
 * Profile: http://uk-koeln.de/fhir/StructureDefinition/Observation/nNGM/raucherstatus
 * Root Entity: LaborMapping
 */
observation {
    // Filter by the finding's short name
    final def finding = context.source[laborMapping().laborFinding()]

    // Convert the finding short name to a string to perform the startsWith check
    final String findingShortName = finding[LaborFinding.SHORT_NAME] as String

    // Check if it starts with the expected prefix
    if (findingShortName == null || !findingShortName.startsWith("GTDSBefund")) {
        System.out.println("nNGM-Fhir-Raucherstatus - Skipping finding with shortName: " + findingShortName)
        return
    }

    System.out.println("nNGM-Fhir-Raucherstatus - Processing Raucherstatus for Finding ID: " + finding[LaborFinding.ID])

    // Identify by the Finding ID to ensure uniqueness
    id = "Observation/Smoking-" + finding[LaborFinding.ID]

    meta {
        source = "urn:centraxx"
        profile "http://uk-koeln.de/fhir/StructureDefinition/Observation/nNGM/raucherstatus"
    }

    status = Observation.ObservationStatus.FINAL

    category {
        coding {
            system = "http://terminology.hl7.org/CodeSystem/observation-category"
            code = "social-history"
        }
    }

    code {
        coding {
            system = "http://loinc.org"
            code = "72166-2"
            display = "Tobacco smoking status"
        }
    }

    // SUBJECT
    final def patient = context.source[laborMapping().relatedPatient()]
    if (patient) {
        System.out.println("nNGM-Fhir-Raucherstatus - Mapping Patient Reference: Patient/" + patient[PatientContainer.ID])
        subject {
            reference = "Patient/" + patient[PatientContainer.ID]
        }
    }

    // FINDING DATE
    final def fDate = context.source[laborMapping().laborFinding().findingDate()]
    if (fDate) {
        effectiveDateTime = normalizeDate(fDate[PrecisionDate.DATE] as String)
    }

    boolean smokingStatusMapped = false

    // ITERATE THROUGH VALUES
    context.source[laborMapping().laborFinding().laborFindingLaborValues()].each { final lflv ->

        // Use crfTemplateField to get the laborValue
        final def laborValue = lflv[LaborFindingLaborValue.CRF_TEMPLATE_FIELD] != null ?
                lflv[LaborFindingLaborValue.CRF_TEMPLATE_FIELD][CrfTemplateField.LABOR_VALUE] :
                lflv[LaborFindingLaborValue.LABOR_VALUE]

        final String lvCode = laborValue?.getAt(AbstractCode.CODE)

        // Raucherstatus
        if (lvCode == "Raucherstatus") {
            final String rawCatalogCode = lflv[LaborFindingLaborValue.CATALOG_ENTRY_VALUE]?.find()?.getAt(CatalogEntry.CODE)
            final def nngmCoding = mapSmokingStatus(rawCatalogCode)
            if (nngmCoding) {
                System.out.println("nNGM-Fhir-Raucherstatus - Mapping Status value: " + nngmCoding.code)
                valueCodeableConcept {
                    coding {
                        system = "http://loinc.org"
                        code = nngmCoding.code
                        display = nngmCoding.display
                    }
                }
                smokingStatusMapped = true
            }
        }

        // MAP COMPONENT: Pack Years
        if (lvCode == "pack years") {
            final def val = lflv[LaborFindingLaborValue.NUMERIC_VALUE]
            if (val != null) {
                System.out.println("nNGM-Fhir-Raucherstatus - Mapping PackYears: " + val)
                component {
                    code { coding { system = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl"; code = "C73993" } }
                    valueQuantity {
                        value = val
                        unit = "{PackYears}"
                        system = "http://unitsofmeasure.org"
                        code = "{PackYears}"
                    }
                }
            }
        }

        // MAP COMPONENT: Nichtraucher seit
        if (lvCode == "Ende Raucher") {
            final def val = lflv[LaborFindingLaborValue.NUMERIC_VALUE]
            if (val != null) {
                System.out.println("nNGM-Fhir-Raucherstatus - Mapping YearsSinceQuitting: " + val)
                component {
                    code { coding { system = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl"; code = "C124908" } }
                    valueQuantity {
                        value = val
                        unit = "a"
                        system = "http://unitsofmeasure.org"
                        code = "a"
                    }
                }
            }
        }
    }

    // Smoking check (dataAbsentReason)
    if (!smokingStatusMapped) {
        System.out.println("nNGM-Fhir-Raucherstatus - WARNING: No Status mapped, setting dataAbsentReason")
        dataAbsentReason {
            coding {
                system = "http://terminology.hl7.org/CodeSystem/data-absent-reason"
                code = "unknown"
            }
        }
    }
}

static Map mapSmokingStatus(String val) {
    switch (val) {
        case "Raucher": return [code: "LA18906-8", display: "Current everyday smoker"]
        case "Exraucher": return [code: "LA15920-4", display: "Former smoker"]
        case "Nichtraucher": return [code: "LA18907-6", display: "Never smoker"]
        case "Unbekannt": return [code: "LA18908-4", display: "Unknown if ever smoked"]
        default: return null
    }
}

static String normalizeDate(final String ds) {
    return (ds != null && ds.length() >= 10) ? ds.substring(0, 10) : ds
}