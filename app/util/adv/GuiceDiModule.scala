package util.adv

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder
import util.adv.build.{AdvBuilder, IAdvBuilder, AdvBuilderFactory}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.10.15 11:06
 * Description: Поддержка Guice для adv-подсистемы.
 */
class GuiceDiModule extends AbstractModule {

  override def configure(): Unit = {
    install(
      new FactoryModuleBuilder()
        .build( classOf[ExtAdvWsActorFactory] )
    )

    install(
      new FactoryModuleBuilder()
        .implement( classOf[IAdvBuilder], classOf[AdvBuilder] )
        .build( classOf[AdvBuilderFactory] )
    )
  }

}
