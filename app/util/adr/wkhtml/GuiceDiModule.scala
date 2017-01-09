package util.adr.wkhtml

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder
import util.adr.IAdRrr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.08.16 18:53
  * Description: Код для инсталляции DI factory ad-рендерера [[WkHtmlRrr]] в Guice.
  */

class GuiceDiModule extends AbstractModule {
  override def configure(): Unit = {
    // Инстантиировать factory для сборки экземпляров ISioUser.
    install(
      new FactoryModuleBuilder()
        .implement(classOf[IAdRrr], classOf[WkHtmlRrr])
        .build( classOf[WkHtmlRrrDiFactory] )
    )
  }
}
