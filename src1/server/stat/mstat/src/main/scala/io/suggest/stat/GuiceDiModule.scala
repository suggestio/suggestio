package io.suggest.stat

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder
import io.suggest.stat.m.MStatsTmpFactory

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.09.16 16:32
  * Description: Конфигуратор для guice для нужд модуля stat.
  */
class GuiceDiModule extends AbstractModule {

  override def configure(): Unit = {
    install(
      new FactoryModuleBuilder()
        .build( classOf[MStatsTmpFactory] )
    )
  }

}
