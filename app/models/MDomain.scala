package models

import util.{DkeyModelT, SiobixFs}
import SiobixFs.fs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.13 15:50
 * Description: Модель доменов. Frontend-модель для доступа к различным данным доменов через другие модели.
 */

case class MDomain(dkey : String) extends DkeyModelT {
  override def domainOpt: Option[MDomain] = Some(this)
  override def domain: MDomain = this
}


// Статическая часть модели живёт здесь.
object MDomain {

  /**
   * Прочитать для dkey. Если нет такого домена, то будет None.
   * @param dkey ключ домена.
   * @return Соответсвующий MDomain, если найден.
   */
  def getForDkey(dkey:String) : Option[MDomain] = {
    val dkeyPath = SiobixFs.dkeyPathConf(dkey)
    fs.exists(dkeyPath) match {
      case true  => Some(MDomain(dkey=dkey))
      case false => None
    }
  }

}

