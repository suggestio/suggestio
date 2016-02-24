package util.billing

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder
import util.billing.cron.{DisableExpiredAdvsFactory, ActivateOfflineAdvsFactory}

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
        .build( classOf[ActivateOfflineAdvsFactory] )
    )

    install(
      new FactoryModuleBuilder()
        .build( classOf[DisableExpiredAdvsFactory] )
    )

  }

}
