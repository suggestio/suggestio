package models.mext

import io.suggest.adv.ext.model.MServices._
import io.suggest.adv.ext.model._
import io.suggest.common.menum.EnumJsonReadsT
import io.suggest.util.logs.MacroLogsImplLazy
import models.mext.fb.FacebookService
import models.mext.tw.TwitterService
import models.mext.vk.VkService

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.14 15:13
 * Description: Мегамодель сервисов для внешнего размещения рекламных карточек.
 */

object MExtServices extends MServicesT with MacroLogsImplLazy with EnumJsonReadsT {

  override type T = Val

  /** Экземпляр модели. */
  protected[this] abstract class Val(override val strId: String)
    extends super.Val(strId)
    with IExtService


  /** Сервис вконтакта. */
  val VKONTAKTE: T = new Val(VKONTAKTE_ID) with VkService

  /** Сервис фейсбука. */
  val FACEBOOK: T = new Val(FACEBOOK_ID) with FacebookService

  /** Сервис твиттера. */
  val TWITTER: T = new Val(TWITTER_ID) with TwitterService

}
