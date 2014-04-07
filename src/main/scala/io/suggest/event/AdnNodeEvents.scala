package io.suggest.event

import io.suggest.event.SioNotifier.Classifier
import io.suggest.ym.model.{AdNetMemberType, MAdnNode}

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

  def getClassifier(memberType: Option[AdNetMemberType] = None,
                    adnId: Option[String] = None,
                    isCreated: Option[Boolean] = None): Classifier = {
    List(headSne, memberType, adnId, isCreated)
  }
}

case class AdnNodeSavedEvent(adnId: String, adnNode: MAdnNode, isCreated: Boolean) extends SioEventT {
  def getClassifier: Classifier = AdnNodeSavedEvent.getClassifier(
    memberType = Option(adnNode.adnMemberInfo.memberType),
    adnId = Option(adnId),
    isCreated = Some(isCreated)
  )
}



/** Событие удаления узла рекламной сети. Это нечто очень редкое в продакшене. */
object AdnNodeDeletedEvent {
  val headSne = Some(getClass.getSimpleName)

  def getClassifier(adnId: Option[String] = None,
                    isDeleted: Option[Boolean] = None): Classifier = {
    List(headSne, adnId, isDeleted)
  }
}

case class AdnNodeDeletedEvent(adnId: String, isDeleted: Boolean) extends SioEventT {
  def getClassifier: Classifier = {
    AdnNodeDeletedEvent.getClassifier(
      adnId = Option(adnId),
      isDeleted = Some(isDeleted)
    )
  }
}


/** Событие включения выключения узла рекламной сети. */
object AdnNodeOnOffEvent {
  val haadSne = Some(getClass.getSimpleName)

  def getClassifier(memberType: Option[AdNetMemberType] = None,
                    adnId: Option[String] = None,
                    isEnabled: Option[Boolean] = None): Classifier = {
    List(haadSne, memberType, adnId, isEnabled)
  }
}

case class AdnNodeOnOffEvent(adn: MAdnNode) extends SioEventT {
  def getClassifier: Classifier = AdnNodeOnOffEvent.getClassifier(
    memberType = Option(adn.adnMemberInfo.memberType),
    adnId = adn.id,
    isEnabled = Some(adn.pubSettings.isEnabled)
  )
}

