package customexport.nngm

import groovy.json.JsonSlurper
import static de.kairos.fhir.centraxx.metamodel.MultilingualEntry.LANG
import static de.kairos.fhir.centraxx.metamodel.MultilingualEntry.VALUE
import static de.kairos.fhir.centraxx.metamodel.RootEntities.organizationUnit

/**
 * Represents a CXX Organization for nNGM
 * Profile: http://uk-koeln.de/fhir/StructureDefinition/Organisation/nNGM
 */
organization {

    final String orgCode = context.source[organizationUnit().code()]
    System.out.println("nNGM-Fhir-Organization - Processing Organization with code: ${orgCode}")

    final String mappedName = OrganizationLookup.mappingList(orgCode)
    final def nmsOrg = OrganizationLookup.matchOrganization(mappedName)

    if (!nmsOrg || !nmsOrg.internalSequenceIdentifier) {
        System.out.println("nNGM-Fhir-Organization - [nNGM-Org-SKIP]: No NMS Identifier found for Org Code: ${orgCode} (Mapped: ${mappedName})")
        return
    }

    System.out.println("nNGM-Fhir-Organization - Mapping Identifier: ${nmsOrg.internalSequenceIdentifier} for ID: " + context.source[organizationUnit().id()])

    id = "Organization/" + context.source[organizationUnit().id()]

    meta {
        source = "urn:centraxx"
        profile "http://uk-koeln.de/fhir/StructureDefinition/Organisation/nNGM"
    }

    identifier {
        system = "http://uk-koeln.de/fhir/sid/nNGM/nms-organization"
        value = nmsOrg.internalSequenceIdentifier
    }

    active = true

    // Get name from Centraxx Multilinguals
    name = context.source[organizationUnit().multilinguals()]?.find { it[LANG] == "de" }?.getAt(VALUE) as String ?: mappedName

}

/**
 * Helper class to handle the JSON lookup efficiently
 */
class OrganizationLookup {
    private static List cachedJson = null

    static def matchOrganization(String displayName) {
        if (cachedJson == null) {
            try {
                def file = new File("config/organizations.json")
                if (file.exists()) {
                    cachedJson = new JsonSlurper().parse(file) as List
                } else {
                    System.out.println("nNGM-Fhir-Organization - [ERROR]: organizations.json not found!")
                    return null
                }
            } catch (Exception e) {
                System.out.println("nNGM-Fhir-Organization - [ERROR]: Failed to parse JSON: " + e.message)
                return null
            }
        }
        System.out.println("nNGM-Fhir-Organization - Searching NMS Registry for: " + displayName)
        return cachedJson.find { it.displayName == displayName }
    }

    /**
     * Maps local Centraxx Org Codes to the displayNames in the nNGM JSON file
     */
    static String mappingList(String organizationCode) {
        switch (organizationCode) {
            case "HÄMA": return "Universitätsklinikum Frankfurt "
            case "KGU":  return "Universitätsklinikum Frankfurt "
            default:     return organizationCode
        }
    }
}