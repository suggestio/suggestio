package util.event

import io.suggest.event.SioEventT
import io.suggest.event.SioNotifier.Classifier
import models._, MShop.ShopId_t, MMart.MartId_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.14 15:33
 * Description: Событие удаление магазина из торгового центра.
 */
object YmShopDeletedEvent {

  def headSneToken: Option[String] = Some(getClass.getSimpleName)

  def getClassifier(martId: Option[MartId_t] = None, shopId: Option[ShopId_t] = None): Classifier = {
    List(headSneToken, martId, shopId)
  }

}


case class YmShopDeletedEvent(martId: MartId_t, shopId: ShopId_t) extends SioEventT {
  override def getClassifier: Classifier = {
    YmShopDeletedEvent.getClassifier(
      martId = Some(martId),
      shopId = Some(shopId)
    )
  }
}
