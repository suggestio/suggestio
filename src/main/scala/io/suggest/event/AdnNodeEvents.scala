package io.suggest.event

import io.suggest.event.SioNotifier.{Subscriber, Classifier}
import io.suggest.ym.model.{MWelcomeAd, MAd, AdNetMemberType, MAdnNode}
import io.suggest.event.subscriber.SnFunSubscriber
import io.suggest.util.MacroLogsImpl
import io.suggest.model.EsModelStaticT
import scala.util.{Failure, Try, Success}
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client

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

case class AdnNodeSavedEvent(adnId: String, adnNode: MAdnNode, isCreated: Boolean) extends SioEventT with IAdnId {
  def getClassifier: Classifier = AdnNodeSavedEvent.getClassifier(
    memberType = Option(adnNode.adn.memberType),
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

case class AdnNodeDeletedEvent(adnId: String, isDeleted: Boolean) extends SioEventT with IAdnId {
  def getClassifier: Classifier = {
    AdnNodeDeletedEvent.getClassifier(
      adnId = Option(adnId),
      isDeleted = Some(isDeleted)
    )
  }
}


/** Событие включения выключения узла рекламной сети. */
object AdnNodeOnOffEvent {
  val headSne = Some(getClass.getSimpleName)

  def getClassifier(adnId: Option[String] = None,
                    isEnabled: Option[Boolean] = None): Classifier = {
    List(headSne, adnId, isEnabled)
  }
}

case class AdnNodeOnOffEvent(adnId: String, isEnabled: Boolean) extends SioEventT with IAdnId {
  def getClassifier: Classifier = AdnNodeOnOffEvent.getClassifier(
    adnId = Option(adnId),
    isEnabled = Some(isEnabled)
  )
}


/** Все AdnNode-события имеют adnId. Тут интерфейс, описывающий это: */
trait IAdnId {
  def adnId: String
}


/** Если нужно удалять рекламные карточки, созданные каким-то узлом при удалении оного, то можно
  * можно использовать этот subscriber. */
object DeleteAdsOnAdnNodeDeleteSubscriber extends MacroLogsImpl {
  import LOGGER._

  /** Карта подписчиков вместе с содержимым подписчика. */
  def getSnMap(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Seq[(Classifier, Seq[Subscriber])] = {
    Seq(
      AdnNodeDeletedEvent.getClassifier() -> Seq(SnFunSubscriber {
        case ande: AdnNodeDeletedEvent =>
          val producerId = ande.adnId
          val logPrefix = s"event(prodId=$producerId): "
          debug(logPrefix + "Starting deletion of all ads, related to producer...")
          MAd.deleteByProducerId1by1(producerId) onComplete handleFinishPf(logPrefix, MAd)
          MWelcomeAd.deleteByProducerId1by1(producerId) onComplete handleFinishPf(logPrefix, MWelcomeAd)

        case other =>
          warn("Unexpected event received: " + other)
      })
    )
  }

  /** Генератор complete-функции подхвата завершения удаления рекламных карточек.
    * Функция только сообщает в логи о своих успехах. */
  private def handleFinishPf(logPrefix: String, model: EsModelStaticT): PartialFunction[Try[_], _] = {
    case Success(result) =>
      debug(logPrefix + "All ads removed ok from model " + model.getClass.getSimpleName + " ;; result = " + result)
    case Failure(ex) =>
      error(logPrefix + "Failed to rm ads from model " + model.getClass.getSimpleName, ex)
  }
}

