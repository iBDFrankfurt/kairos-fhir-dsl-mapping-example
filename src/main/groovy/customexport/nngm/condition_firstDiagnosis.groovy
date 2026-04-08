package customexport.nngm


import static de.kairos.fhir.centraxx.metamodel.RootEntities.diagnosis

condition {
    final String icdCode = context.source[diagnosis().icdEntry().code()]

    // Filter: Lung Cancer only
    if (icdCode == null || !icdCode.toUpperCase().startsWith('C34')) {
        System.out.println("nNGM-Fhir-FirstDiagnosis - Skipping Diagnosis (non-C34 or null): " + icdCode)
        return
    }

    // onsetDateTime
    final def dDate = context.source[diagnosis().diagnosisDate().date()]
    if (!dDate) {
        System.out.println("nNGM-Fhir-FirstDiagnosis - WARNING: Skipping Diagnosis ID: " + context.source[diagnosis().id()] + " - missing mandatory diagnosisDate")
        return // Skip if no date, otherwise invalid FHIR
    }

    System.out.println("nNGM-Fhir-FirstDiagnosis - Mapping Condition for Diagnosis ID: " + context.source[diagnosis().id()])

    id = "Condition/" + context.source[diagnosis().id()]

    meta {
        profile "http://uk-koeln.de/fhir/StructureDefinition/Condition/nNGM/FirstDiagnosis"
    }

    subject {
        reference = "Patient/" + context.source[diagnosis().patientContainer().id()]
    }

    // Identifiers and Recorder (Clinician)
    final def diagnosisId = context.source[diagnosis().diagnosisId()]
    if (diagnosisId) {
        System.out.println("nNGM-Fhir-FirstDiagnosis - Mapping Identifier: " + diagnosisId)
        identifier {
            value = diagnosisId
            system = "urn:centraxx"
        }
    }

    final def clinician = context.source[diagnosis().clinician()]
    if (clinician) {
        System.out.println("nNGM-Fhir-FirstDiagnosis - Mapping Clinician: " + clinician)
        recorder {
            display = clinician as String
        }
    }

    onsetDateTime = normalizeDate(dDate as String)

    // Histologie (ICD-O-3 Morphology)
    final String morphCode = context.source[diagnosis().icdOentry().code()]
    System.out.println("nNGM-Fhir-FirstDiagnosis - Mapping Morphology code: " + morphCode)

    code {
        coding {
            system = "urn:oid:2.16.840.1.113883.6.43.1"
            code = morphCode ?: "NOS"
            version = context.source[diagnosis().icdOentry().catalogue().catalogueVersion()]
        }
        text = context.source[diagnosis().icdOentry().preferredLong()] ?: context.source[diagnosis().icdEntry().preferredLong()]
    }

    // Topography (ICD-O-3 Topology)
    final String topoCode = context.source[diagnosis().icdOcode()] ?: icdCode
    if (topoCode) {
        System.out.println("nNGM-Fhir-FirstDiagnosis - Mapping Topography code: " + topoCode)
        bodySite {
            coding {
                // Must match ValueSet: http://uk-koeln.de/fhir/ValueSet/icd-o-3-topologie
                system = "http://uk-koeln.de/fhir/ValueSet/icd-o-3-topologie"
                code = topoCode
            }
        }
    }
}

static String normalizeDate(final String ds) {
    return (ds != null && ds.length() >= 10) ? ds.substring(0, 10) : ds
}