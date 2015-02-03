package models.adv

import io.suggest.event.SioNotifier.{Classifier, Event}
import models.{MAdvI, MAdvMode}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.01.15 18:22
 * Description: Классы событий для нужд размещения.
 */

object AdvSavedEvent {
  def getClassifier(mode: Option[MAdvMode] = None,
                    isCreated: Option[Boolean] = None,
                    prodAdnId: Option[String] = None,
                    rcvrAdnId: Option[String] = None,
                    madId: Option[String] = None): Classifier = {
    val prefix = Some(classOf[AdvSavedEvent].getSimpleName)
    List(prefix, mode, isCreated, prodAdnId, rcvrAdnId, madId)
  }
}

/** Событие сохранения экземпляра-реализации [[models.MAdvI]]. */
case class AdvSavedEvent(adv: MAdvI, isCreated: Boolean) extends Event {
  override def getClassifier: Classifier = {
    AdvSavedEvent.getClassifier(
      mode      = Some(adv.mode),
      isCreated = Some(isCreated),
      prodAdnId = Some(adv.prodAdnId),
      rcvrAdnId = Some(adv.rcvrAdnId),
      madId     = Some(adv.adId)
    )
  }
}
