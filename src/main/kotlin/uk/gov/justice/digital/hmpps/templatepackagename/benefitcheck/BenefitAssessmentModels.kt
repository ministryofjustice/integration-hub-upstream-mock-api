package uk.gov.justice.digital.hmpps.templatepackagename.benefitcheck

import tools.jackson.databind.JsonNode
import java.math.BigDecimal
import java.time.LocalDate

data class BenefitAssessmentRequest(
  val firstName: String,
  val lastName: String,
  val nino: String,
  val dateOfBirth: LocalDate,
  val claimedBenefits: List<String>,
  val annualIncome: BigDecimal,
  val savingsAmount: BigDecimal,
  val housingCostsPerMonth: BigDecimal,
  val dependantChildren: Int,
  val disabledApplicant: Boolean,
  val caringResponsibilities: Boolean,
  val postcode: String,
)

data class IntegrationHubResponse(
  val status: Int,
  val correlationId: String?,
  val body: JsonNode,
)
