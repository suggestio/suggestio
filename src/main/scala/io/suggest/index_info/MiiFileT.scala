package io.suggest.index_info

import io.suggest.model.{JsonDfsBackend, MIndexInfo}
import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.hadoop.fs.{Path, PathFilter}
import io.suggest.util.SiobixFs.fs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.13 17:22
 * Description: Набор трейтов для нужд MII.
 */


trait MiiFileT {
  import MIndexInfo._

  @JsonIgnore def dkey: String
  @JsonIgnore def prefix: String
  @JsonIgnore def name: String

  @JsonIgnore def filename: String = prefix + name
  @JsonIgnore lazy val filepath = getFilePath(dkey, filename)

  @JsonIgnore def save: MiiFileT
}


/**
 * Файл, содержащий IndexInfo. Такой файл надо бы сохранять по-особому, ибо IndexInfo может быть разнотипным.
 */
trait MiiFileWithIiT extends MiiFileT {
  val indexInfo: IndexInfo

  def dkey = indexInfo.dkey
  def name = indexInfo.name

  /**
   * Сохранить текущее добро в базу.
   * @return Сохраненный экземпляр.
   */
  // TODO это сохранение неполиморфно. Нужно тут сделать, чтобы тип сохранялся вне метаданных IndexInfo.
  // TODO нужен трейт для load-части, чтобы подмешивать его в соотв. объекты.
  def save = {
    JsonDfsBackend.writeTo(path=filepath, value=this)
    this
  }
}


/**
 * Для fs.listStatus() бывает необходимо фильтровать. Для mii - по префиксу.
 */
trait MiiPathFilter extends Serializable {
  def prefix: String

  val pathFilter = new PathFilter with Serializable {
    def accept(path: Path): Boolean = {
      path.getName.startsWith(prefix)
    }
  }
}
