package io.suggest.es

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder
import io.suggest.es.model.{EsPublishActoryFactory, EsScrollPublisherFactory, EsScrollSubscriptionFactory}
import io.suggest.es.util.IEsClient

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.06.17 12:27
  */
class GuiceDiModule extends AbstractModule {

  override def configure(): Unit = {

    install(
      new FactoryModuleBuilder()
        .build( classOf[EsScrollSubscriptionFactory] )
    )

    install(
      new FactoryModuleBuilder()
        .build( classOf[EsScrollPublisherFactory] )
    )

    install(
      new FactoryModuleBuilder()
        .build( classOf[EsPublishActoryFactory] )
    )

    bind( classOf[org.elasticsearch.client.Client] )
      .toProvider( classOf[IEsClient] )

  }

}
