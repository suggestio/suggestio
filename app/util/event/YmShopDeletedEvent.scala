package util.event

import io.suggest.event.SioEventT
import io.suggest.event.SioNotifier.Classifier

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.14 15:33
 * Description: Событие удаление магазина из торгового центра.
 */
object YmShopDeletedEvent {

  def headSneToken: Option[String] = Some(getClass.getSimpleName)

  def getClassifier(martId: Option[Int] = None, shopId: Option[Int] = None): Classifier = {
    List(headSneToken, martId, shopId)
  }

}


case class YmShopDeletedEvent(martId: Int, shopId: Int) extends SioEventT {
  override def getClassifier: Classifier = {
    YmShopDeletedEvent.getClassifier(
      martId = Some(martId),
      shopId = Some(shopId)
    )
  }
}
