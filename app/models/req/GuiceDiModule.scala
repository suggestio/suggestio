package models.req

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 17:00
 * Description: Настройки инжекции для моделей m.req.
 */
class GuiceDiModule extends AbstractModule {
  override def configure(): Unit = {
    // Инстантиировать factory для сборки экземпляров ISioUser.
    install(
      new FactoryModuleBuilder()
        .build( classOf[MSioUserLazyFactory] )
    )
  }
}
