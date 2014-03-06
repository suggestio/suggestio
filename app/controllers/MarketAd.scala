package controllers

import util.PlayMacroLogsImpl
import util.acl.IsSuperuser
import views.html.market.lk.ad._
import models._
import play.api.libs.concurrent.Execution.Implicits._
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.14 11:26
 * Description: Контроллер для работы с рекламным фунционалом.
 */
object MarketAd extends SioController with PlayMacroLogsImpl {

  /**
   * Создание рекламной карточки.
   * @param shopId id магазина.
   */
  def createShopAd(shopId: String) = IsSuperuser.async { implicit request =>
    MShop.getById(shopId) map {
      case Some(mshop)  => Ok(createAdTpl(mshop))
      case None         => NotFound("shop not found: " + shopId)
    }
  }

}
