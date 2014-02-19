package controllers

import io.suggest.util.MacroLogsImpl
import util.acl.IsSuperuser
import models._
import views.html.market.lk.shop.offers._
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 11:37
 * Description: Управления офферами, т.к. коммерческими предложениями. Конкретные продавцы, владельцы магазинов
 * или кто-то ещё могут вносить коррективы в список представленных товаров.
 * Следует помнить, что предложения носят неточный характер и носят промо-характер.
 */
object MarketOffer extends SioController with MacroLogsImpl {
  import LOGGER._

  // TODO Когда будут юзеры, тут не должно быть никаких IsSuperuser

  /** Показать список офферов указанного магазина. */
  def showShopOffers(shopId: Int) = IsSuperuser.async { implicit request =>
    MShopPromoOffer.getAllForShop(shopId).map { offers =>
      Ok(listOffersTpl(offers))
    }
  }


}
