package io.suggest.event

import io.suggest.ym.model.MAd
import io.suggest.event.SioNotifier.Classifier

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.03.14 11:51
 * Description:
 */
object AdSavedEvent {

  val headSneToken: Option[String] = Some(getClass.getSimpleName)

  def getClassifier(producerId: Option[String] = None, id: Option[String] = None): Classifier = {
    List(headSneToken, producerId, id)
  }

}

case class AdSavedEvent(mad: MAd) extends SioEventT {
  def getClassifier: Classifier = AdSavedEvent.getClassifier(
    producerId = Option(mad.producerId),
    id = mad.id
  )
}



/** Событие удаления рекламной карточки из БД. */
object AdDeletedEvent {

  val headSneToken: Option[String] = Some(getClass.getSimpleName)

  def getClassifier(producerId: Option[String] = None, id: Option[String] = None): Classifier = {
    List(headSneToken, producerId, id)
  }
}

/** Экземпляр события удаления рекламной карточки из БД. */
case class AdDeletedEvent(mad: MAd) extends SioEventT {
  def getClassifier: Classifier = AdSavedEvent.getClassifier(
    producerId = Option(mad.producerId),
    id = mad.id
  )
}