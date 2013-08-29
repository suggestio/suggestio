package io.suggest.util

import org.apache.hadoop.fs.{FileSystem, Path}
import com.scaleunlimited.cascading.hadoop.HadoopUtils

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
  def siobix_out_dir: String = siobixOutDirVal

  // Вместо val используется def+val из-за ограничений scala или vm: http://www.scala-lang.org/old/node/8139
  protected val siobixOutDirVal = System.getProperty("siobix.dfs.dir") match {
    case null       => "/home/user/projects/sio/2/bixo/dout"
    case str:String => str
  }


  val siobix_out_path  = new Path(siobix_out_dir)

  val siobix_conf_path = new Path(siobix_out_path, SioConstants.CONF_SUBDIR)
  val dkeysConfRoot    = new Path(siobix_conf_path, "dkey")
  val crawlRoot        = new Path(siobix_out_path,  "crawl")
  val imgRoot          = new Path(siobix_out_path,  "img")
  val thumbsRoot       = new Path(imgRoot,          SioConstants.THUMBS_SUBDIR)

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
