package controllers

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.acl._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.05.14 18:18
 * Description: Контроллер для управления процессом размещения рекламных карточек с узла на узел:
 * - узел 1 размещает рекламу на других узлах (форма, сабмит и т.д.).
 * - узелы-получатели одобряют или отсеивают входящие рекламные карточки.
 */
object MarketAdv extends SioController {

  /** Страница размещения рекламной карточки. */
  def infoForAd(adId: String) = CanAdvertiseAd(adId).async { implicit request =>
    ???
  }

  /** Сабмит формы размещения рекламной карточки. */
  def advertiseFormSubmit(adId: String) = CanAdvertiseAd(adId).async { implicit request =>
    ???
  }

}
