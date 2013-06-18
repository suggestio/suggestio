package models

import play.api.cache.Cache
import play.api.Play.current
import io.suggest.model.{DomainSettings, DomainSettingsStaticT}

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
  def getForDkey(dkey:String) : Option[DomainSettings] = {
    Cache.getOrElse(dkey + "/ds", 60)(getForDkeyNocache(dkey))
  }


  /**
   * Получить данные по домену напрямую из HDFS.
   * @param dkey
   * @return
   */
  def getForDkeyNocache(dkey:String) : Option[DomainSettings] = {
    DomainSettings.load(dkey)
  }

}
