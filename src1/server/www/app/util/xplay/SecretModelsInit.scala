package util.xplay

import io.suggest.sec.m.{SecretKeyInit, SecretKeyInitializer}
import javax.inject.Inject
import models.adv.MExtAdvQs
import models.adv.ext.act.ActorPathQs
import models.blk.OneAdQsArgs
import models.im.MImgT
import models.mup.MUploadTargetQs
import models.usr.MEmailRecoverQs
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.09.18 22:24
  * Description: Инициализатор моделей, статически-требующих ключи из конфига.
  *
  * Т.к. нормально подружить object'ы и inject с разбега не получилось (хотя можно инжектить переменные),
  * тут код для "ручного" запихивания ключей в модели через guice.
  *
  * Сингтон, т.к. дока требует всем быть Singleton'ами.
  * https://www.playframework.com/documentation/2.6.x/ScalaDependencyInjection#Stopping/cleaning-up
  */
class SecretModelsInit @Inject() (
                                   secretKeyInitializer: SecretKeyInitializer,
                                   applicationLifecycle: ApplicationLifecycle,
                                 ) {

  /** Список моделей для инициализации. */
  def MODELS: Seq[SecretKeyInit] = {
    MExtAdvQs ::
    ActorPathQs ::
    OneAdQsArgs ::
    MImgT ::
    MUploadTargetQs ::
    MEmailRecoverQs ::
    Nil
  }

  // Запустить иницализацию моделей, требующих секретного ключа.
  secretKeyInitializer.initAll( MODELS: _* )

  /** Сбросить все значения на исходную при выключении. Чисто на всякий случай. */
  applicationLifecycle.addStopHook { () =>
    secretKeyInitializer.resetAll( MODELS: _* )
    Future.successful( () )
  }

}
