package customexport.nngm

import de.kairos.fhir.centraxx.metamodel.IcdEntry
import javax.annotation.Nullable

import static de.kairos.fhir.centraxx.metamodel.RootEntities.diagnosis
import static de.kairos.fhir.centraxx.metamodel.RootEntities.tnm

/**
 * Profile: http://uk-koeln.de/fhir/StructureDefinition/Condition/nNGM/FirstDiagnosis
 * Filter: ICD-10 starts with 'C34'
 */
condition {
    final String currentId = context.source[diagnosis().id()] as String
    final String icdCode = context.source[diagnosis().icdEntry().code()]

    System.out.println("[nNGMProfileCondition-DEBUG]: Checking Diag ID: " + currentId + " (ICD: " + icdCode + ")")

    // Filter: C34 Lung Cancer
    if (icdCode == null || !icdCode.startsWith("C34")) {
        return
    }

    // Identity and Profile
    id = "Condition/" + currentId
    meta {
        source = "urn:centraxx"
        profile "http://uk-koeln.de/fhir/StructureDefinition/Condition/nNGM/FirstDiagnosis"
    }

    // FHIR Status
    clinicalStatus {
        coding {
            system = "http://terminology.hl7.org/CodeSystem/condition-clinical"
            code = "active"
        }
    }
    verificationStatus {
        coding {
            system = "http://terminology.hl7.org/CodeSystem/condition-verifstatus"
            code = "confirmed"
        }
    }

    // Subject (Patient)
    subject {
        reference = "Patient/" + context.source[diagnosis().patientContainer().id()]
    }

    // Onset Date
    final def dDate = context.source[diagnosis().diagnosisDate().date()]
    if (dDate) {
        onsetDateTime = normalizeDate(dDate as String)
    } else {
        System.out.println("[nNGMProfileCondition-DEBUG]: WARNING: Missing Mandatory Onset Date!")
        return
    }

    // Condition.code -> Morphologie ICD-O-3
    final IcdEntry icdo = diagnosis().icdOentry()
    final String morphCode = context.source[icdo.code()] ?: context.source[diagnosis().icdEntry().code()]

    code {
        coding {
            // Must match ValueSet: http://uk-koeln.de/fhir/ValueSet/nNGM/histologie-klassifikation
            system = "http://uk-koeln.de/fhir/CodeSystem/nNGM/histologie-klassifikation"
            code = morphCode
            version = context.source[icdo.catalogue().catalogueVersion()] ?: context.source[diagnosis().icdEntry().catalogue().catalogueVersion()]
        }
        text = context.source[icdo.preferredLong()] ?: context.source[diagnosis().icdEntry().preferredLong()]
    }

    // Condition.bodySite -> Topologie ICD-O-3
    final String topoCode = context.source[diagnosis().diagnosisLocalisation()] ?: context.source[diagnosis().icdOcode()]
    if (topoCode) {
        bodySite {
            coding {
                // Must match ValueSet: http://uk-koeln.de/fhir/ValueSet/icd-o-3-topologie
                system = "http://uk-koeln.de/fhir/ValueSet/icd-o-3-topologie"
                code = topoCode
            }
        }
    }

    // Condition.stage -> UICC Staging (Sliced uicc7 / uicc8)
    final String stadium = context.source[tnm().stadium()]
    final String version = context.source[tnm().version()]

    if (stadium) {
        System.out.println("[nNGMProfileCondition-DEBUG]: Found Stage: " + stadium + " (Version: " + version + ")")
        stage {
            summary {
                coding {
                    // Populate correct slice system based on version
                    if (version?.contains("7")) {
                        system = "http://uk-koeln.de/fhir/CodeSystem/nNGM/uiccStagingV7"
                    } else if (version?.contains("8")) {
                        system = "http://uk-koeln.de/fhir/CodeSystem/nNGM/uiccStagingV8"
                    }
                    code = extractCode(stadium)
                }
            }
        }
    }
}

static String normalizeDate(final String ds) {
    return (ds != null && ds.length() >= 10) ? ds.substring(0, 10) : ds
}

@Nullable
static String extractCode(@Nullable final String s) {
    if (s == null || s.trim().isEmpty()) return null
    final String[] parts = s.trim().split("\\s+")
    return parts.length > 0 ? parts[0] : null
}
