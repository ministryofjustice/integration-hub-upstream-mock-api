package uk.gov.justice.digital.hmpps.templatepackagename.integration

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.templatepackagename.benefitcheck.BenefitAssessmentRequest
import uk.gov.justice.digital.hmpps.templatepackagename.benefitcheck.IntegrationHubClient
import uk.gov.justice.digital.hmpps.templatepackagename.integration.wiremock.IntegrationHubApiMockServer
import uk.gov.justice.digital.hmpps.templatepackagename.integration.wiremock.IntegrationHubApiMockServer.Companion.integrationHubApi
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate

@ExtendWith(IntegrationHubApiMockServer::class)
class IntegrationHubClientTest {
  private val client =
    IntegrationHubClient(
      builder = WebClient.builder(),
      baseUrl = "http://localhost:8091",
      username = "consumer-client",
      password = "consumer-password",
      timeout = Duration.ofSeconds(2),
    )

  @Test
  fun `sends the assessment directly to Integration Hub`() {
    integrationHubApi.stubFor(
      post(urlEqualTo("/v1/benefit-checks/assessments"))
        .willReturn(
          aResponse()
            .withStatus(201)
            .withHeader("content-type", "application/json")
            .withHeader("x-correlation-id", "platform-request-456")
            .withBody(SUCCESS_RESPONSE),
        ),
    )

    val response = client.createAssessment(request, "consumer-request-123")

    assertThat(response.status).isEqualTo(201)
    assertThat(response.correlationId).isEqualTo("platform-request-456")
    assertThat(response.body.get("requestId").asString()).isEqualTo("platform-request-456")
    assertThat(response.body.get("assessment").get("decision").asString()).isEqualTo("ELIGIBLE")
    integrationHubApi.verify(
      postRequestedFor(urlEqualTo("/v1/benefit-checks/assessments"))
        .withHeader("Authorization", equalTo("Basic Y29uc3VtZXItY2xpZW50OmNvbnN1bWVyLXBhc3N3b3Jk"))
        .withHeader("x-correlation-id", equalTo("consumer-request-123"))
        .withRequestBody(containing("\"nino\":\"QQ123456C\"")),
    )
  }

  @Test
  fun `returns a platform validation error to the consumer application`() {
    integrationHubApi.stubFor(
      post(urlEqualTo("/v1/benefit-checks/assessments"))
        .willReturn(
          aResponse()
            .withStatus(400)
            .withHeader("content-type", "application/json")
            .withBody("""{"requestId":"platform-request-456","error":{"code":"invalid_request","message":"Invalid nino"}}"""),
        ),
    )

    val response = client.createAssessment(request, null)

    assertThat(response.status).isEqualTo(400)
    assertThat(response.body.get("error").get("code").asString()).isEqualTo("invalid_request")
  }

  private companion object {
    val request =
      BenefitAssessmentRequest(
        firstName = "Alex",
        lastName = "Taylor",
        nino = "QQ123456C",
        dateOfBirth = LocalDate.parse("1985-07-12"),
        claimedBenefits = listOf("UNIVERSAL_CREDIT"),
        annualIncome = BigDecimal("12000"),
        savingsAmount = BigDecimal("500"),
        housingCostsPerMonth = BigDecimal("750"),
        dependantChildren = 1,
        disabledApplicant = false,
        caringResponsibilities = false,
        postcode = "SW1A 1AA",
      )

    const val SUCCESS_RESPONSE =
      """{"requestId":"platform-request-456","provider":"mock-benefit-checker","assessment":{"assessmentId":"6f0804b7-c34b-352d-9dc0-a98e2caadd1d","decision":"ELIGIBLE","matchedEntitlements":[],"riskFlags":[],"processedAt":"2026-08-24T12:00:00Z","decisionSummary":"Eligible"}}"""
  }
}
