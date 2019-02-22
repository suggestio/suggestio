package models.mext.gu

import models.mext.{IExtService, ILoginProvider}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.02.19 14:53
  * Description: JVM-часть модели для нужд гос.услуг.
  */
class GosUslugiService extends IExtService {

  override def loginProvider: Option[ILoginProvider] = ???

}
