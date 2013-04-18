package util

import play.api.Play.current
import play.api.cache.Cache
import io.suggest.model.{JsonDfsBackend, DomainSettings}
import org.apache.hadoop.fs.Path
import com.scaleunlimited.cascading.hadoop.HadoopUtils

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.04.13 18:33
 * Description: работа с метаданными siobix, которые лежат в dfs.
 */

object SiobixFs {

  val siobix_out_dir  = current.configuration.getString("siobix.dfs.dir").getOrElse("/home/user/projects/bixo-git/dout")
  val siobix_out_path = new Path(siobix_out_dir)
  val conf = HadoopUtils.getDefaultJobConf
  implicit val fs = siobix_out_path.getFileSystem(conf)

  JsonDfsBackend.setOutDir(siobix_out_path)

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

}
