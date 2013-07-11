package io.suggest.model

import org.apache.hadoop.fs.{Path, FileSystem}
import io.suggest.util.JacksonWrapper
import io.suggest.util.SiobixFs._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.13 14:58
 * Description: Есть редко-изменяемые структурированные данные, которые удобно хранить в json.
 * Тут модель для сериализации/десериализации и хранения этих данных в hdfs.
 */

// Статическая часть модели
object JsonDfsBackend {

  type JsonMap_t = Map[String, Any]

  /**
   * Прочитать ранее сохраненное в json состояние из хранилища.
   * @param dkey ключ домена сайта
   * @param name имя состояния
   * @param fs DFS для записи. Нельзя вынести в implicit из-за T:Manifest-конструкции.
   * @tparam T тип для рефлексии, обязателен.
   * @return Опциональное нечто типа T
   */
  def getAs[T: Manifest](dkey:String, name:String, fs:FileSystem) : Option[T] = getAs[T](getPath(dkey, name), fs)
  def getAs[T: Manifest](path:String, fs:FileSystem) : Option[T] = getAs[T](new Path(path), fs)
  def getAs[T: Manifest](path:Path, fs:FileSystem) : Option[T] = {
    val path1 = ensurePathAbsolute(path)
    fs.exists(path1) match {
      case false => None

      case true =>
        val is = fs.open(path1)
        try {
          Some(JacksonWrapper.deserialize[T](is))
        } finally {
          is.close()
        }
    }
  }


  /**
   * Серилизовать в json и сохранить в hdfs.
   * @param dkey ключ домена сайта
   * @param name имя сохраняемого объекта
   * @param value и сам объект
   */
  def writeTo(dkey:String, name:String, value:Any)(implicit fs:FileSystem) {
    val path = getPath(dkey, name)
    writeTo(path, value)
  }

  def writeTo(path:String, value:Any)(implicit fs:FileSystem) {
    writeTo(new Path(path), value)
  }

  def writeTo(path:Path, value:Any)(implicit fs:FileSystem) {
    val os = fs.create(ensurePathAbsolute(path), true)
    try {
      JacksonWrapper.serializePretty(os, value)
    } finally {
      os.close()
    }
  }


  /**
   * Убедиться, что указанный путь является абсолютным. Если нет - то дописать в начало дефолтовый путь.
   * @param path путь.
   * @return 100% абсолютный путь.
   */
  protected def ensurePathAbsolute(path:Path) : Path = {
    path.isAbsolute match {
      case true  => path
      case false => new Path(siobix_out_path, path)
    }
  }


  /**
   * Функция генерации пути.
   * @param dkey ключ домена сайта
   * @param name имя сохраняемого состяния. Обычно имя класса.
   * @return Path, относительный по отношению к _root_fs (и любым другим fs)
   */
  def getPath(dkey:String, name:String) = new Path(dkeyPathConf(dkey), name + ".json")

}


trait JsonDfsClient {
  protected type ImportExportMap = Map[String,Any]
  protected def exportState : ImportExportMap
  protected def importStateElement(key:String, value:Any)

   /**
   * Импорт экспортированного состояния.
   * @param map
   */
  protected def importState(map: ImportExportMap) {
    map.foreach { case (k,v) => importStateElement(k,v) }
  }

  /**
   * Экспортировать состояние, сериализовать и отправить в DFS.
   */
  protected def saveState(implicit fs:FileSystem)

  /**
   * Восстановить ранее сохраненное состояние из DFS.
   */
  protected def loadState(implicit fs:FileSystem): Boolean

  protected def getClassName = getClass.getCanonicalName
  protected def getStateFileName = getClassName + ".json"
}


// Трайт для быстрой интеграции функций JsonDfsBackend в акторы и синглтоны, относящиеся к домену.
trait JsonDfsClientDkey extends JsonDfsClient {

  protected def dkey : String

  protected def saveState(implicit fs:FileSystem) {
    JsonDfsBackend.writeTo(dkey, getClassName, exportState)
  }

  protected def loadState(implicit fs:FileSystem) = {
    JsonDfsBackend.getAs[ImportExportMap](dkey, getStateFileName, fs) exists { data =>
      importState(data)
      true
    }
  }

}


// Трайт для быстрой интеграции JsonDfsClient в акторы и синглтоны, работающих вне всех доменов
// Это например супервизоры верхнего уровня, менеджеры индексов и т.д.
trait JsonDfsClientGlobal extends JsonDfsClient {

  protected def getStatePath = new Path(siobix_conf_path, getStateFileName)

  protected def saveState(implicit fs:FileSystem) {
    JsonDfsBackend.writeTo(getStatePath, exportState)
  }

  protected def loadState(implicit fs:FileSystem): Boolean = {
    JsonDfsBackend.getAs[ImportExportMap](getStatePath, fs) exists { data =>
      importState(data)
      true
    }
  }
}