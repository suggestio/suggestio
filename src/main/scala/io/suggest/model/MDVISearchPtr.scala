package io.suggest.model

import org.apache.hadoop.fs.{FileSystem, Path}
import io.suggest.index_info.MDVIUnit

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.07.13 10:27
 * Description: Активных индексов может быть несколько, и они могут иметь различные имена.
 * Однако веб-морде нужно быстро узнавать в каком индексе нужно производить поиск.
 * Тут - модель для задания указателей на активные индексы. Что-то типа алиасов ES, но описывают более специфичные
 * и необходимые для suggest.io вещи.
 *
 * Содержимое файла указатель имя (или имена разделенные \n) active-индексов, на которые указывает этот указатель.
 */

object MDVISearchPtr {

  val searchSubdirNamePath = new Path("search")
  val readBuffSize = 128

  /**
   * Выдать путь до поддиректории /search, хранящей файлы с search-указателями для указанного домена.
   * @param dkey ключ домена
   * @return Path.
   */
  def getDkeySearchDirPath(dkey:String) = new Path(MDVIUnit.getDkeyPath(dkey), searchSubdirNamePath)

  /**
   * Выдать путь до файла search-указателя.
   * @param dkey ключ домена
   * @param id идентификатор
   * @return Path
   */
  def getDkeySearchPath(dkey:String, id:String) = new Path(getDkeySearchDirPath(dkey), id)

  /**
   * Прочитать указатель для dkey и id.
   * @param dkey Ключ домена.
   * @param id Идентификатор.
   * @return Опциональный распрарсенный экземпляр MDVISearchPtr.
   */
  def getForDkeyId(dkey:String, id:String)(implicit fs:FileSystem): Option[MDVISearchPtr] = {
    val path = getDkeySearchPath(dkey, id)
    JsonDfsBackend.getAs[MDVISearchPtr](path, fs)
  }

}


import MDVISearchPtr._

case class MDVISearchPtr(
  dkey:  String,
  id:    String,
  mdvis: List[String]
) {

  /**
   * Сохранить в DFS.
   * @return Экземпляр сохраненного сабжа. Т.е. текущий экземпляр.
   */
  def save(implicit fs:FileSystem): MDVISearchPtr = {
    val path = getDkeySearchPath(dkey, id)
    JsonDfsBackend.writeTo(path, this, overwrite=true)
    this
  }

}