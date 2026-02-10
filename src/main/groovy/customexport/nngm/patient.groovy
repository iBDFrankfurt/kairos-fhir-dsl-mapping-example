package customexport.nngm

import de.kairos.fhir.centraxx.metamodel.Country
import de.kairos.fhir.centraxx.metamodel.IdContainer
import de.kairos.fhir.centraxx.metamodel.MultilingualEntry
import de.kairos.fhir.centraxx.metamodel.PatientAddress
import de.kairos.fhir.centraxx.metamodel.PrecisionDate
import de.kairos.fhir.centraxx.metamodel.enums.GenderType
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender
import org.hl7.fhir.r4.model.HumanName

import static de.kairos.fhir.centraxx.metamodel.RootEntities.patient

/**
 * Represented by a CXX Patient for nNGM
 * Specified by https://simplifier.net/nngm-form/profilenngmpatientpatient
 */
patient {
    id = "Patient/" + context.source[patient().patientContainer().id()]

    meta {
        source = "urn:centraxx"
        profile "http://uk-koeln.de/fhir/StructureDefinition/Patient/nNGM/patient"
    }

    // Identifiers
    context.source[patient().patientContainer().idContainer()].each { final def idContainer ->
        identifier {
            system = "urn:centraxx"
            value = idContainer[IdContainer.PSN]
        }
    }

    // Name (Official)
    humanName {
        use = HumanName.NameUse.OFFICIAL
        family = context.source[patient().lastName()]
        given(context.source[patient().firstName()] as String)

        final def title = context.source[patient().title().multilinguals()]?.find { it[MultilingualEntry.LANG] == "de" }
        if (title) {
            prefix(title.getAt(MultilingualEntry.VALUE) as String)
            _prefix {
                extension {
                    url = "http://hl7.org/fhir/StructureDefinition/iso21090-EN-qualifier"
                    valueCode = "AC"
                }
            }
        }
    }

    // Geburtsname (Maiden)
    if (context.source[patient().birthName()]) {
        humanName {
            use = HumanName.NameUse.MAIDEN
            family = context.source[patient().birthName()]
        }
    }

    if (context.source[patient().birthdate()] && context.source[patient().birthdate().date()]) {
        birthDate = context.source[patient().birthdate().date()]
    }

    final def dateOfDeath = context.source[patient().dateOfDeath()]
    if (dateOfDeath && "UNKNOWN" != context.source[patient().dateOfDeath().precision()]) {
        deceasedBoolean = true
        deceasedDateTime = dateOfDeath[PrecisionDate.DATE]
    } else {
        deceasedBoolean = false
    }

    // Gender with nNGM constraint pat-nngm-1
    final GenderType genderType = context.source[patient().genderType()] as GenderType
    gender {
        if (genderType == GenderType.MALE) {
            value = AdministrativeGender.MALE
        } else if (genderType == GenderType.FEMALE) {
            value = AdministrativeGender.FEMALE
        } else if (genderType == GenderType.UNKNOWN) {
            value = AdministrativeGender.UNKNOWN
        } else {
            value = AdministrativeGender.OTHER
            extension {
                url = "http://fhir.de/StructureDefinition/gender-amtlich-de"
                valueCode = (genderType == GenderType.X) ? "D" : "X"
            }
        }
    }

    // Address
    context.source[patient().addresses()]?.each { final ad ->
        if (ad[PatientAddress.STREET]) {
            address {
                type = "both"
                city = ad[PatientAddress.CITY] as String
                postalCode = ad[PatientAddress.ZIPCODE] as String
                country = ad[PatientAddress.COUNTRY]?.getAt(Country.ISO2_CODE) as String
                line(getLineString(ad as Map))
            }
        } else if (ad[PatientAddress.PO_BOX]) {
            address {
                type = "postal"
                city = ad[PatientAddress.CITY] as String
                postalCode = ad[PatientAddress.ZIPCODE] as String
                country = ad[PatientAddress.COUNTRY]?.getAt(Country.ISO2_CODE) as String
                line(ad[PatientAddress.PO_BOX] as String)
            }
        }
    }
}

static String getLineString(final Map address) {
    final def keys = [PatientAddress.STREET, PatientAddress.STREETNO]
    final def addressParts = keys.collect { return address[it] }.findAll { it != null }
    return addressParts ? addressParts.join(" ") : null
}
