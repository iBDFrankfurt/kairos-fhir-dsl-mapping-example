package customexport.nngm

import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.r4.model.*

import java.nio.file.Paths

import static de.kairos.fhir.centraxx.metamodel.RootEntities.patientMasterDataAnonymous

/**
 * Aggregates clinical resources into a valid nNGM Document Bundle
 * Compliant with:
 * - Profile-nNGM-Bundle
 * - Profile-nNGM-Composition-Case
 */
patient {

    final String exportDir = "C:/Development/fhir-export/nngm"
    final FhirContext ctx = FhirContext.forR4()
    final String currentPatientId = context.source[patientMasterDataAnonymous().patientContainer().id()]

    System.out.println("nNGM-Fhir-Bundle - Starting aggregation for Patient-ID: ${currentPatientId}")

    // Load files for this specific patient
    final List<File> files = new File(exportDir).listFiles().findAll {
        it.isFile() && it.name.contains(currentPatientId) && !it.name.startsWith("Aggregate")
    }

    if (files.isEmpty()) {
        System.out.println("nNGM-Fhir-Bundle - No resource files found for Patient ${currentPatientId} in ${exportDir}")
        return
    }

    System.out.println("nNGM-Fhir-Bundle - Found ${files.size()} files to process.")

    // Create Bundle
    Bundle docBundle = new Bundle()
    docBundle.setType(Bundle.BundleType.DOCUMENT)
    docBundle.setTimestamp(new Date())

    // Mandatory Bundle Identifier
    Identifier bundleId = docBundle.getIdentifier()
    bundleId.setUse(Identifier.IdentifierUse.OFFICIAL)
    bundleId.setSystem("http://uk-koeln.de/fhir/sid/bundle-identifier")
    bundleId.setValue("BUN-" + UUID.randomUUID().toString())
    bundleId.getType().addCoding()
            .setSystem("http://uk-koeln.de/fhir/CodeSystem/nNGM/identifier-type")
            .setCode("BUN")

    // Create Composition
    Composition composition = createNngmComposition("Patient/" + currentPatientId)

    docBundle.addEntry()
            .setResource(composition)

    // Resources
    files.each { File file ->
        try {
            System.out.println("nNGM-Fhir-Bundle - Reading file: ${file.name}")
            file.withReader("UTF-8") { reader ->
                Bundle sourceBundle = (Bundle) ctx.newJsonParser().parseResource(reader)
                sourceBundle.entry.each { entry ->
                    Resource r = entry.resource
                    if (r instanceof Composition || r instanceof Bundle) return

                    // Add to bundle
                    docBundle.addEntry().setResource(r)

                    // Link in Composition sections
                    attachToNngmComposition(composition, r)
                }
            }
        } catch (Exception e) {
            System.out.println("nNGM-Fhir-Bundle - ERROR: Failed to process ${file.name}: ${e.message}")
        }
    }

    // VALIDATION CHECK
    // nNGM requires CONSENT, FIRST_DIAGNOSIS, COVERAGE, DIAGNOSTICS_REQUEST
//    def mandatorySections = ["CONSENT", "FIRST_DIAGNOSIS", "COVERAGE", "DIAGNOSTICS_REQUEST"]
//    mandatorySections.each { sectionCode ->
//        if (!composition.getSection().any { it.code.getCodingFirstRep().getCode() == sectionCode }) {
//            System.out.println("nNGM-Fhir-Composition - WARNING: Mandatory Section [${sectionCode}] is missing entries for Patient [${currentPatientId}]")
//        }
//    }

    // WRITE AGGREGATE
    File out = Paths.get(exportDir, "Aggregate_nNGM_" + currentPatientId + ".json").toFile()
    System.out.println("nNGM-Fhir-Bundle - Writing aggregated document to: ${out.absolutePath}")
    out.withWriter("UTF-8") { writer ->
        ctx.newJsonParser().setPrettyPrint(true).encodeResourceToWriter(docBundle, writer)
    }
}

static Composition createNngmComposition(String patientRef) {
    System.out.println("nNGM-Fhir-Composition - Initializing Case Composition for subject: ${patientRef}")
    Composition c = new Composition()
    c.id = UUID.randomUUID().toString()
    c.meta.addProfile("http://uk-koeln.de/fhir/StructureDefinition/nNGM/Composition")

    c.setStatus(Composition.CompositionStatus.FINAL)
    c.setDate(new Date())
    c.setTitle("nNGM Fallbegleitdokumentation")

    // Identifier
    c.getIdentifier()
            .setSystem("http://uk-koeln.de/fhir/sid/nngm-case-no")
            .setValue("CASE-" + UUID.randomUUID().toString())

    // Type (Fixed LOINC)
    c.getType().addCoding()
            .setSystem("http://loinc.org")
            .setCode("11503-0")
            .setDisplay("Medical records")

    c.setSubject(new Reference(patientRef))

    // Author: MUST be an Organization in this profile
    c.setAuthor([new Reference("Organization/SZ-00018")])

    return c
}

static void attachToNngmComposition(Composition c, Resource r) {
    String profile = r.meta.profile.collect { it.value }.join(",")

    // Mapping based on nNGM Profile Slices
    if (r instanceof Consent) {
        addSection(c, "CONSENT", "Einwilligung", r)
    } else if (r instanceof Condition && profile.contains("FirstDiagnosis")) {
        addSection(c, "FIRST_DIAGNOSIS", "Erstdiagnose", r)
    } else if (r instanceof Coverage) {
        addSection(c, "COVERAGE", "Versicherung", r)
    } else if (r instanceof ServiceRequest) {
        addSection(c, "DIAGNOSTICS_REQUEST", "Anforderung", r)
    } else if (r instanceof Observation) {
        if (profile.contains("ecog")) addSection(c, "ECOGS", "ECOG", r)
        else if (profile.contains("raucherstatus")) addSection(c, "TOBACCO_SMOKING", "Raucherstatus", r)
        else if (profile.contains("tumorstadium")) addSection(c, "TUMOR_STAGES", "Tumorstadium", r)
        else if (profile.contains("histologie")) addSection(c, "TUMOR_HISTOLOGIES", "Histologie", r)
        else if (profile.contains("Vitalstatus")) addSection(c, "VITAL_STATES", "Vitalstatus", r)
    }
}

static void addSection(Composition c, String code, String title, Resource r) {
    def section = c.getSection().find { it.getCode().getCodingFirstRep().getCode() == code }
    if (!section) {
        section = c.addSection()
        section.setTitle(title)
        section.getCode().addCoding()
                .setSystem("http://uk-koeln.de/fhir/CodeSystem/nNGM/CompositionSectionType")
                .setCode(code)
    }
    System.out.println("nNGM-Fhir-Composition - Mapping [${r.resourceType.name()}/${r.idElement.idPart}] to section: ${code}")
    section.addEntry(new Reference(r.resourceType.name() + "/" + r.idElement.idPart))
}