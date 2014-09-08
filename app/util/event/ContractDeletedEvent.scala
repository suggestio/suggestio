package util.event

import io.suggest.event.SioNotifier.Classifier
import io.suggest.event.SioEventT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.14 17:01
 * Description: Событие удаления контракта.
 */
object ContractDeletedEvent {
  def headSne = Some(getClass.getSimpleName)

  def getClassifier(contractId: Option[Int] = None): Classifier = {
    List(headSne, contractId)
  }
}

case class ContractDeletedEvent(contractId: Int) extends SioEventT {
  override def getClassifier: Classifier = {
    ContractDeletedEvent.getClassifier(contractId = Some(contractId))
  }
}
