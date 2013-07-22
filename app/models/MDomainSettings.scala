package models

import play.api.cache.Cache
import play.api.Play.current
import io.suggest.model.{DomainSettingsT, DomainSettings, DomainSettingsStaticT}
import util.SiobixFs.fs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.13 16:33
 * Description: Враппер для sioutil-модели работы с DomainSettings.
 */

object MDomainSettings extends DomainSettingsStaticT {

  /**
   * С помощью кеша и HDFS получить данные по домену.
   * @param dkey
   * @return
   */
  override def getForDkey(dkey:String) : Option[DomainSettingsT] = {
    Cache.getOrElse(dkey + "/ds", 60)(super.getForDkey(dkey))
  }


  /**
   * Получить данные по домену напрямую из HDFS.
   * @param dkey
   * @return
   */
  def getForDkeyNocache(dkey:String) : Option[DomainSettingsT] = {
    DomainSettings.getForDkey(dkey)
  }

}
