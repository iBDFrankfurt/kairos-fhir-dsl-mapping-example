import static de.kairos.fhir.centraxx.metamodel.AbstractEntity.ID
import static de.kairos.fhir.centraxx.metamodel.RootEntities.diagnosis

/**
 * Represented by a CXX Diagnosis
 * Specified by https://simplifier.net/guide/nNGM-Form/Home/FHIR-Profile/Basisangaben/ErstdiagnoseCondition.guide.md?version=current
 * @author Timo Schneider
 * @since CXX.v.3.17.1.6, v.3.17.2
 */
condition {

    final String icdCode = context.source[diagnosis().icdEntry().code()]
    if (!hasRelevantCode(icdCode)) { // diagnosis without C or D code are filtered
        return
    }

    id = "Condition/" + context.source[diagnosis().id()]

    meta {
        profile "http://uk-koeln.de/fhir/StructureDefinition/Condition/nNGM/FirstDiagnosis"
    }

    subject {
        reference = "Patient/" + context.source[diagnosis().patientContainer().id()]
    }

    final def diagnosisId = context.source[diagnosis().diagnosisId()]
    if (diagnosisId) {
        identifier {
            value = diagnosisId
            type {
                coding {
                    system = "urn:centraxx"
                    code = "diagnosisId"
                }
            }
        }
    }

    final def clinician = context.source[diagnosis().clinician()]
    if (clinician) {
        recorder {
            identifier {
                display = clinician
            }
        }
    }

    onsetDateTime {
        date = normalizeDate(context.source[diagnosis().diagnosisDate().date()] as String)
    }

    final String multipleCodingSymbol = mapUsage(context.source[diagnosis().icdEntry().usage()] as String)

    // Histologie
    code {
        coding {
            if (multipleCodingSymbol != null) {
                extension {
                    url = "http://fhir.de/StructureDefinition/icd-10-gm-mehrfachcodierungs-kennzeichen"
                    valueCoding {
                        system = "http://fhir.de/CodeSystem/icd-10-gm-mehrfachcodierungs-kennzeichen"
                        version = "2021"
                        code = multipleCodingSymbol
                    }
                }
            }
            system = "urn:oid:2.16.840.1.113883.6.43.1"
            code = icdCode as String
            version = context.source[diagnosis().icdEntry().catalogue().catalogueVersion()]
        }
        text = context.source[diagnosis().icdEntry().preferredLong()] as String
    }

    // ICD-O-3 topography
    final String catalogName = context.source[diagnosis().icdEntry().catalogue().name()]
    if (catalogName != null && catalogName.contains("ICD-O-3")) {
        bodySite {
            coding {
                system = "http://terminology.hl7.org/CodeSystem/icd-o-3"
                code = icdCode as String
                version = context.source[diagnosis().icdEntry().catalogue().catalogueVersion()]
            }
            text = context.source[diagnosis().icdEntry().preferredLong()] as String
        }
    }

    if (context.source[diagnosis().diagnosisLocalisation()] != null) {
        bodySite {
            coding {
                system = "http://dktk.dkfz.de/fhir/onco/core/CodeSystem/SeitenlokalisationCS"
                code = context.source[diagnosis().diagnosisLocalisation()] as String
            }
        }
    }

    context.source[diagnosis().samples()]?.each { final sample ->
        extension {
            url = "http://dktk.dkfz.de/fhir/StructureDefinition/onco-core-Extension-Specimen"
            valueReference {
                reference = "Specimen/" + sample[ID]
            }
        }
    }
}

static String mapUsage(final String usage) {
    switch (usage) {
        case "optional":
            return "!"
        case "aster":
            return "*"
        case "dagger":
            return "†"
        default:
            return null
    }
}

// usage with enum with 1.16.0 kairos-fhir-dsl
/*static String mapUsage(final IcdEntryUsage usage){
  switch (usage){
    case IcdEntryUsage.OPTIONAL :
      return "!"
    case IcdEntryUsage.ASTER:
      return "*"
    case IcdEntryUsage.DAGGER:
      return "†"
    default:
      return null
  }
}*/

/**
 * removes milli seconds and time zone.
 * @param dateTimeString the date time string
 * @return the result might be something like "1989-01-15T00:00:00"
 */
static String normalizeDate(final String dateTimeString) {
    return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}

static boolean hasRelevantCode(final String icdCode) {
    return icdCode != null && (icdCode.toUpperCase().startsWith('C34'))
}
