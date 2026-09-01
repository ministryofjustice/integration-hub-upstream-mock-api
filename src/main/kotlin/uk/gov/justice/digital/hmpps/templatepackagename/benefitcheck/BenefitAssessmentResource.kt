package uk.gov.justice.digital.hmpps.templatepackagename.benefitcheck

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.JsonNode

@RestController
@PreAuthorize("hasRole('ROLE_INTEGRATION_HUB__REQUEST_API')")
@RequestMapping("/v1/benefit-checks/assessments", produces = ["application/json"])
class BenefitAssessmentResource(private val integrationHubClient: IntegrationHubClient) {

  @PostMapping(consumes = ["application/json"])
  fun createAssessment(
    @RequestBody request: BenefitAssessmentRequest,
    @RequestHeader(name = CORRELATION_ID_HEADER, required = false) correlationId: String?,
  ): ResponseEntity<JsonNode> {
    val response = integrationHubClient.createAssessment(request, correlationId)
    return ResponseEntity.status(response.status)
      .headers(HttpHeaders().apply { response.correlationId?.let { set(CORRELATION_ID_HEADER, it) } })
      .body(response.body)
  }

  private companion object {
    const val CORRELATION_ID_HEADER = "x-correlation-id"
  }
}
