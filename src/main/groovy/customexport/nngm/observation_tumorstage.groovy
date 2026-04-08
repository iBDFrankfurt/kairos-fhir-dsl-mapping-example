package customexport.nngm


import org.hl7.fhir.r4.model.Observation

import static de.kairos.fhir.centraxx.metamodel.RootEntities.tnm

/**
 * Profile: http://uk-koeln.de/fhir/StructureDefinition/Observation/nNGM/tumorstadium
 * Root Entity: TNM
 */
observation {

    final def tnmDate = context.source[tnm().date()]
    final def uiccStadium = context.source[tnm().stadium()]

    if (!tnmDate || !uiccStadium) {
        System.out.println("nNGM-Fhir-Tumorstadium - [nNGM-Observation-SKIP]: Missing Date or UICC Stadium for TNM ID: " + context.source[tnm().id()])
        return
    }

    System.out.println("nNGM-Fhir-Tumorstadium - Mapping Tumorstadium for TNM ID: " + context.source[tnm().id()])

    id = "Observation/Tnm-" + context.source[tnm().id()]

    meta {
        source = "urn:centraxx"
        profile "http://uk-koeln.de/fhir/StructureDefinition/Observation/nNGM/tumorstadium"
    }

    status = Observation.ObservationStatus.FINAL

    // survey
    category {
        coding {
            system = "http://terminology.hl7.org/CodeSystem/observation-category"
            code = "survey"
        }
    }

    // SNOMED: UICC tumor staging system
    code {
        coding {
            system = "http://snomed.info/sct"
            code = "260879005"
            display = "UICC tumor staging system"
        }
    }

    subject {
        reference = "Patient/" + context.source[tnm().patientContainer().id()]
    }

    effectiveDateTime = normalizeDate(tnmDate as String)

    // UICC Version Extension
    if (context.source[tnm().version()]) {
        extension {
            url = "http://uk-koeln.de/fhir/StructureDefinition/Extension/nNGM/uiccVersion"
            valueString = context.source[tnm().version()] as String
        }
    }

    // UICC-Stadium
    System.out.println("nNGM-Fhir-Tumorstadium - Mapping UICC Stadium: " + uiccStadium)
    valueCodeableConcept {
        coding {
            system = "http://uk-koeln.de/fhir/ValueSet/nNGM/uicc-stage"
            code = uiccStadium as String
        }
    }

    // T (Tumor)
    if (context.source[tnm().t()]) {
        System.out.println("nNGM-Fhir-Tumorstadium - Mapping T component: " + context.source[tnm().t()])
        component {
            // T-Prefix Extension
            if (context.source[tnm().praefixTDict()]) {
                extension {
                    url = "http://uk-koeln.de/fhir/StructureDefinition/Extension/nNGM/tnm-cpu-praefix"
                    valueString = context.source[tnm().praefixTDict().code()]
                }
            }
            code {
                coding {
                    system = "http://snomed.info/sct"
                    code = "78873005"
                }
            }
            valueCodeableConcept {
                coding {
                    system = "http://uk-koeln.de/fhir/ValueSet/nNGM/tnm-t"
                    code = context.source[tnm().t()] as String
                }
            }
        }
    }

    // N (Lymph Nodes)
    if (context.source[tnm().n()]) {
        System.out.println("nNGM-Fhir-Tumorstadium - Mapping N component: " + context.source[tnm().n()])
        component {
            // N-Prefix Extension
            if (context.source[tnm().praefixNDict()]) {
                extension {
                    url = "http://uk-koeln.de/fhir/StructureDefinition/Extension/nNGM/tnm-cpu-praefix"
                    valueString = context.source[tnm().praefixNDict().code()]
                }
            }
            code {
                coding {
                    system = "http://snomed.info/sct"
                    code = "277206009"
                }
            }
            valueCodeableConcept {
                coding {
                    system = "http://uk-koeln.de/fhir/ValueSet/nNGM/tnm-n"
                    code = context.source[tnm().n()] as String
                }
            }
        }
    }

    // M (Metastasis)
    if (context.source[tnm().m()]) {
        System.out.println("nNGM-Fhir-Tumorstadium - Mapping M component: " + context.source[tnm().m()])
        component {
            // M-Prefix Extension
            if (context.source[tnm().praefixMDict()]) {
                extension {
                    url = "http://uk-koeln.de/fhir/StructureDefinition/Extension/nNGM/tnm-cpu-praefix"
                    valueString = context.source[tnm().praefixMDict().code()]
                }
            }
            code {
                coding {
                    system = "http://snomed.info/sct"
                    code = "277208005"
                }
            }
            valueCodeableConcept {
                coding {
                    system = "http://uk-koeln.de/fhir/ValueSet/nNGM/tnm-m"
                    code = context.source[tnm().m()] as String
                }
            }
        }
    }

    // TNM-prefix (y-Symbol or r-Symbol)
    final String ySym = context.source[tnm().ySymbol()]
    final String rSym = context.source[tnm().recidivClassification()]
    if (ySym || rSym) {
        System.out.println("nNGM-Fhir-Tumorstadium - Mapping r/y Prefix component: " + (ySym ?: rSym))
        component {
            code {
                coding {
                    system = "http://snomed.info/sct"
                    code = "399566009"
                }
            }
            valueCodeableConcept {
                coding {
                    system = "http://uk-koeln.de/fhir/CodeSystem/nNGM/tnm-ry-praefix"
                    code = ySym ?: rSym
                }
            }
        }
    }

    // Focus link back to the Condition (Diagnosis)
    if (context.source[tnm().tumour()?.centraxxDiagnosis()]) {
        focus {
            reference = "Condition/" + context.source[tnm().tumour().centraxxDiagnosis().id()]
        }
    }
}

static String normalizeDate(final String ds) {
    return (ds != null && ds.length() >= 10) ? ds.substring(0, 10) : ds
}