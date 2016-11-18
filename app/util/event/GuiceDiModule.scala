package util.event

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder
import io.suggest.event.SioNotifierStaticClientI

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.04.16 19:52
  * Description: Поддержка Guice для sio-notifier.
  */
class GuiceDiModule extends AbstractModule {

  override def configure(): Unit = {

    bind( classOf[SioNotifierStaticClientI] )
      .to( classOf[SiowebNotifier] )

    install(
      new FactoryModuleBuilder()
        .build( classOf[ISiowebNotifierActorsFactory] )
    )
  }
}
