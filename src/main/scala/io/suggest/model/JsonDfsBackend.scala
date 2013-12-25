package io.suggest.model

import org.apache.hadoop.fs.{Path, FileSystem}
import io.suggest.util._
import io.suggest.util.SiobixFs._
import scala.concurrent.{ExecutionContext, Await, Future, future}
import scala.concurrent.duration._
import scala.Some
import scala.util.Success

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

  // writeToDkeyName(): Перезаписывать ли по умолчанию, если overwrite не задан?
  val OVERWRITE_DFLT = true

  // writeToDkeyName(): Печатать красиво по умолчанию?
  // Да, т.к. на время отладки нужен быстрый и понятный доступ к файлам для быстрого исправления проблем.
  val PRETTY_DFLT = true

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
   * @param dkey Ключ домена сайта
   * @param name Имя сохраняемого объекта
   * @param value Сам объект, подлежащий сериализации.
   * @param overwrite Перезаписывать имеющийся файл?
   * @param pretty Красиво печатать? Или же одной строчкой.
   */
  def writeToDkeyName(dkey:String, name:String, value:Any, overwrite:Boolean = OVERWRITE_DFLT, pretty:Boolean = PRETTY_DFLT)(implicit fs:FileSystem) {
    val path = getPath(dkey, name)
    writeToPath(path, value, overwrite, pretty)
  }


  /**
   * Серилизовать произвольные данные в указанный путь.
   * @param path Путь к файлу, который может не существовать.
   * @param value Данные.
   * @param overwrite Перезаписывать? По дефолту - OVERWRITE_DFLT.
   * @param pretty Писать красиво? Если false, то одной строчкой. По дефолту = PRETTY_DFLT.
   * @param fs Файловая система.
   */
  def writeToPath(path:Path, value:Any, overwrite:Boolean = OVERWRITE_DFLT, pretty:Boolean = PRETTY_DFLT)(implicit fs:FileSystem) {
    val os = fs.create(ensurePathAbsolute(path), overwrite)
    try {
      if (pretty) {
        JacksonWrapper.serializePretty(os, value)
      } else {
        JacksonWrapper.serialize(os, value)
      }

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


trait JsonBackendT {
  protected type ImportExportMap = Map[String,Any]

  /** Экспорт состояния. Метод реализуется на клиенте. */
  def exportState : ImportExportMap

  /** Импорт одного элемента из прочитанного состояния. */
  def importStateElement(key:String, value:Any)


  /**
   * Импорт экспортированного состояния.
   * @param map
   */
  protected def applyState(map: ImportExportMap) {
    map.foreach { case (k,v) => importStateElement(k,v) }
  }

  protected def saveState(implicit ec: ExecutionContext) = writeState(exportState)

  /** Экспортировать состояние, сериализовать и отправить в DFS. */
  def writeState(data: ImportExportMap)(implicit ec: ExecutionContext): Future[_]


  /** Восстановить ранее сохраненное состояние из DFS. */
  protected def loadState(implicit ec: ExecutionContext): Future[Boolean] = {
    readState map maybeApplyState
  }

  protected def maybeApplyState(dataOpt: Option[ImportExportMap]): Boolean = {
    dataOpt exists { data =>
      applyState(data)
      true
    }
  }

  protected def loadSyncTimeout = 2 seconds

  protected def loadStateSync(implicit ec: ExecutionContext): Boolean = {
    Await.result(loadState, loadSyncTimeout)
  }

  /** Прочитать состояние. */
  def readState(implicit ec: ExecutionContext): Future[Option[ImportExportMap]]


  /** Под каким идентификатором сохранять состояние. */
  def getSaveStateId: String

}


// Трайт для быстрой интеграции JsonDfsClient в акторы и синглтоны, работающих вне всех доменов
// Это например супервизоры верхнего уровня, менеджеры индексов и т.д.
trait JsonDfsBackendGlobalT extends JsonBackendT {

  // Защита от параллельной асинхронной записи в один и тот же файл.
  protected var jsonDfsWriteFut: Future[_] = Future.successful()

  protected def getStateFileName = getSaveStateId + ".json"

  protected def getFileSystem = fs

  protected def getStatePath = new Path(siobix_conf_path, getStateFileName)

  def writeState(data: ImportExportMap)(implicit ec: ExecutionContext): Future[_] = {
    jsonDfsWriteFut = jsonDfsWriteFut map { _ => writeStateSync(data) }
    jsonDfsWriteFut
  }

  protected def writeStateSync(data: ImportExportMap) = {
    JsonDfsBackend.writeToPath(getStatePath, data)(getFileSystem)
  }


  def readState(implicit ec: ExecutionContext): Future[Option[ImportExportMap]] = future { readStateSync }

  protected def readStateSync: Option[ImportExportMap] = {
    JsonDfsBackend.getAs[ImportExportMap](getStatePath, getFileSystem)
  }

  /** Нанооптимизация: синхронная запись без Future+Await. */
  override protected def loadStateSync(implicit ec: ExecutionContext): Boolean = {
    maybeApplyState(readStateSync)
  }
}


/** Бэкэнд для хранения random-access данных в MObject. */
trait JsonHBaseBackendT extends JsonBackendT {

  /** Экспортировать состояние, сериализовать и отправить в DFS. */
  def writeState(data: ImportExportMap)(implicit ec: ExecutionContext): Future[_] = {
    val dataSer = JacksonWrapper.serialize(data).getBytes
    MObject.setProp(getSaveStateId, dataSer)
  }

  /** Прочитать состояние. */
  def readState(implicit ec: ExecutionContext): Future[Option[ImportExportMap]] = {
    val key = getSaveStateId
    MObject.getProp(key) map { resultOpt =>
      resultOpt.map {
        v => JacksonWrapper.deserialize[ImportExportMap](v)
      }
    }
  }

}


trait JsonBackendWrapperT extends JsonBackendT {
  protected def jsonBackend: JsonBackendT

  /** Экспортировать состояние, сериализовать и отправить в DFS. */
  def writeState(data: ImportExportMap)(implicit ec: ExecutionContext): Future[_] = {
    jsonBackend.writeState(data)
  }

  /** Прочитать состояние. */
  def readState(implicit ec: ExecutionContext): Future[Option[ImportExportMap]] = {
    jsonBackend.readState
  }

  // Остальное наверное можно не врапать, т.к. те функции обычно не меняются в backend'ах.
}



/** Json-бэкэнд, умеющий работать без проблем, если ему задать значение бэкэнда. */
trait JsonBackendSwitchableT extends JsonBackendWrapperT {

  protected def getJsonBackendStorageType: StorageType.StorageType

  def getSaveStateId: String = getClass.getName

  /** В зависимости от конфига, выбрать тот или иной backend, пробросив в них абстрактные методы. */
  protected def getJsonBackend: JsonBackendT = {
    val me = this
    getJsonBackendStorageType match {
      case StorageType.DFS =>
        new JsonDfsBackendGlobalT {
          def exportState: ImportExportMap = me.exportState
          def importStateElement(key: String, value: Any) = me.importStateElement(key, value)
          def getSaveStateId: String = me.getSaveStateId
        }

      case StorageType.HBASE =>
        new JsonHBaseBackendT {
          def exportState: ImportExportMap = me.exportState
          def importStateElement(key: String, value: Any) = me.importStateElement(key, value)
          def getSaveStateId: String = me.getSaveStateId
        }
    }
  }

}


/** json-бэкэнд, работающий без дополнительных настроек. */
trait JsonBackendAutoswitchT extends JsonBackendSwitchableT {
  protected def getJsonBackendStorageType = SioDefaultStorage.STORAGE
  protected val jsonBackend = getJsonBackend
}

