package customexport.nngm

import groovy.json.JsonSlurper
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap

import static de.kairos.fhir.centraxx.metamodel.RootEntities.attendingDoctor

/**
 * Represents a CXX AttendingDoctor
 * Specified by https://simplifier.net/guide/nNGM-Form/Home/FHIR-Profile/Basisangaben/BehanderPractioner.guide.md?version=current
 *
 * Hints:
 * Get Practitioners via Curl from https://simplifier.net/guide/nNGM-Form/Home/NMS/NMS---Endpunkt-f%C3%BCr-Personen.page.md?version=current
 *
 * @author Timo Schneider
 * @since v.1.15.0, CXX.v.2022.1.0
 */
practitioner {

  id = "Practitioner/" + context.source[attendingDoctor().id()]

  meta {
    profile"http://uk-koeln.de/fhir/StructureDefinition/Practitioner/nNGM"
  }

  // TODO get practitioner by NMS Endpoint: https://nngm-nms.medicalsyn.com/api/v1.0/Public/Person
  identifier {
    system = "urn:centraxx"
    value = context.source[attendingDoctor().contact().syncId()]
  }
}

class NNGMPractitioner {
  String internalSequenceIdentifier
  String title
  String firstName
  String lastName
  List<String> organizationAssignments = []
}


private List<NNGMPractitioner> getPractitionerFromNMS() {
  // A thread-safe cache with time-based expiration
  final ConcurrentHashMap<String, Map<String, Object>> cache = new ConcurrentHashMap<>()

  final String httpMethod = "GET"
  final URL url = new URL("https://nngm-nms.medicalsyn.com/api/v1.0/Public/Person")

  // Check if the result is already in the cache and not expired
  def cacheKey = url.toString()
  def cachedResult = cache.get(cacheKey)
  if (cachedResult != null && cachedResult.timestamp + 60 * 1000 > System.currentTimeMillis()) {
    println("Returning cached result for $url")
    return cachedResult.data as List<NNGMPractitioner>
  }

  final def json = queryMdr(url, httpMethod)
  return json?.data?.collect { practitionerData ->
    // Collect data and return
    new NNGMPractitioner(
            internalSequenceIdentifier: practitionerData.internalSequenceIdentifier,
            title: practitionerData.title,
            firstName: practitionerData.firstName,
            lastName: practitionerData.lastName,
            organizationAssignments: practitionerData.organizationAssignments ?: []
    )
  }
}
/**
 * Executes the REST query, validates and returns the result, if exists.
 * @return JsonSlurper with the REST response or null, if response was not valid.
 */
private static def queryMdr(final URL url, final String httpMethod) {
  final HttpURLConnection connection = url.openConnection() as HttpURLConnection
  connection.setRequestMethod(httpMethod)
  connection.setRequestProperty("Accept", "application/json")

  if (!validateResponse(connection.getResponseCode(), httpMethod, url)) {
    return null
  }

  return connection.getInputStream().withCloseable { final inStream ->
    new JsonSlurper().parse(inStream as InputStream)
  }
}

/**
 * Validates the HTTP response
 * @return true, if status code is valid 200 or otherwise false. A false response is logged.
 */
private static boolean validateResponse(final int httpStatusCode, final String httpMethod, final URL url) {
  final int expectedStatusCode = 200
  if (httpStatusCode != expectedStatusCode) {
    LoggerFactory.getLogger(getClass()).warn("'" + httpMethod + "' request on '" + url + "' returned status code: " + httpStatusCode + ". Expected: " + expectedStatusCode)
    return false
  }
  return true
}
