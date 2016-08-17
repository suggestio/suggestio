package io.suggest.di

import play.api.libs.ws.WSClient

/** Клиент web-services. */
trait IWsClient {
  implicit def wsClient: WSClient
}
