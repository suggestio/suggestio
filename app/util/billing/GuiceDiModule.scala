package util.billing

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder
import models.adv.bill.MAdv2Factory

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.11.15 11:02
 * Description: Дополнительные настройки Guice для классов биллинга.
 */
class GuiceDiModule extends AbstractModule {

  override def configure(): Unit = {
    install(
      new FactoryModuleBuilder()
        .build( classOf[AdvertiseOfflineAdvsFactory] )
    )

    install(
      new FactoryModuleBuilder()
        .build( classOf[DepublishExpiredAdvsFactory] )
    )

    install(
      new FactoryModuleBuilder()
        .build( classOf[MAdv2Factory] )
    )
  }

}
