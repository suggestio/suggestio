package io.suggest.event

import io.suggest.ym.model.MMartAd
import io.suggest.ym.model.MMart.MartId_t
import io.suggest.event.SioNotifier.Classifier
import io.suggest.ym.model.MShop.ShopId_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.03.14 11:51
 * Description:
 */
object AdSavedEvent {

  val headSneToken: Option[String] = Some(getClass.getSimpleName)

  def getClassifier(martId: Option[MartId_t] = None, shopId: Option[ShopId_t] = None): Classifier = {
    List(headSneToken, martId, shopId)
  }

}

case class AdSavedEvent(mmartAd: MMartAd) extends SioEventT {
  def getClassifier: Classifier = AdSavedEvent.getClassifier(
    martId = Option(mmartAd.receiverIds),
    shopId = mmartAd.producerId
  )
}



/** Событие удаления рекламной карточки из БД. */
object AdDeletedEvent {

  val headSneToken: Option[String] = Some(getClass.getSimpleName)

  def getClassifier(martId: Option[MartId_t] = None, shopId: Option[ShopId_t] = None): Classifier = {
    List(headSneToken, martId, shopId)
  }
}

/** Экземпляр события удаления рекламной карточки из БД. */
case class AdDeletedEvent(mmartAd: MMartAd) extends SioEventT {
  def getClassifier: Classifier = AdSavedEvent.getClassifier(
    martId = Option(mmartAd.receiverIds),
    shopId = mmartAd.producerId
  )
}