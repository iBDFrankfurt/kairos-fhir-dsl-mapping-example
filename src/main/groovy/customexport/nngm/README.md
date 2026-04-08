![Kairos Logo](https://www.kairos.de/app/uploads/kairos-logo-blue_iqvia.png "Kairos Logo")

nNGM Mappings
======================

Dieses Repository enthält die CentraXX FHIR DSL Mappings für das nNGM-Projekt (nationales Netzwerk Genomische Medizin). Die Skripte transformieren CentraXX-Daten in nNGM-konforme FHIR-Ressourcen.

* **Profil-Definitionen:** [Simplifier nNGM Guide](https://simplifier.net/guide/nNGM-Form/Home?version=current)
* **Ziel-Version:** FHIR R4 (4.0.1)

---

## Manuelle Konfiguration (NMS-Abgleich)

Die nNGM-Profile für **Organization** und **Practitioner** erfordern zwingend Identifikatoren aus dem zentralen nNGM Netzwerk-Management-System (NMS). Da diese IDs nicht standardmäßig in CentraXX vorhanden sind, müssen lokale Organisationen und Behandler über JSON-Dateien im Verzeichnis `config/` (relativ zum Gateway-Ausführungspfad) gemappt werden.

### 1. Organisationen (`organizations.json`)

Das Skript gleicht lokale CentraXX-Organisations-Codes mit dem Feld `displayName` in der JSON-Datei ab.

* **Quelle:** Daten müssen manuell vom NMS-Endpunkt bezogen werden:
  `GET https://nngm-nms.medicalsyn.com/api/v1.0/Public/Organization`
* **Dateipfad:** `config/organizations.json`
* **Präfix-Logik der `internalSequenceIdentifier`:**
    * `SZ` - Netzwerkzentrum
    * `SK` - Krankenhaus
    * `SP` - Praxis
    * `SPa` - Pathologie

**Schema:**
```json
[
    {
        "internalSequenceIdentifier": "string",
        "displayName": "string",
        "isActive": "boolean",
        "isDigiNet": "boolean",
        "networkPartnerIdentifier": "string"
    }
]
```

### 2. Behandler / Personen (`practitioners.json`)

Behandler werden über Vor- und Nachnamen (case-insensitive) gemappt.

* **Quelle:** Daten vom NMS-Endpunkt:
  `GET https://nngm-nms.medicalsyn.com/api/v1.0/Public/Person`
* **Dateipfad:** `config/practitioners.json`
* **Wichtig:** Achten Sie darauf, dass die Person im Feld `organizationAssignments` die `networkPartnerIdentifier` der Organisation enthält, für die sie tätig ist.

**Schema:**
```json
[
    {
        "internalSequenceIdentifier": "string",
        "title": "string|null",
        "firstName": "string",
        "lastName": "string",
        "organizationAssignments": [ "string" ]
    }
]
```

---

## Aggregation und Fall-Dokumentation

Nach dem Export der Einzelressourcen (Patient, Diagnosis, Observations, Specimen) müssen diese über das Aggregations-Skript in einem **Document Bundle** zusammengefasst werden.

* **Composition-Profil:** `http://uk-koeln.de/fhir/StructureDefinition/nNGM/Composition`
* **Bundle-Profil:** `http://uk-koeln.de/fhir/StructureDefinition/Bundle/nNGM`

Das Aggregations-Skript erzeugt pro Patient ein valides nNGM-Dokument, das als "Fall" an das nNGM-Zentrum übermittelt werden kann.

---

## Debugging

Alle Skripte enthalten erweiterte Log-Ausgaben für das CentraXX-Gateway. Suchen Sie im Log nach folgendem Präfix:

`nNGM-Fhir-{Profil-Name} - `

Beispiele:
* `nNGM-Fhir-Patient - Mapping Name: Max Mustermann`
* `nNGM-Fhir-Organization - [nNGM-Org-SKIP]: No NMS Identifier found for Org Code: HÄMA`

---

# Changelog
* **1.0.0**: Initiale Erstellung der nNGM Profile (Patient, Condition, Observation, Specimen, Organization, Practitioner).
* **1.1.0**: Implementierung der NMS-Lookup-Logik für Organisationen und Behandler via JSON-Config.
* **1.2.0**: Hinzufügen des Aggregations-Skripts zur Erstellung von nNGM Document Bundles.