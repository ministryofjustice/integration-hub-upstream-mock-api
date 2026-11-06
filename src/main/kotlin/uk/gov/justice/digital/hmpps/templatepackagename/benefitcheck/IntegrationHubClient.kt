package uk.gov.justice.digital.hmpps.templatepackagename.benefitcheck

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.JsonNode
import java.net.URI
import java.time.Duration

@Component
class IntegrationHubClient(
  builder: WebClient.Builder,
  @Value("\${integration-hub-api.base-url}") baseUrl: String,
  private val tokenProvider: IntegrationHubTokenProvider,
  @Value("\${integration-hub-api.timeout}") private val timeout: Duration,
) {
  init {
    val target = URI(baseUrl)
    val localHttp = target.scheme == "http" && target.host in setOf("localhost", "127.0.0.1", "[::1]")
    require((target.scheme == "https" || localHttp) && target.host != null && target.userInfo == null && target.query == null && target.fragment == null) {
      "Use HTTPS for the Integration Hub API; HTTP is allowed only for local tests"
    }
  }

  private val webClient = builder.baseUrl(baseUrl).build()

  fun createAssessment(request: BenefitAssessmentRequest, correlationId: String?): IntegrationHubResponse = try {
    webClient.post()
      .uri(ASSESSMENTS_PATH)
      .contentType(MediaType.APPLICATION_JSON)
      .headers { headers ->
        headers.setBearerAuth(tokenProvider.accessToken())
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
      .block(timeout) ?: throw IntegrationHubUnavailableException()
  } catch (error: Exception) {
    if (error is IntegrationHubUnavailableException) throw error
    throw IntegrationHubUnavailableException(error)
  }

  private companion object {
    const val ASSESSMENTS_PATH = "/v1/benefit-checks/assessments"
    const val CORRELATION_ID_HEADER = "x-correlation-id"
  }
}

class IntegrationHubUnavailableException(cause: Throwable? = null) : RuntimeException("Integration Hub did not return a response", cause)
