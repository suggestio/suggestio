package util.adv.gtag

import com.google.inject.Inject
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.tags.{MItemTag, MItemTags}
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.m.item.{MItem, MItems}
import models.MPrice
import models.adv.gtag.MAdvFormResult
import models.mproj.MCommonDi
import slick.dbio.DBIOAction
import util.billing.Bill2Util

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 13:43
 * Description: Утиль для биллинга размещений в тегах.
 */
class GeoTagAdvBillUtil @Inject() (
  bill2Util                           : Bill2Util,
  mItems                              : MItems,
  mItemTags                           : MItemTags,
  mCommonDi                           : MCommonDi
) {

  import mCommonDi._
  import dbConfig.driver.api._

  /**
   * Закинуть в корзину bill-v2
   * @param orderId id-ордера-корзины, т.е. текущего заказа. Туда надо добавить возможную покупку.
   *                Например, выхлоп [[util.billing.Bill2Util.ensureNodeCart()]].
   * @param advTargetId id узла-цели размещения тегов, обычно рекламная карточка.
   * @param res Данные по размещаемым тегам.
   * @return Фьючерс c результатом.
   */
  def addToOrder(orderId: Gid_t, advTargetId: String, price: MPrice, res: MAdvFormResult): Future[_] = {
    // Собираем инстанс нового товара в корзине: пачка тегов.
    val mitem0 = MItem(
      orderId   = orderId,
      iType     = MItemTypes.GeoTag,
      status    = MItemStatuses.Draft,
      price     = price,
      adId      = advTargetId,
      dateStart = res.period.dateStart.toDateTimeAtStartOfDay,
      dateEnd   = res.period.dateEnd.toDateTimeAtStartOfDay,
      rcvrIdOpt = None
    )

    // Собираем экшен для внесения item'а с заказанными тегами в БД.
    val slkAction = for {
      mitem2    <- mItems.insertOne(mitem0)
      mItemTags <- {
        // Сгенерить и сохранить tag_item'ы для тегов в res.
        val itemId = mitem2.id.get
        val mItemTagActs = for (tb <- res.tags) yield {
          val mItemTag = MItemTag(itemId, face = tb.face, nodeId = tb.nodeId)
          mItemTags.insertOne(mItemTag)
        }
        DBIOAction.seq(mItemTagActs: _*)
      }
    } yield {
      mitem2
    }

    // Запустить сохранение нового item'а.
    dbConfig.db.run(slkAction.transactionally)
  }

}
