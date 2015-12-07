package util.di

import io.suggest.event.SioNotifierStaticClientI
import org.elasticsearch.client.Client
import play.api.inject._
import play.api.{Configuration, Environment}
import util.SiowebEsUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.10.15 18:12
 * Description: Тут инициализации DI-конфигурации под нужды web21.
 */
class DiModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[Client]
        .to( SiowebEsUtil.client ),
      bind[SioNotifierStaticClientI]
        .to( util.event.SiowebNotifier.Implicts.sn )
    )
  }

}
