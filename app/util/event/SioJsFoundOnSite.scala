package util.event

import io.suggest.event.SioEventT
import io.suggest.event.SioNotifier.Classifier
import util.acl.PersonWrapper.PwOpt_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.10.13 11:10
 * Description: События из семейства "на каком-то сайте найден установленный sio.js". Обычно подается в шину одновременно
 * с qi-событиями. Вот только эти события -- для внутреннего пользования, а qi-события -- для обратной связи с юзером.
 */

object SioJsFoundOnSite {
  val headSneToken = Some("sioJsFoundOnSite")

  def getClassifier(dkeyOpt: Option[String] = None, isValidOpt:Option[Boolean] = None): Classifier = {
    List(headSneToken, dkeyOpt, isValidOpt)
  }
}


abstract class SioJsFoundOnSite extends SioEventT {
  def dkey: String
  def isValid: Boolean
  def getClassifier: Classifier = SioJsFoundOnSite.getClassifier(dkeyOpt = Some(dkey),  isValidOpt = Some(isValid))
  def addedBy: PwOpt_t
}


case class ValidSioJsFoundOnSite(dkey: String, addedBy: PwOpt_t) extends SioJsFoundOnSite {
  def isValid: Boolean = true
}
