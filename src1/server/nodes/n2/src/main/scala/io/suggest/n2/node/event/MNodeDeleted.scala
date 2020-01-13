package io.suggest.n2.node.event

import io.suggest.event.SioEventT
import io.suggest.event.SioNotifier.Classifier
import io.suggest.n2.node.INodeId

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 14:36
 * Description: Событие удаления узла рекламной сети. Это нечто очень редкое в продакшене.
 */
object MNodeDeleted {

  private def headSne = Some(getClass.getSimpleName)

  def getClassifier(nodeId    : Option[String]    = None,
                    isDeleted : Option[Boolean]   = None): Classifier = {
    List(headSne, nodeId, isDeleted)
  }

}


case class MNodeDeleted(nodeId: String, isDeleted: Boolean)
  extends SioEventT
  with INodeId
{

  def getClassifier: Classifier = {
    MNodeDeleted.getClassifier(
      nodeId    = Option(nodeId),
      isDeleted = Some(isDeleted)
    )
  }

}
