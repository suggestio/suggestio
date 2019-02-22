package models.mext

import io.suggest.ext.svc.{MExtService, MExtServices}
import models.mext.fb.FacebookService
import models.mext.gu.GosUslugiService
import models.mext.tw.TwitterService
import models.mext.vk.VkService

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.14 15:13
 * Description: Мегамодель сервисов для внешнего размещения рекламных карточек.
 */
object MExtServicesJvm {

  def forService(svc: MExtService): IExtService = {
    svc match {
      case MExtServices.VKontakte => new VkService
      case MExtServices.FaceBook  => new FacebookService
      case MExtServices.Twitter   => new TwitterService
      case MExtServices.GosUslugi => new GosUslugiService
    }
  }

}

