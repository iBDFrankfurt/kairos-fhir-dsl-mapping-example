package customexport.nngm

import de.kairos.fhir.centraxx.metamodel.IcdEntry
import org.hl7.fhir.r4.model.Observation

import static de.kairos.fhir.centraxx.metamodel.RootEntities.histology

/**
 * Profile: http://uk-koeln.de/fhir/StructureDefinition/Observation/nNGM/histologie
 */
observation {

  final def histDate = context.source[histology().date()]
  final def icdEntry = context.source[histology().icdEntry()]

  if (!histDate || !icdEntry) {
    System.out.println("nNGM-Fhir-Histologie - [nNGM-Histology-SKIP]: Missing mandatory date or ICD-O Morphology for ID: " + context.source[histology().id()])
    return
  }

  System.out.println("nNGM-Fhir-Histologie - Mapping Histology for ID: " + context.source[histology().id()])

  id = "Observation/Histology-" + context.source[histology().id()]

  meta {
    profile "http://uk-koeln.de/fhir/StructureDefinition/Observation/nNGM/histologie"
  }

  status = Observation.ObservationStatus.FINAL

  category {
    coding {
      system = "http://terminology.hl7.org/CodeSystem/observation-category"
      code = "laboratory"
    }
  }

  code {
    coding {
      system = "http://loinc.org"
      code = "59847-4"
      display = "Morphology [Identifier] in Cancer specimen"
    }
  }

  subject {
    reference = "Patient/" + context.source[histology().patientContainer().id()]
  }

  effectiveDateTime = normalizeDate(histDate as String)

  // Observation.valueCodeableConcept -> Morphologie
  final String mCode = icdEntry[IcdEntry.CODE] as String
  System.out.println("nNGM-Fhir-Histologie - Mapping Morphology value: " + mCode)

  valueCodeableConcept {
    coding {
      system = "urn:oid:2.16.840.1.113883.6.43.1" // ICD-O-3 Morphology OID
      code = mCode
      version = icdEntry[IcdEntry.CATALOGUE]?.getAt(de.kairos.fhir.centraxx.metamodel.Catalogue.CATALOGUE_VERSION)
    }
    text = icdEntry[IcdEntry.PREFERRED_LONG] as String
  }

  // Grading (LOINC 59542-1)
  if (context.source[histology().gradingDict()]) {
    final String gCode = context.source[histology().gradingDict().code()] as String
    System.out.println("nNGM-Fhir-Histologie - Mapping Grading component: " + gCode)
    component {
      code {
        coding {
          system = "http://loinc.org"
          code = "59542-1"
          display = "Histologic grade in Cancer specimen"
        }
      }
      valueCodeableConcept {
        coding {
          system = "http://uk-koeln.de/fhir/CodeSystem/nNGM/grading"
          code = gCode
        }
      }
    }
  }

  // Reference to Tumor/Condition
  if (context.source[histology().tumour()] && hasRelevantCode(context.source[histology().tumour().centraxxDiagnosis().diagnosisCode()] as String)) {
    final String condId = context.source[histology().tumour().centraxxDiagnosis().id()]
    System.out.println("nNGM-Fhir-Histologie - Mapping Focus reference to Condition: " + condId)
    focus {
      reference = "Condition/" + condId
    }
  }
}

static String normalizeDate(final String ds) {
  return (ds != null && ds.length() >= 10) ? ds.substring(0, 10) : ds
}

static boolean hasRelevantCode(final String icdCode) {
  return icdCode != null && (icdCode.toUpperCase().startsWith('C34'))
}