package io.suggest.event

import io.suggest.event.SioNotifier.Classifier
import io.suggest.ym.model.MAdnNode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 14:36
 * Description: Этот исходник содержит события, относящиеся к узлам рекламной сети
 * [[io.suggest.ym.model.MAdnNode]]. Как правило, все перечисленные события порождаются моделью узла сети.
 */

/** Событие сохранения узла рекламной сети. Используется как при создании, так и при обновлении узла. */
object AdnNodeSavedEvent {
  val headSne = Some(getClass.getSimpleName)

  def getClassifier(adnId: Option[String] = None, isCreated: Option[Boolean] = None): Classifier = {
    List(headSne, adnId, isCreated)
  }
}

case class AdnNodeSavedEvent(adnId: String, adnNode: MAdnNode, isCreated: Boolean) extends SioEventT {
  def getClassifier: Classifier = AdnNodeSavedEvent.getClassifier(
    adnId = Some(adnId),
    isCreated = Some(isCreated)
  )
}



/** Событие удаления узла рекламной сети. Это нечто очень редкое в продакшене. */
object AdnNodeDeletedEvent {
  val headSne = Some(getClass.getSimpleName)

  def getClassifier(adnId: Option[String] = None, isDeleted: Option[Boolean] = None): Classifier = {
    List(headSne, adnId)
  }
}

case class AdnNodeDeletedEvent(adnId: String, isDeleted: Boolean) extends SioEventT {
  def getClassifier: Classifier = {
    AdnNodeDeletedEvent.getClassifier(Some(adnId), Some(isDeleted))
  }
}
