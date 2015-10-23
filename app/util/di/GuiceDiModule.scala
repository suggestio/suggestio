package util.di

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder
import models.{Context2Factory, Context2, Context}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.10.15 18:30
 * Description: Модуль конфигурирования Guice для проекта.
 * В отличие от DiModule, тут исповедуется прямое взаимодействия с особенностями guice.
 */
class GuiceDiModule extends AbstractModule {
  override def configure(): Unit = {

    // Поддержка assisted-сборки и инжекции Context2Factory.
    install(
      new FactoryModuleBuilder()
        .implement( classOf[Context], classOf[Context2] )
        .build( classOf[Context2Factory] )
    )

  }
}
