package customexport.nngm


import de.kairos.fhir.centraxx.metamodel.OrganisationUnit
import org.hl7.fhir.r4.model.Consent
import org.hl7.fhir.r4.model.DateTimeType

import static de.kairos.fhir.centraxx.metamodel.RootEntities.consent

/**
 * Represented by a CXX Consent
 * Specified by https://simplifier.net/guide/nNGM-Form/Home/FHIR-Profile/Basisangaben/EinwilligungserklrungConsent.guide.md?version=current
 * @author Timo Schneider
 * @since KAIROS-FHIR-DSL.v.1.8.0, CXX.v.3.18.1
 */

consent {

  final def consentCode = context.source[consent().consentType().code()]
//  if (consentCode != "NNGM NAME") {
//    return //no export
//  }

  id = "Consent/" + context.source[consent().id()]
  meta{
    source = "urn:centraxx"
    profile "http://uk-koeln.de/fhir/StructureDefinition/nNGM/Consent"
  }


  final def validUntil = context.source[consent().validUntil().date()]
  final def validFrom = context.source[consent().validFrom().date()]

  final String interpretedStatus = validUntilInterpreter(validUntil as String)

  status = interpretedStatus

  scope {
    coding {
      system = "http://terminology.hl7.org/CodeSystem/consentscope"
      code = "research"
    }
  }

  category {
    coding {
      system = "http://loinc.org"
      code = "59284-0"
    }
  }

  patient{
    reference = "Patient/" +context.source[consent().patientContainer().id()]
  }

  dateTime = context.source[consent().validFrom()] as DateTimeType


  context.source[consent().patientContainer().organisationUnits()].each { final oe ->
    organization {
      reference = "Organisationseinheit/" + oe[OrganisationUnit.ID]
      display = oe[OrganisationUnit.CODE]
    }
  }

  sourceReference {
    reference = "Patient/" + context.source[consent().id()]
  }

  policyRule {
    coding {
      system = "http://uk-koeln.de/fhir/CodeSystem/nNGM/nngm-consent-policy"
      code = consentCode
    }
  }

  //final def consentParts = []
  //final def consentPartsOnly = context.source[consent().consentPartsOnly()]


//  if (consentPartsOnly == false){
//    consentParts.addAll(["1a"] )
//  }
//  else if (consentPartsOnly == true){
//    consentParts.addAll(context.source[consent().consentElements().consentableAction().code()])
//  }

  // Define a multidimensional array to hold the code and display information
  String[][] consentParts = [
          ["1a", "Teil 1a: Molekularpathologische Diagnostik im nNGM"],
          ["1b", "Teil 1b: Überregionale Beratung und Studiensuche im nNGM"],
          ["2", "Teil 2: Forschung im nNGM"],
          ["TE", "Teilnahmeerklärung"],
          ["DS", "Datenschutzerklärung"],
          ["MD", "Molekularpathologische Diagnostik im nNGM (Behandlungskontext)"],
          ["ST", "Überregionale Beratung und Studiensuche im nNGM (Behandlungskontext)"],
          ["WPI", "Weitergabe pseudonymisierter krankheitsbezogener Daten (MDAT) innerhalb des nNGM und an kooperierende Partner"],
          ["WP", "Weitergabe von MDAT und Resttumorproben innerhalb des nNGM und an kooperierende Partner"],
          ["WD", "Weitergabe pseudonymisierter krankheitsbezogener Daten (MDAT) in ein Drittland"],
          ["WR", "Weitergabe von MDAT und Resttumorproben in ein Drittland"],
          ["WPK", "Weitergabe pseudonymisierter krankheitsbezogener Daten (MDAT) zur kommerziellen Nutzung"],
          ["WK", "Weitergabe von MDAT und Resttumorproben zur kommerziellen Nutzung"],
          ["KW", "Kontaktaufnahme des nNGM-Zentrums zu einem späteren Zeitpunkt zur Gewinnung weiterer Informationen über den Behandlungsverlauf"],
          ["KE", "Kontaktaufnahme des nNGM-Zentrums zum Zweck des Einschlusses in eine mögliche infrage kommende neue Studie"],
          ["RD", "Rückmeldung wichtiger gesundheitsrelevanter Ergebnisse (Zufallsfunde)"]
  ]


  provision {
    for (String[] part : consentParts) {
      final codePart = part[0]
      final displayPart = part[1]

      consentParts.each { final cA ->
        type = Consent.ConsentProvisionType.PERMIT
        period {
          start = validFrom
          end = validUntil
        }
        code {
          coding {
            system = "http://loinc.org"
            code = codePart
            display = displayPart
          }
        }
      }
    }
  }

}

static String validUntilInterpreter(String validFromDate){
  def fromDate = Date.parse("yyyy-MM-dd", validFromDate.substring(0,10))
  if(!validFromDate){
    return "active"
  }
  else{
    def currDate = new Date()
    final def res = currDate <=> (fromDate)
    if (res == 0){
      return "active"
    }
    else if (res == 1){
      return "inactive"
    }
  }
}

static String mapConsent(final String cxxConsentPart){
  switch(cxxConsentPart){
    case ("IDAT_bereitstellen_EU_DSGVO_konform"):
      return "IDAT_bereitstellen_EU_DSGVO_konform"
    case ("IDAT_erheben"):
      return "IDAT_erheben"
    case ("IDAT_speichern_verarbeiten"):
      return "IDAT_speichern_verarbeiten"
    case ("IDAT_zusammenfuehren_Dritte"):
      return "IDAT_zusammenfuehren_Dritte"
    case ("MDAT_erheben"):
      return "MDAT_erheben"
    case ("MDAT_speichern_verarbeiten"):
      return "MDAT_speichern_verarbeiten"
    case ("MDAT_wissenschaftlich_nutzen_EU_DSGVO_konform"):
      return "MDAT_wissenschaftlich_nutzen_EU_DSGVO_konform"
    case ("MDAT_zusammenfuehren_Dritte"):
      return "MDAT_zusammenfuehren_Dritte"
    case ("Rekontaktierung_Verknuepfung_Datenbanken"):
      return "Rekontaktierung_Verknuepfung_Datenbanken"
    case ("Rekontaktierung_weitere_Erhebung"):
      return "Rekontaktierung_weitere_Erhebung"
    case ("Rekontaktierung_weitere_Studie"):
      return "Rekontaktierung_weitere_Studie"
    case ("MDAT_GECCO83_bereitstellen_NUM_CODEX"):
      return "MDAT_GECCO83_bereitstellen_NUM_CODEX"
    case ("MDAT_GECCO83_speichern_verarbeiten_NUM_CODEX"):
      return "MDAT_GECCO83_speichern_verarbeiten_NUM_CODEX"
    case ("MDAT_GECCO83_wissenschaftlich_nutzen_COVID_19_Forschung_EU_DSGVO_konform"):
      return "MDAT_GECCO83_wissenschaftlich_nutzen_COVID_19_Forschung_EU_DSGVO_konform"
    case ("MDAT_GECCO83_wissenschaftlich_nutzen_Pandemie_Forschung_EU_DSGVO_konform"):
      return "MDAT_GECCO83_wissenschaftlich_nutzen_Pandemie_Forschung_EU_DSGVO_konform"
    case ("Rekontaktierung_Zusatzbefund"):
      return "Rekontaktierung_Zusatzbefund"
  }
}


