package io.suggest.event

import io.suggest.ym.model.MShop.ShopId_t
import io.suggest.event.SioNotifier.Classifier
import io.suggest.ym.model.MShop

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.03.14 14:03
 * Description: События MShop.
 */
@deprecated("shop+mart arch deprecated. Use AdnNodeSavedEvent", "2014.apr.07")
object MShopSavedEvent {

  val headSneToken: Option[String] = Some(getClass.getSimpleName)

  def getClassifier(shopId: Option[ShopId_t] = None): Classifier = {
    List(headSneToken, shopId)
  }

}

@deprecated("shop+mart arch deprecated. Use AdnNodeSavedEvent", "2014.apr.07")
case class MShopSavedEvent(mshop: MShop) extends SioEventT {
  def getClassifier: Classifier = MShopSavedEvent.getClassifier(mshop.id)
}


/** Событие переключения через [[io.suggest.ym.model.MShop.setIsEnabled()]]. */
@deprecated("shop+mart arch deprecated. Use AdnNodeOnOffEvent", "2014.apr.07")
object MShopOnOffEvent {

  val headSneToken: Option[String] = Some(getClass.getSimpleName)

  def getClassifier(shopId: Option[ShopId_t] = None, isEnabled: Option[Boolean] = None): Classifier = {
    List(headSneToken, shopId, isEnabled)
  }

}

/** Экземпляр события переключение через [[io.suggest.ym.model.MShop.setIsEnabled()]]. */
@deprecated("shop+mart arch deprecated. Use AdnNodeOnOffEvent", "2014.apr.07")
case class MShopOnOffEvent(shopId: ShopId_t, isEnabled: Boolean, reason: Option[String]) extends SioEventT {
  def getClassifier: Classifier = MShopOnOffEvent.getClassifier(
    shopId = Option(shopId),
    isEnabled = Some(isEnabled)
  )
}
