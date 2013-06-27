package models

import util.{DkeyContainerT, SiobixFs}
import SiobixFs.fs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.13 15:50
 * Description: Модель доменов. Frontend-модель для доступа к различным данным доменов через другие модели.
 */

case class MDomain(
  dkey : String
) extends DkeyContainerT {

  /**
   * Выдать настройки для домена.
   * @return
   */
  def domainSettings = MDomainSettings.getForDkey(dkey).get
  def authzForPerson(person_id:String) = MPersonDomainAuthz.getForPersonDkey(dkey, person_id)

}


// Статическая часть модели живёт здесь.
object MDomain {

  /**
   * Прочитать для dkey. Если нет такого домена, то будет None.
   * @param dkey ключ домена.
   * @return Соответсвующий MDomain, если найден.
   */
  def getForDkey(dkey:String) : Option[MDomain] = {
    val dkeyPath = SiobixFs.dkeyPath(dkey)
    fs.exists(dkeyPath) match {
      case true  => Some(MDomain(dkey=dkey))
      case false => None
    }
  }

}

