package models.mup

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.02.18 23:55
  * Description: DI-конфигурация для upload-моделей.
  */
class GuiceDiModule extends AbstractModule {

  override def configure(): Unit = {
    install(
      new FactoryModuleBuilder()
        .implement( classOf[IUploadCtx], classOf[MUploadCtx] )
        .build( classOf[IUploadCtxFactory] )
    )
  }

}
