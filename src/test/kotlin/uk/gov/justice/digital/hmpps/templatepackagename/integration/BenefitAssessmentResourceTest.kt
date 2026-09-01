package uk.gov.justice.digital.hmpps.templatepackagename.integration

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.templatepackagename.integration.wiremock.IntegrationHubApiMockServer
import uk.gov.justice.digital.hmpps.templatepackagename.integration.wiremock.IntegrationHubApiMockServer.Companion.integrationHubApi

@ExtendWith(IntegrationHubApiMockServer::class)
class BenefitAssessmentResourceTest : IntegrationTestBase() {

  @Nested
  @DisplayName("POST /v1/benefit-checks/assessments")
  inner class CreateAssessment {

    @Test
    fun `forwards the request and correlation ID to Integration Hub`() {
      integrationHubApi.stubFor(
        post(urlEqualTo("/v1/benefit-checks/assessments"))
          .withHeader("Authorization", equalTo("Basic Y29uc3VtZXItY2xpZW50OmNvbnN1bWVyLXBhc3N3b3Jk"))
          .withHeader("x-correlation-id", equalTo("consumer-request-123"))
          .willReturn(
            aResponse()
              .withStatus(201)
              .withHeader("content-type", "application/json")
              .withHeader("x-correlation-id", "platform-request-456")
              .withBody(SUCCESS_RESPONSE),
          ),
      )

      webTestClient.post()
        .uri("/v1/benefit-checks/assessments")
        .headers(setAuthorisation(roles = listOf("ROLE_INTEGRATION_HUB__REQUEST_API")))
        .header("x-correlation-id", "consumer-request-123")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isCreated
        .expectHeader().valueEquals("x-correlation-id", "platform-request-456")
        .expectBody()
        .jsonPath("$.requestId").isEqualTo("platform-request-456")
        .jsonPath("$.assessment.decision").isEqualTo("ELIGIBLE")
    }

    @Test
    fun `passes through a platform validation error`() {
      integrationHubApi.stubFor(
        post(urlEqualTo("/v1/benefit-checks/assessments"))
          .willReturn(
            aResponse()
              .withStatus(400)
              .withHeader("content-type", "application/json")
              .withBody("""{"requestId":"platform-request-456","error":{"code":"invalid_request","message":"Invalid nino"}}"""),
          ),
      )

      webTestClient.post()
        .uri("/v1/benefit-checks/assessments")
        .headers(setAuthorisation(roles = listOf("ROLE_INTEGRATION_HUB__REQUEST_API")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$.error.code").isEqualTo("invalid_request")
    }
  }

  private companion object {
    val request =
      """
      {
        "firstName":"Alex", "lastName":"Taylor", "nino":"QQ123456C", "dateOfBirth":"1985-07-12",
        "claimedBenefits":["UNIVERSAL_CREDIT"], "annualIncome":12000, "savingsAmount":500,
        "housingCostsPerMonth":750, "dependantChildren":1, "disabledApplicant":false,
        "caringResponsibilities":false, "postcode":"SW1A 1AA"
      }
      """.trimIndent()

    const val SUCCESS_RESPONSE =
      """{"requestId":"platform-request-456","provider":"mock-benefit-checker","assessment":{"assessmentId":"6f0804b7-c34b-352d-9dc0-a98e2caadd1d","decision":"ELIGIBLE","matchedEntitlements":[],"riskFlags":[],"processedAt":"2026-08-24T12:00:00Z","decisionSummary":"Eligible"}}"""
  }
}
