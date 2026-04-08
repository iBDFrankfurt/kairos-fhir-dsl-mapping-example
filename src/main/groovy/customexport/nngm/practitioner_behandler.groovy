package customexport.nngm

import groovy.json.JsonSlurper
import static de.kairos.fhir.centraxx.metamodel.RootEntities.attendingDoctor

/**
 * Profile: http://uk-koeln.de/fhir/StructureDefinition/Practitioner/nNGM
 */
practitioner {

    final String firstName = context.source[attendingDoctor().contact().contactPersonFirstName()] ?: ""
    final String lastName = context.source[attendingDoctor().contact().contactPersonLastName()] ?: ""
    System.out.println("nNGM-Fhir-Practitioner - Processing Doctor: ${firstName} ${lastName}")

    final def nmsPractitioner = PractitionerLookup.matchPractitioner(firstName, lastName)

    if (!nmsPractitioner || !nmsPractitioner.internalSequenceIdentifier) {
        System.out.println("nNGM-Fhir-Practitioner - [nNGM-Practitioner-SKIP]: No NMS ID found for ${firstName} ${lastName}")
        return
    }

    System.out.println("nNGM-Fhir-Practitioner - Mapping Identifier: ${nmsPractitioner.internalSequenceIdentifier}")

    id = "Practitioner/" + context.source[attendingDoctor().id()]

    meta {
        source = "urn:centraxx"
        profile "http://uk-koeln.de/fhir/StructureDefinition/Practitioner/nNGM"
    }

    identifier {
        system = "http://uk-koeln.de/fhir/sid/nNGM/nms-person"
        value = nmsPractitioner.internalSequenceIdentifier
    }

    name {
        family = nmsPractitioner.lastName
        given(nmsPractitioner.firstName as String)
        if (nmsPractitioner.title) {
            prefix(nmsPractitioner.title as String)
        }
    }
}

/**
 * Efficient Lookup Class
 */
class PractitionerLookup {
    private static List cachedPractitioners = null

    static def matchPractitioner(String firstName, String lastName) {
        if (cachedPractitioners == null) {
            try {
                def file = new File("config/practitioners.json")
                if (file.exists()) {
                    cachedPractitioners = new JsonSlurper().parse(file) as List
                } else {
                    System.out.println("nNGM-Fhir-Practitioner - [ERROR]: practitioners.json not found!")
                    return null
                }
            } catch (Exception e) {
                System.out.println("nNGM-Fhir-Practitioner - [ERROR]: JSON Parsing failed: " + e.message)
                return null
            }
        }

        System.out.println("nNGM-Fhir-Practitioner - Searching NMS Registry for: ${firstName} ${lastName}")
        return cachedPractitioners.find {
            it.firstName?.equalsIgnoreCase(firstName) && it.lastName?.equalsIgnoreCase(lastName)
        }
    }
}