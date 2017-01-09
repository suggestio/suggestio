package io.suggest.di

import com.google.inject.{Inject, Provider, Singleton}
import com.sksamuel.elastic4s.ElasticClient
import org.elasticsearch.client.Client
import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}

/**
  * Интерфейс для поля с инжектируемым инстансом scala4s-клиента для elasticsearch.
  *
  * @see [[https://github.com/sksamuel/elastic4s]]
  */
trait IEs4sClient {

  /** Инстанс elastic4s-клиента. */
  val es4sClient: ElasticClient

}


/** Конфигуратор инжекции модулей es4s-клиента. */
class Es4sClientModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[ElasticClient]
        .toProvider[Es4sClientProvider]
    )
  }
}


/** Inject-провайдер инстанса ElasticClient. */
@Singleton
class Es4sClientProvider @Inject() (client: Client) extends Provider[ElasticClient] {

  val es4sClient = ElasticClient.fromClient(client)

  override def get(): ElasticClient = {
    es4sClient
  }

}
