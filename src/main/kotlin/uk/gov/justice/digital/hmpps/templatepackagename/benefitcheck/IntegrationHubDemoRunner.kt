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
      log.info("Integration Hub demo response:\n{}", response.body.toPrettyString())
    } else {
      log.error("Integration Hub demo failed: status={}, requestId={}, errorCode={}", response.status, requestId, errorCode)
    }
  }

  private companion object {
    private val log = LoggerFactory.getLogger(IntegrationHubDemoRunner::class.java)

    private val DEMO_REQUEST =
      BenefitAssessmentRequest(
        firstName = "Alex",
        lastName = "Morgan",
        nino = "AA123456A",
        dateOfBirth = LocalDate.parse("1991-08-25"),
        claimedBenefits = listOf("UNIVERSAL_CREDIT", "CHILD_BENEFIT"),
        annualIncome = BigDecimal("21000"),
        savingsAmount = BigDecimal("1000"),
        housingCostsPerMonth = BigDecimal("950"),
        dependantChildren = 2,
        disabledApplicant = true,
        caringResponsibilities = false,
        postcode = "SW1A 1AA",
      )
  }
}
