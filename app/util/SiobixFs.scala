package util

import play.api.Play.current
import io.suggest.model.JsonDfsBackend
import org.apache.hadoop.fs.Path
import com.scaleunlimited.cascading.hadoop.HadoopUtils
import io.suggest.index_info.SioEsConstants

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.04.13 18:33
 * Description: работа с метаданными siobix, которые лежат в dfs.
 */

object SiobixFs {

  val siobix_out_dir  = current.configuration.getString("siobix.dfs.dir").getOrElse("/home/user/projects/bixo-git/dout")
  val siobix_out_path = new Path(siobix_out_dir)
  val siobix_conf_path = new Path(siobix_out_path, SioEsConstants.CONF_SUBDIR)
  val conf = HadoopUtils.getDefaultJobConf
  implicit val fs = siobix_out_path.getFileSystem(conf)

  JsonDfsBackend.setOutDir(siobix_out_path)

  def dkeyPath(dkey:String) = new Path(siobix_out_path, dkey)
  def dkeyPathConf(dkey:String) = new Path(dkeyPath(dkey), SioEsConstants.CONF_SUBDIR)

}
