package util.event

import io.suggest.event.SioEventT
import io.suggest.event.SioNotifier.Classifier
import models._
import MMart.MartId_t, MShop.ShopId_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.14 14:07
 * Description: SN-событие добавления магазина в торговый центр.
 */
object YmShopAddedEvent {

  def headSneToken: Option[String] = Some(getClass.getSimpleName)

  def getClassifier(martId: Option[MartId_t] = None, shopId: Option[ShopId_t] = None): Classifier = {
    List(headSneToken, martId, shopId)
  }

}


case class YmShopAddedEvent(martId: MartId_t, shopId: ShopId_t) extends SioEventT {
  override def getClassifier: Classifier = {
    YmShopAddedEvent.getClassifier(
      martId = Some(martId),
      shopId = Some(shopId)
    )
  }
}