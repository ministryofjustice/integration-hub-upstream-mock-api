package uk.gov.justice.digital.hmpps.templatepackagename.benefitcheck

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.JsonNode
import java.time.Duration

@Component
class IntegrationHubClient(
  builder: WebClient.Builder,
  @Value("\${integration-hub-api.base-url}") baseUrl: String,
  @Value("\${integration-hub-api.username}") private val username: String,
  @Value("\${integration-hub-api.password}") private val password: String,
  @Value("\${integration-hub-api.timeout}") private val timeout: Duration,
) {
  private val webClient = builder.baseUrl(baseUrl).build()

  fun createAssessment(request: BenefitAssessmentRequest, correlationId: String?): IntegrationHubResponse = webClient.post()
    .uri(ASSESSMENTS_PATH)
    .contentType(MediaType.APPLICATION_JSON)
    .headers { headers ->
      headers.setBasicAuth(username, password)
      correlationId?.let { headers[CORRELATION_ID_HEADER] = it }
    }
    .bodyValue(request)
    .exchangeToMono { response ->
      response.bodyToMono(JsonNode::class.java).map { body ->
        IntegrationHubResponse(
          status = response.statusCode().value(),
          correlationId = response.headers().header(CORRELATION_ID_HEADER).firstOrNull(),
          body = body,
        )
      }
    }
    .onErrorMap { error ->
      if (error is IntegrationHubUnavailableException) error else IntegrationHubUnavailableException(error)
    }
    .block(timeout) ?: throw IntegrationHubUnavailableException()

  private companion object {
    const val ASSESSMENTS_PATH = "/v1/benefit-checks/assessments"
    const val CORRELATION_ID_HEADER = "x-correlation-id"
  }
}

class IntegrationHubUnavailableException(cause: Throwable? = null) : RuntimeException("Integration Hub did not return a response", cause)
