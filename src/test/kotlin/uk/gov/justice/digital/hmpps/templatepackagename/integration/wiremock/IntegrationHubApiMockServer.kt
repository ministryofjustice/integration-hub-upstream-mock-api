package uk.gov.justice.digital.hmpps.templatepackagename.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class IntegrationHubApiMockServer :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val integrationHubApi = WireMockServer(8091)
  }

  override fun beforeAll(context: ExtensionContext) {
    integrationHubApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    integrationHubApi.resetAll()
  }

  override fun afterAll(context: ExtensionContext) {
    integrationHubApi.stop()
  }
}
