package io.suggest.event

import io.suggest.event.SioNotifier.Classifier
import io.suggest.ym.model.MMart._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.14 14:07
 * Description:
 */
@deprecated("shop+mart arch deprecated. Use AdnNodeSavedEvent", "2014.apr.07")
object YmMartAddedEvent {

  def headSneToken: Option[String] = Some(getClass.getSimpleName)

  def getClassifier(martId: Option[MartId_t] = None): Classifier = List(headSneToken, martId)

}


@deprecated("shop+mart arch deprecated. Use AdnNodeSavedEvent", "2014.apr.07")
case class YmMartAddedEvent(martId: MartId_t) extends SioEventT {
  override def getClassifier: Classifier = YmMartAddedEvent.getClassifier(martId = Option(martId))
}