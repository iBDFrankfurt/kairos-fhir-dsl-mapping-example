package customexport.nngm

import org.hl7.fhir.r4.model.Observation
import java.time.LocalDate
import java.time.Period

import static de.kairos.fhir.centraxx.metamodel.PrecisionDate.DATE
import static de.kairos.fhir.centraxx.metamodel.RootEntities.patientMasterDataAnonymous

/**
 * Profile: http://uk-koeln.de/fhir/StructureDefinition/nNGM/Vitalstatus
 * Root Entity: PatientMasterDataAnonymous (or Patient)
 */
observation {
  final def patient = context.source[patientMasterDataAnonymous().patientContainer()]
  if (!patient) {
    System.out.println("nNGM-Fhir-Vitalstatus - ERROR: No patient context found.")
    return
  }

  System.out.println("nNGM-Fhir-Vitalstatus - Mapping Vitalstatus for Patient ID: " + patient[de.kairos.fhir.centraxx.metamodel.PatientContainer.ID])

  id = "Observation/Vitalstatus-" + context.source[patientMasterDataAnonymous().patientContainer().id()]

  meta {
    source = "urn:centraxx"
    profile "http://uk-koeln.de/fhir/StructureDefinition/nNGM/Vitalstatus"
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
      code = "67162-8"
      display = "Vital status"
    }
  }

  subject {
    reference = "Patient/" + context.source[patientMasterDataAnonymous().patientContainer().id()]
  }

  // effectiveDateTime = Date of Last Contact (Last known alive)
  // If no specific contact date is found, use the master data creation date
  final def lastContactDate = context.source[patientMasterDataAnonymous().creationDate()]
  if (lastContactDate) {
    effectiveDateTime = normalizeDate(lastContactDate as String)
  } else {
    System.out.println("nNGM-Fhir-Vitalstatus - ERROR: Missing mandatory lastContactDate (effectiveDateTime)")
    return
  }

  // Determine Vital Status
  final def deathDateObj = context.source[patientMasterDataAnonymous().dateOfDeath()]
  final def birthDateObj = context.source[patientMasterDataAnonymous().birthdate()]
  final String[] vitalState = mapVitalStatus(birthDateObj, deathDateObj)

  System.out.println("nNGM-Fhir-Vitalstatus - Mapped Vital Status code: " + vitalState[0])

  valueCodeableConcept {
    coding {
      system = "http://uk-koeln.de/fhir/CodeSystem/nNGM/Vitalstatus"
      code = vitalState[0]
      display = vitalState[1]
    }

    // Extension: Todesdatum (if deceased)
    if (vitalState[0] == "T" && deathDateObj != null) {
      extension {
        url = "http://uk-koeln.de/fhir/StructureDefinition/Extension/nNGM/todesdatum"
        valueDateTime = normalizeDate(deathDateObj[DATE] as String)
      }
    }

    // Extension: Datum Letzter Kontakt
    if (lastContactDate) {
      extension {
        url = "http://uk-koeln.de/fhir/StructureDefinition/Extension/nNGM/datumLetzterKontakt"
        valueDateTime = normalizeDate(lastContactDate as String)
      }
    }
  }
}

/**
 * Logic to determine nNGM Vital Status codes: L (Lebt), T (Verstorben), A (Lost to follow-up)
 */
static String[] mapVitalStatus(final Object dateOfBirth, final Object dateOfDeath) {
  if (dateOfDeath != null && dateOfDeath[DATE] != null) {
    return ["T", "verstorben"]
  }

  if (dateOfBirth == null || dateOfBirth[DATE] == null) {
    return ["A", "lost to follow-up"]
  }

  final String dateString = dateOfBirth[DATE]
  final LocalDate birthDate = LocalDate.parse(dateString.substring(0, 10))

  if (isOlderThanMaxAge(birthDate)) {
    return ["T", "verstorben"]
  }

  return ["L", "lebt"]
}

static boolean isOlderThanMaxAge(final LocalDate dob) {
  return Period.between(dob, LocalDate.now()).getYears() > 120
}

static String normalizeDate(final String ds) {
  return (ds != null && ds.length() >= 10) ? ds.substring(0, 10) : ds
}