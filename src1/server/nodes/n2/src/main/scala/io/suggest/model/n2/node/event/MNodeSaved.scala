package io.suggest.model.n2.node.event

import io.suggest.event.SioEventT
import io.suggest.event.SioNotifier.Classifier
import io.suggest.model.common.OptStrId
import io.suggest.model.n2.node.{MNodeType, INodeId, MNode}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.01.16 17:10
 * Description: Событие сохранения узла рекламной сети.
 * Используется как при создании, так и при обновлении узла.
 */
object MNodeSaved {

  private val headSne = Some(getClass.getSimpleName)

  def getClassifier(nodeId    : Option[String]    = None,
                    nodeType  : Option[MNodeType] = None,
                    isCreated : Option[Boolean]   = None): Classifier = {
    List(headSne, nodeId, nodeType, isCreated)
  }

}


case class MNodeSaved(mnode: MNode, isCreated: Boolean)
  extends SioEventT
  with INodeId
  with OptStrId
{

  def getClassifier: Classifier = {
    MNodeSaved.getClassifier(
      nodeId    = mnode.id,
      nodeType  = Some( mnode.common.ntype ),
      isCreated = Some( isCreated )
    )
  }

  override def id = mnode.id

  override def nodeId = mnode.id.get

}
