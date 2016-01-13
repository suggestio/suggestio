package io.suggest.event

import io.suggest.event.SioNotifier.Classifier
import io.suggest.model.common.OptStrId
import io.suggest.model.n2.node.{MNode, MNodeType}

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

case class MNodeSavedEvent(mnode: MNode, isCreated: Boolean) extends SioEventT with INodeId with OptStrId {
  def getClassifier: Classifier = MNodeSavedEvent.getClassifier(
    nodeId    = mnode.id,
    nodeType  = Some( mnode.common.ntype ),
    isCreated = Some( isCreated )
  )

  override def id = mnode.id
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


