package util.xplay

import io.suggest.sec.m.SecretKeyInitializer
import javax.inject.Inject
import models.adv.MExtAdvQs
import models.adv.ext.act.ActorPathQs
import models.blk.OneAdQsArgs
import models.im.MImgT
import models.mup.MUploadTargetQs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.09.18 22:24
  * Description: Инициализатор моделей, статически-требующих ключи из конфига.
  *
  * Т.к. нормально подружить object'ы и inject с разбега не получилось (хотя можно инжектить переменные),
  * тут код для "ручного" запихивания ключей в модели через guice.
  */
class SecretModelsInit @Inject() (secretKeyInitializer: SecretKeyInitializer) {

  // Запустить иницализацию моделей, требующих секретного ключа.
  secretKeyInitializer.doInitAll(
    MExtAdvQs,
    ActorPathQs,
    OneAdQsArgs,
    MImgT,
    MUploadTargetQs
  )

}
