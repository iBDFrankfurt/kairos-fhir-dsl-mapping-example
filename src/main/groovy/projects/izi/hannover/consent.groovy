package projects.izi.hannover

import de.kairos.centraxx.fhir.r4.utils.FhirUrls
import de.kairos.fhir.centraxx.metamodel.IdContainer
import de.kairos.fhir.centraxx.metamodel.IdContainerType

import java.text.SimpleDateFormat

import static de.kairos.fhir.centraxx.metamodel.RootEntities.consent
import static de.kairos.fhir.centraxx.metamodel.RootEntities.diagnosis

/**
 * Represented by a CXX Consent
 * @author Mike Wähnert
 * @since KAIROS-FHIR-DSL.v.1.16.0, CXX.v.2022.2.0
 * HINT: binary file attachments are not needed and not supported yet.
 */
consent {

  final Map<String, String> localToCentralType = [
      //Frankfurt ITMP => Leipzig IZI Central
      "PATIENTCOSENTV2.4"  : "Broad_Consent",
      "PATIENTCONSENT2.5"  : "Broad_Consent",
      "CIMD_CONSENT"       : "CIMD_Consent",
      // Hannover HUB => Leipzig IZI Central
      "ConsentDefaultStudy": "Study_Consent",
      "ConsentCIMD"        : "CIMD_Consent"]

  final String localConsentTypeCode = context.source[consent().consentType().code()]
  final String centralConsentTypeCode = localToCentralType.getOrDefault(localConsentTypeCode, "Broad_Consent");
  if (centralConsentTypeCode == null) {
    return // no export
  }

  id = "Consent/Consent-" + context.source[consent().id()]

  final def patIdContainer = context.source[diagnosis().patientContainer().idContainer()]?.find {
    "SID" == it[IdContainer.ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE)
  }

  if (patIdContainer) {
    patient {
      identifier {
        value = patIdContainer[IdContainer.PSN]
        type {
          coding {
            system = "urn:centraxx"
            code = patIdContainer[IdContainer.ID_CONTAINER_TYPE]?.getAt(IdContainerType.CODE) as String
          }
        }
      }
    }
  }

  final def validFrom = context.source[consent().validFrom().date()]
  final def validUntil = context.source[consent().validUntil().date()]

  provision {
    period {
      start = validFrom
      end = validUntil
    }

    purpose {
      system = FhirUrls.System.Consent.Type.BASE_URL
      code = centralConsentTypeCode
    }
  }

  final boolean isDeclined = context.source[consent().declined()]
  final boolean isCompleteRevoked = context.source[consent().revocation()] != null && !context.source[consent().revocation().revokePartsOnly()]
  final String interpretedStatus = getStatus(isDeclined, isCompleteRevoked, validUntil as String)
  status = interpretedStatus

  final boolean hasFlexiStudy = context.source[consent().consentType().flexiStudy()] != null
  scope {
    coding {
      system = "http://terminology.hl7.org/CodeSystem/consentscope"
      code = hasFlexiStudy ? "research" : "patient-privacy"
    }
  }

  category {
    coding {
      system = "http://loinc.org"
      code = "59284-0" // Patient Consent
    }
  }

  dateTime {
    date = context.source[consent().creationDate()]
  }

  policyRule {
    coding {
      system = "http://terminology.hl7.org/CodeSystem/v3-ActCode"
      code = "OPTINR" // opt-in with restrictions
    }
  }

  if (context.source[consent().signedOn()]) {
    verification {
      verified = true
      verificationDate {
        date = context.source[consent().signedOn().date()]
      }
    }
  }

  extension {
    url = FhirUrls.Extension.Consent.NOTES
    valueString = context.source[consent().notes()]
  }


  if (context.source[consent().revocation()] && context.source[consent().revocation().notes()]) {
    extension {
      url = FhirUrls.Extension.Consent.Revocation.REVOCATION_NOTES
      valueString = context.source[consent().revocation().notes()]
    }
  }
}

static String getStatus(final boolean isDeclined, final boolean isCompleteRevoked, final String validFromDate) {
  if (isDeclined) {
    return "rejected"
  }

  if (isCompleteRevoked) {
    return "inactive"
  }

  if (!validFromDate) {
    return "active"
  }

  final Date fromDate = new SimpleDateFormat("yyyy-MM-dd").parse(validFromDate.substring(0, 10))
  final Date currDate = new Date()
  final int res = currDate <=> (fromDate)
  return res == 1 ? "inactive" : "active"
}