package uk.gov.justice.digital.hmpps.templatepackagename.integration

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.templatepackagename.benefitcheck.createPilotTokenManager
import uk.gov.justice.digital.hmpps.templatepackagename.integration.wiremock.IntegrationHubApiMockServer
import uk.gov.justice.digital.hmpps.templatepackagename.integration.wiremock.IntegrationHubApiMockServer.Companion.integrationHubApi
import java.time.Duration

@ExtendWith(IntegrationHubApiMockServer::class)
class PilotTokenManagerTest {
  private fun manager() = createPilotTokenManager(
    ClientRegistration.withRegistrationId("integration-hub")
      .clientId("pilot-consumer")
      .clientSecret("test-secret")
      .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
      .scope("api://pilot/.default")
      .tokenUri("http://localhost:8091/token")
      .build(),
    WebClient.builder().build(),
  )

  private val request = OAuth2AuthorizeRequest.withClientRegistrationId("integration-hub")
    .principal("integration-hub-pilot")
    .build()

  private fun stubToken(expires: Int) {
    integrationHubApi.stubFor(
      post(urlEqualTo("/token")).willReturn(
        aResponse().withHeader("content-type", "application/json")
          .withBody("""{"access_token":"test-access-token","token_type":"Bearer","expires_in":$expires}"""),
      ),
    )
  }

  @Test
  fun `obtains a token and reuses it while valid`() {
    stubToken(3600)
    val manager = manager()
    repeat(2) {
      assertThat(manager.authorize(request).block(Duration.ofSeconds(3))!!.accessToken.tokenValue).isEqualTo("test-access-token")
    }
    integrationHubApi.verify(
      1,
      postRequestedFor(urlEqualTo("/token"))
        .withRequestBody(containing("grant_type=client_credentials"))
        .withRequestBody(containing("client_id=pilot-consumer"))
        .withRequestBody(containing("client_secret=test-secret"))
        .withRequestBody(containing("scope=api%3A%2F%2Fpilot%2F.default")),
    )
  }

  @Test
  fun `renews a token close to expiry`() {
    stubToken(1)
    val manager = manager()
    repeat(2) { manager.authorize(request).block(Duration.ofSeconds(3)) }
    integrationHubApi.verify(2, postRequestedFor(urlEqualTo("/token")))
  }

  @Test
  fun `failed token request does not return a credential`() {
    integrationHubApi.stubFor(
      post(urlEqualTo("/token")).willReturn(
        aResponse().withStatus(401).withHeader("content-type", "application/json")
          .withBody("""{"error":"invalid_client"}"""),
      ),
    )
    assertThatThrownBy { manager().authorize(request).block(Duration.ofSeconds(3)) }
      .hasMessageContaining("invalid_client")
  }
}
