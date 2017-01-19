package util.adv

import akka.actor.Actor
import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder
import util.adv.build.{AdvBuilder, AdvBuilderFactory, IAdvBuilder}
import util.adv.ext._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.10.15 11:06
 * Description: Поддержка Guice для adv-подсистемы.
 */
class GuiceDiModule extends AbstractModule {

  override def configure(): Unit = {
    // adv ext root actor
    install(
      new FactoryModuleBuilder()
        .build( classOf[AdvExtWsActorFactory] )
    )

    // adv builder
    install(
      new FactoryModuleBuilder()
        .implement( classOf[IAdvBuilder], classOf[AdvBuilder] )
        .build( classOf[AdvBuilderFactory] )
    )

    // ext services
    install(
      new FactoryModuleBuilder()
        .implement( classOf[Actor], classOf[OAuth1ServiceActor] )
        .build( classOf[OAuth1ServiceActorFactory] )
    )
    install(
      new FactoryModuleBuilder()
        .implement( classOf[Actor], classOf[AdvExtServiceActor] )
        .build( classOf[AdvExtServiceActorFactory] )
    )

    // adv ext targets
    install(
      new FactoryModuleBuilder()
        .build( classOf[OAuth1TargetActorFactory] )
    )
    install(
      new FactoryModuleBuilder()
        .build( classOf[AdvExtTargetActorFactory] )
    )
  }

}
