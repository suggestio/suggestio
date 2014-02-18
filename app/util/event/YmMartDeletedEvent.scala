package util.event

import io.suggest.event.SioEventT
import io.suggest.event.SioNotifier.Classifier

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.14 14:07
 * Description: SN-событие удаления торгового центра.
 */
object YmMartDeletedEvent {

  def headSneToken: Option[String] = Some(getClass.getSimpleName)

  def getClassifier(martId: Option[Int] = None): Classifier = List(headSneToken, martId)

}


case class YmMartDeletedEvent(martId: Int) extends SioEventT {
  override def getClassifier: Classifier = YmMartDeletedEvent.getClassifier(martId = Some(martId))
}