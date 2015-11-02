package io.suggest.event

import com.google.inject.Inject
import io.suggest.event.SioNotifier.{Subscriber, Classifier}
import io.suggest.model.es.EsModelStaticT
import io.suggest.model.n2.node.{MNodeType, MNode}
import io.suggest.ym.model.{MWelcomeAd, MAd}
import io.suggest.event.subscriber.SnFunSubscriber
import io.suggest.util.MacroLogsImpl
import scala.util.{Failure, Try, Success}
import scala.concurrent.ExecutionContext
import org.elasticsearch.client.Client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 14:36
 * Description: Этот исходник содержит события, относящиеся к узлам рекламной сети MNode.
 * Как правило, все перечисленные события порождаются моделью узла сети.
 */

/** Событие сохранения узла рекламной сети. Используется как при создании, так и при обновлении узла. */
object MNodeSavedEvent {
  val headSne = Some(getClass.getSimpleName)

  def getClassifier(nodeId    : Option[String]    = None,
                    nodeType  : Option[MNodeType] = None,
                    isCreated : Option[Boolean]   = None): Classifier = {
    List(headSne, nodeId, nodeType, isCreated)
  }
}

case class MNodeSavedEvent(mnode: MNode, isCreated: Boolean) extends SioEventT with INodeId {
  def getClassifier: Classifier = MNodeSavedEvent.getClassifier(
    nodeId    = mnode.id,
    nodeType  = Some( mnode.common.ntype ),
    isCreated = Some( isCreated )
  )

  override def nodeId = mnode.id.get
}



/** Событие удаления узла рекламной сети. Это нечто очень редкое в продакшене. */
object MNodeDeletedEvent {
  val headSne = Some(getClass.getSimpleName)

  def getClassifier(nodeId    : Option[String]    = None,
                    isDeleted : Option[Boolean]   = None): Classifier = {
    List(headSne, nodeId, isDeleted)
  }
}

case class MNodeDeletedEvent(nodeId: String, isDeleted: Boolean)
  extends SioEventT
  with INodeId
{
  def getClassifier: Classifier = {
    MNodeDeletedEvent.getClassifier(
      nodeId    = Option(nodeId),
      isDeleted = Some(isDeleted)
    )
  }

}


/** Все MNode-события имеют adnId. Тут интерфейс, описывающий это: */
trait INodeId {
  def nodeId: String
}


// TODO 2015.oct.26: Когда будет реализована чистка по MNode.common.isDependent, этот код можно будет упростить/удалить.

/** Если нужно удалять рекламные карточки, созданные каким-то узлом при удалении оного, то можно
  * можно использовать этот subscriber. */
class DeleteAdsOnAdnNodeDeleteSubscriber @Inject() (
  implicit val ec: ExecutionContext,
  implicit val client: Client,
  implicit val sn: SioNotifierStaticClientI
)
  extends SNStaticSubscriber with MacroLogsImpl
{
  import LOGGER._

  /** Карта подписчиков вместе с содержимым подписчика. */
  override def snMap: List[(Classifier, Seq[Subscriber])] = {
    val classifier = MNodeDeletedEvent.getClassifier()
    val sub = SnFunSubscriber {
      case ande: MNodeDeletedEvent =>
        val producerId = ande.nodeId
        val logPrefix = s"event(prodId=$producerId): "
        debug(logPrefix + "Starting deletion of all ads, related to producer...")
        MAd.deleteByProducerId1by1(producerId)
          .onComplete( handleFinishPf(logPrefix, MAd) )
        MWelcomeAd.deleteByProducerId1by1(producerId)
          .onComplete( handleFinishPf(logPrefix, MWelcomeAd) )

      case other =>
        warn("Unexpected event received: " + other)
    }
    List(
      classifier -> Seq(sub)
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

