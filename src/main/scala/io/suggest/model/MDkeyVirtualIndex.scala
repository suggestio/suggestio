package io.suggest.model

import org.apache.hadoop.fs.Path
import io.suggest.util.SiobixFs.{dkeyPathConf, fs}
import io.suggest.util.Logs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.13 14:30
 * Description: Сабж хранит инфу о связях текущего dkey с виртуальными индексами, а также о необходимости произведения
 * действий над ними (batch). Внутренне, хранилище данных об dkey-индексе устроено в виде директории с файлами:
 * /vasya.com/indexing/active/...
 */

object MDkeyVirtualIndex extends Logs with Serializable {

  // Имя поддиректории модели в папке $dkey. Используется как основа для всего остального в этой модели.
  val rootDirNamePath      = new Path("indexing")
  val activeSubdirNamePath = new Path("active")
  val activeDirRelPath     = new Path(rootDirNamePath, activeSubdirNamePath)
  val searchPtrNamePath    = new Path("search")
  val batchDirNamePath     = new Path("batch")

  /**
   * Выдать путь к SEARCH-указателю.
   * @param dkey Ключ домена.
   * @return Путь.
   */
  def getDkeySearchPtrPath(dkey:String) = new Path(dkeyPathConf(dkey), searchPtrNamePath)

}

