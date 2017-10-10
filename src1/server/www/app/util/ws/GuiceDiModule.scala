package util.ws

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.04.16 18:47
  * Description: Поддержка DI через Guice.
  */
class GuiceDiModule extends AbstractModule {

  override def configure(): Unit = {
    // Поддержка assisted-сборки и инжекции Context2Factory.
    install(
      new FactoryModuleBuilder()
        .build( classOf[IWsChannelActorFactory] )
    )
  }

}