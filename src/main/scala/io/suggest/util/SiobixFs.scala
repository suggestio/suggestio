package io.suggest.util

import org.apache.hadoop.fs.{FileSystem, Path}
import com.scaleunlimited.cascading.hadoop.HadoopUtils
import io.suggest.index_info.SioEsConstants

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.04.13 18:33
 * Description: работа с метаданными siobix, которые лежат в dfs.
 */

object SiobixFs extends SiobixFsStaticT

trait SiobixFsStaticT {

  /**
   * Корневая директория данных. Все остальные пути зависят от неё.
   */
  val siobix_out_dir: String = System.getProperty("siobix.dfs.dir") match {
    case null       => "/home/user/projects/sio/2/bixo/dout"
    case str:String => str
  }

  val siobix_out_path  = new Path(siobix_out_dir)

  val siobix_conf_path = new Path(siobix_out_path, SioEsConstants.CONF_SUBDIR)
  val dkeysConfRoot    = new Path(siobix_conf_path, "dkey")
  val crawlRoot        = new Path(siobix_out_path,  "crawl")

  implicit val fs: FileSystem = {
    val conf = HadoopUtils.getDefaultJobConf
    siobix_out_path.getFileSystem(conf)
  }

  /**
   * Выдать путь до директории с конфигами для указанного dkey.
   * @param dkey ключ домена.
   * @return Path(/dout/conf/dkey)
   * TODO нужно переделать название функции.
   */
  def dkeyPathConf(dkey:String) = new Path(dkeysConfRoot, dkey)

}
