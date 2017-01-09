package util.mail

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.16 20:34
  * Description: Настройки Guice для util.mail.*
  */
class GuiceDiModule extends AbstractModule {
  override def configure(): Unit = {
    install(
      new FactoryModuleBuilder()
        .build( classOf[CommonsEmailBuildersFactory] )
    )
    install(
      new FactoryModuleBuilder()
        .build( classOf[PlayMailerEmailBuildersFactory] )
    )
  }
}
