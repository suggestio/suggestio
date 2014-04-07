package io.suggest.event

import io.suggest.event.SioNotifier.Classifier
import io.suggest.ym.model._
import MMart._, MShop._
import scala.Some

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.14 14:07
 * Description: SN-событие добавления магазина в торговый центр.
 */
@deprecated("shop+mart arch deprecated. Use AdnNodeSavedEvent", "2014.apr.07")
object YmShopAddedEvent {

  def headSneToken: Option[String] = Some(getClass.getSimpleName)

  def getClassifier(martId: Option[MartId_t] = None, shopId: Option[ShopId_t] = None): Classifier = {
    List(headSneToken, martId, shopId)
  }

}


@deprecated("shop+mart arch deprecated. Use AdnNodeSavedEvent", "2014.apr.07")
case class YmShopAddedEvent(martId: MartId_t, shopId: ShopId_t) extends SioEventT {
  override def getClassifier: Classifier = {
    YmShopAddedEvent.getClassifier(
      martId = Some(martId),
      shopId = Some(shopId)
    )
  }
}