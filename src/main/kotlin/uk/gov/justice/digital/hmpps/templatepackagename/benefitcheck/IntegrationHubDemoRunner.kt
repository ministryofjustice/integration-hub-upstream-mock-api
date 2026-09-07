package uk.gov.justice.digital.hmpps.templatepackagename.benefitcheck

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component
@ConditionalOnProperty(prefix = "integration-hub-demo", name = ["enabled"], havingValue = "true")
class IntegrationHubDemoRunner(
  private val integrationHubClient: IntegrationHubClient,
  @Value("\${integration-hub-demo.correlation-id}") private val correlationId: String,
) : ApplicationRunner {
  override fun run(args: ApplicationArguments) {
    try {
      logResponse(integrationHubClient.createAssessment(DEMO_REQUEST, correlationId))
    } catch (error: IntegrationHubUnavailableException) {
      log.error("Integration Hub demo timed out or could not connect: {}", error.cause?.message ?: error.message)
    }
  }

  private fun logResponse(response: IntegrationHubResponse) {
    val requestId = response.body.get("requestId")?.asString()
    val errorCode = response.body.get("error")?.get("code")?.asString()

    if (response.status in 200..299) {
      log.info(
        "Integration Hub demo completed: status={}, requestId={}, provider={}",
        response.status,
        requestId,
        response.body.get("provider")?.asString(),
      )
    } else {
      log.error("Integration Hub demo failed: status={}, requestId={}, errorCode={}", response.status, requestId, errorCode)
    }
  }

  private companion object {
    private val log = LoggerFactory.getLogger(IntegrationHubDemoRunner::class.java)

    private val DEMO_REQUEST =
      BenefitAssessmentRequest(
        firstName = "Test",
        lastName = "User",
        nino = "AA123456A",
        dateOfBirth = LocalDate.parse("1990-01-01"),
        claimedBenefits = listOf("UNIVERSAL_CREDIT"),
        annualIncome = BigDecimal("25000"),
        savingsAmount = BigDecimal("500"),
        housingCostsPerMonth = BigDecimal("800"),
        dependantChildren = 0,
        disabledApplicant = false,
        caringResponsibilities = false,
        postcode = "SW1A 1AA",
      )
  }
}
