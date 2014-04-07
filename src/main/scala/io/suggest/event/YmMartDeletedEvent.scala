package io.suggest.event

import io.suggest.event.SioNotifier.Classifier
import io.suggest.ym.model.MMart._
import scala.Some

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.14 14:07
 * Description: SN-событие удаления торгового центра.
 */
@deprecated("shop+mart arch deprecated. Use AdnNodeDeletedEvent", "2014.apr.07")
object YmMartDeletedEvent {

  def headSneToken: Option[String] = Some(getClass.getSimpleName)

  def getClassifier(martId: Option[MartId_t] = None): Classifier = List(headSneToken, martId)

}


@deprecated("shop+mart arch deprecated. Use AdnNodeDeletedEvent", "2014.apr.07")
case class YmMartDeletedEvent(martId: MartId_t) extends SioEventT {
  override def getClassifier: Classifier = YmMartDeletedEvent.getClassifier(martId = Some(martId))
}