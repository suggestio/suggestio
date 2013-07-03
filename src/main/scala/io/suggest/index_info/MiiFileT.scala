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


trait MiiFileT extends Serializable {

  @JsonIgnore def dkey: String
  @JsonIgnore def prefix: String
  @JsonIgnore def name: String

  @JsonIgnore def filename: String = prefix + name
  @JsonIgnore lazy val filepath = MIndexInfo.getFilePath(dkey, filename)

  @JsonIgnore def save: MiiFileT
}


// Статические константы из MiiFileWithIiT вынесены сюда.
object MiiFileWithIi extends Serializable {
  type JsonMap_t = JsonDfsBackend.JsonMap_t

  val KEY_DKEY   = "dkey"
  val KEY_IITYPE = "iitype"
  val KEY_IINFO  = "iinfo"
}


/**
 * Файл, содержащий IndexInfo. Такой файл надо бы сохранять по-особому, ибо IndexInfo может быть разнотипным.
 */
trait MiiFileWithIiT[T <: MiiFileWithIiT[T]] extends MiiFileT {

  import MiiFileWithIi._

  val indexInfo: IndexInfo

  def dkey = indexInfo.dkey
  def name = indexInfo.name

  /**
   * Сохранить текущее добро в базу.
   * @return Сохраненный экземпляр.
   */
  // TODO это сохранение неполиморфно. Нужно тут сделать, чтобы тип сохранялся вне метаданных IndexInfo.
  // TODO нужен трейт для load-части, чтобы подмешивать его в соотв. объекты.
  def save: T = {
    JsonDfsBackend.writeTo(path=filepath, value=export)
    this.asInstanceOf[T]
  }


  /**
   * Экспорт состояния текущего экземпляра.
   * @return Карту, пригодную для сериализации в json.
   */
  protected def export: JsonMap_t = Map(
    KEY_DKEY   -> dkey,
    KEY_IITYPE -> indexInfo.iitype,
    KEY_IINFO  -> indexInfo.export
  )
}


/**
 * Для fs.listStatus() бывает необходимо фильтровать. Для mii - по префиксу.
 */
trait MiiPathFilterT extends Serializable {
  def prefix: String
  val prefixCh = prefix.charAt(0)

  val pathFilter = new PathFilter with Serializable {
    def accept(path: Path): Boolean = {
      path.getName.startsWith(prefix)
    }
  }
}


// Статическая часть для object'ов, обслуживающих классы MiiFileWithIiT. Тут функции десериализации полиморфных Mii инстансов.
// Суть в том, чтобы избавиться от одинакового кода между MiiActive и MiiAdd.
trait MiiFileWithIiStaticT[T] extends MiiPathFilterT {
  import MiiFileWithIi._

  /**
   * Прочитать данные из хранилища. Экспортируемая из объекта функция, выдает результат нужного типа.
   * @param dkey Ключ домена.
   * @return Опциональный сабж.
   */
  def getForDkey(dkey:String) : List[T] = {
    val dkeyPath = MIndexInfo.getDkeyPath(dkey)
    fs.listStatus(dkeyPath, pathFilter)
      .toList
      .foldLeft[List[T]] (Nil) { (acc, st) => readThisAcc(acc, st.getPath) }
  }


  /**
   * Интерфейсная фунцкия, отвечающая за чтение данных из уже известных файлов.
   * @param l список путей, которые надо прочитать.
   * @param acc опционально: исходный аккамулятор. По дефолту - пустой список.
   * @return Список сабжей. При ошибках чтения/парсинга, длина списка результатов будет меньше чем l.size.
   */
  def readThese(l: Seq[Path], acc:List[T] = Nil) : List[T] = {
    l.foldLeft[List[T]] (acc) { readThisAcc }
  }


  /**
   * Прочитать данные из указанного файла, используя аккамулятор для сбора результатов.
   * @param acc Старый аккамулятор предыдущих результатов.
   * @param path Путь к файлу (не проверяется никак).
   * @return Новый аккамулятор.
   */
  def readThisAcc(acc:List[T], path:Path): List[T] = {
    JsonDfsBackend.getAs[JsonMap_t](path=path, fs=fs) match {
      // Всё на месте. Нужно теперь оттранслировать IndexInfo из карты в экземпляр соответствующего типу класса.
      case Some(m) =>
        val dkey   = m(KEY_DKEY).toString
        val iitype = m(KEY_IITYPE).toString
        val iinfo  = m(KEY_IINFO).asInstanceOf[IndexInfoStatic.AnyJsonMap]
        val iinfo1 = IndexInfoStatic(dkey, iitype, iinfo)
        val result = toResult(dkey, iinfo1, m)
        result :: acc

      // Внезапно, найденный файл не осилили прочесть.
      case None => acc
    }
  }


  /**
   * Костыль для чтения из карты данных с приведением типа.
   * @param key ключ в карте (см. KEY_*)
   * @param m карта
   * @param default дефолтовое значение
   * @tparam V тип возвращаемого значения и дефолтового значения.
   * @return Значение указанного типа из карты или дефолт.
   */
  protected def getFromMap[V](key:String, m:JsonMap_t, default:V): V = {
    m.get(key)
      .map(_.asInstanceOf[V])
      .getOrElse(default)
  }


  /**
   * Фунция создания объекта на стороне реализатора этого трейта.
   * @param dkey ключ домена.
   * @param iinfo Распарсенная инфа по индексу.
   * @param m Представлеи JSON'а, который сохранен
   * @return Экземпляр T.
   */
  protected def toResult(dkey:String, iinfo:IndexInfo, m:JsonMap_t): T

}

