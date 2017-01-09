package io.suggest.loc.geo.ipgeobase

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.09.16 23:38
  * Description: Настройка Guice для подсистемы ipgeobase.
  */
class GuiceDiModule extends AbstractModule {

  override def configure(): Unit = {
    install(
      new FactoryModuleBuilder()
        .build( classOf[MCitiesTmpFactory] )
    )
    install(
      new FactoryModuleBuilder()
        .build( classOf[MIpRangesTmpFactory] )
    )
  }

}
