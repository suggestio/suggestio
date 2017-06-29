package io.suggest.es

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder
import io.suggest.es.model.{EsPublishActoryFactory, EsScrollPublisherFactory, EsScrollSubscriptionFactory}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.06.17 12:27
  * Description:
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

  }

}
