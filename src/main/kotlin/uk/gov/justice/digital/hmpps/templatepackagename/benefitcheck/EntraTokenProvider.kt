package uk.gov.justice.digital.hmpps.templatepackagename.benefitcheck

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.util.UUID

fun interface IntegrationHubTokenProvider {
  fun accessToken(): String
}

@Component
class EntraTokenProvider(
  @Value("\${integration-hub-api.entra.tenant-id:}") private val tenantId: String,
  @Value("\${integration-hub-api.entra.client-id:}") private val clientId: String,
  @Value("\${integration-hub-api.entra.client-secret:}") private val clientSecret: String,
  @Value("\${integration-hub-api.entra.scope:}") private val scope: String,
  @Value("\${integration-hub-api.timeout}") private val timeout: Duration,
) : IntegrationHubTokenProvider {
  // Missing pilot settings must not stop unrelated health/template tests at startup.
  // They are required when the outbound pilot client is used.
  private val manager by lazy {
    val tenant = UUID.fromString(tenantId).toString()
    UUID.fromString(clientId)
    require(clientSecret.isNotBlank() && scope.endsWith("/.default")) { "Configure the pilot consumer credential and API scope" }
    val registration = ClientRegistration.withRegistrationId("integration-hub")
      .clientId(clientId)
      .clientSecret(clientSecret)
      .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
      .scope(scope)
      .tokenUri("https://login.microsoftonline.com/$tenant/oauth2/v2.0/token")
      .build()
    val tokenClient = WebClient.builder()
      .clientConnector(ReactorClientHttpConnector(HttpClient.create().followRedirect(false).responseTimeout(timeout)))
      .build()
    createPilotTokenManager(registration, tokenClient)
  }

  override fun accessToken(): String = manager.authorize(
    OAuth2AuthorizeRequest.withClientRegistrationId("integration-hub")
      .principal("integration-hub-pilot")
      .build(),
  ).map { it.accessToken.tokenValue }.block(timeout)
    ?: throw IllegalStateException("Entra did not return a pilot access token")
}

internal fun createPilotTokenManager(
  registration: ClientRegistration,
  tokenClient: WebClient,
): ReactiveOAuth2AuthorizedClientManager {
  val registrations = InMemoryReactiveClientRegistrationRepository(registration)
  val service = InMemoryReactiveOAuth2AuthorizedClientService(registrations)
  val tokenResponseClient = WebClientReactiveClientCredentialsTokenResponseClient().apply { setWebClient(tokenClient) }
  val provider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
    .clientCredentials { it.accessTokenResponseClient(tokenResponseClient) }
    .build()
  return AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(registrations, service).apply {
    setAuthorizedClientProvider(provider)
  }
}
