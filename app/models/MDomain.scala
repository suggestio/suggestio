package models

import play.api.cache.Cache
import play.api.Play.current
import io.suggest.model.DomainSettings
import util.SiobixFs
import SiobixFs.fs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.13 15:50
 * Description: Модель доменов. Frontend-модель для доступа к различным данным доменов через другие модели.
 */

case class MDomain(
  dkey : String
) {

  /**
   * Выдать настройки для домена.
   * @return
   */
  def domainSettings = MDomain.getSettingsForDkeyCache(dkey).get
  def authzForPerson(person_id:String) = MPersonDomainAuthz.getForPersonDkey(dkey, person_id)

}


// Статическая часть модели живёт здесь.
object MDomain {

  /**
   * С помощью кеша и HDFS получить данные по домену.
   * @param dkey
   * @return
   */
  def getSettingsForDkeyCache(dkey:String) : Option[DomainSettings] = {
    Cache.getOrElse(dkey + "/ds", 60)(getSettingForDkey(dkey))
  }


  /**
   * Получить данные по домену напрямую из HDFS.
   * @param dkey
   * @return
   */
  def getSettingForDkey(dkey:String) : Option[DomainSettings] = {
    DomainSettings.load(dkey)
  }


  /**
   * Прочитать для dkey. Если нет такого домена, то будет None.
   * @param dkey
   * @return
   */
  def getForDkey(dkey:String) : Option[MDomain] = {
    val dkeyPath = SiobixFs.dkeyPath(dkey)
    fs.exists(dkeyPath) match {
      case true  => Some(MDomain(dkey=dkey))
      case false => None
    }
  }

}

