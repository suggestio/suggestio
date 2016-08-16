package util.adr.phantomjs

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder
import util.adr.IAdRrr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.08.16 18:53
  * Description: Код для инсталляции DI factory ad-рендерера [[PhantomJsRrr]] в Guice.
  */

class GuiceDiModule extends AbstractModule {
  override def configure(): Unit = {
    // Инстантиировать factory для сборки экземпляров ISioUser.
    install(
      new FactoryModuleBuilder()
        .implement(classOf[IAdRrr], classOf[PhantomJsRrr])
        .build( classOf[PhantomJsRrrDiFactory] )
    )
  }
}
