package models

import util._
import SiobixFs.fs
import io.suggest.model.JsonDfsBackend
import scala.concurrent.{Future, future}
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import util.domain_user_settings.DUS_Basic
import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.hadoop.fs.Path
import org.hbase.async.{GetRequest, PutRequest}
import scala.Some
import scala.collection.JavaConversions._
import StorageUtil.StorageType._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.13 17:41
 * Description: Пользовательские настройки домена. Глобальны в рамках домена. Порт модели domain_data из старого sioweb.
 * Тут разные данные, которые выставляет админ (админы) сайта. Данные хранятся в виде карты Map[String,Any], которая
 * сериализуется в json и обратно. Это немного усложняет код, но позволяет быстро расширять набор настроек без использования
 * версионизации классов и файлов json.
 *
 * Эти настройки вынесены за пределы DomainSettings, потому что последние изменяются кравлером, а эти изменяются только
 * юзером (т.е. веб-интерфейсом).
 *
 * Непосредсвенно сами настройки (ключи, дефолтовые значения, геттеры) вынесены в группы в util.domain_user_settings.*
 * чтобы не засорять ядро модели.
 */


/**
 * ActiveRecord-реализация сабжа.
 * @param dkey ключ домена
 * @param data данные
 */
case class MDomainUserSettings(
  dkey : String,
  data : MDomainUserSettings.DataMap_t
) extends DkeyModelT with DUS_Basic {

  import MDomainUserSettings._

  /** Сохранить карту данных в DFS. Если карта пуста, то удалить файл карты из хранилища. */
  @JsonIgnore def save = BACKEND save this

  /**
   * Заменить данные, создав новый экземпляр сабжа.
   * @param newData Обновлённая карта данных.
   * @return Новый экземпляр класса с обновлённой картой данных.
   */
  def withData(newData:DataMap_t) = MDomainUserSettings(dkey, newData)

  /**
   * Динамическая читалка значений из настроек или списка дефолтовых значений, если ничего не найдено.
   * @param key ключ настройки
   * @tparam T тип значения (обязательно)
   * @return значение указанного типа
   */
  protected def getter[T <: Any](key: String) : T = {
    data.get(key) match {
      case Some(value)  => value.asInstanceOf[T]
      case None         => defaults(key).asInstanceOf[T]
    }
  }

}


/** Статическая сторона модели и её бэкэнды. */
object MDomainUserSettings extends DfsModelStaticT {

  val BACKEND: Backend = StorageUtil.STORAGE match {
    case DFS    => new DfsBackend
    case HBASE  => new HBaseBackend
  }

  type DataMapKey_t = String
  type DataMapValue_t = Any
  type DataMap_t = Map[DataMapKey_t, DataMapValue_t]

  // При добавлении настроек сюда надо конкатенировать все новые карты дефолтовых групп значений.
  def defaults: DataMap_t = DUS_Basic.defaults

  def futureAwaitDuration = 3 seconds

  private val emptyDataMap: DataMap_t = Map()

  /**
   * Прочитать карту для ключа. Даже если ничего не сохранено, функция возвращает рабочий экземпляр класса.
   * @param dkey ключ домена
   * @return MDomainUserSettings, если такой есть в хранилище.
   */
  def getForDkey(dkey:String) = {
    getProps(dkey) map {
      MDomainUserSettings(dkey, _)
    }
  }

  /**
   * Прочитать data для указанного добра.
   * @param dkey ключ домена.
   * @return карта данных пользовательских настроек.
   */
  def getProps(dkey: String) = BACKEND.getProps(dkey)

  /**
   * Выдать пустые (дефолтовые) пользовательские настройки.
   * TODO тут всегда создается объект, хотя можно было бы снизить объем мусора.
   * @param dkey ключ домена
   * @return MDomainUserSettingsT, возвращающий дефолт по всем направлениям.
   */
  def empty(dkey:String) = MDomainUserSettings(dkey, emptyDataMap)


  /** Интерфейс для внутренних бэкэндов модели. */
  trait Backend {
    def save(d: MDomainUserSettings): Future[MDomainUserSettings]
    def getProps(dkey: String): Future[DataMap_t]
  }


  /** Бэкэнд для хранения данных в dfs. */
  class DfsBackend extends Backend {

    // Имя файла, под именем которого сохраняется всё добро. Имена объектов обычно содержат $ на конце, поэтому это удаляем.
    val filename = "domainUserSettings"

    /**
     * Сгенерить DFS-путь для указанного сайта и класса модели.
     * @param dkey ключ домена сайта.
     * @return Путь.
     */
    def getPath(dkey:String) : Path = {
      new Path(SiobixFs.dkeyPathConf(dkey), filename)
    }

    def save(d: MDomainUserSettings): Future[MDomainUserSettings] = future {
      val path = getPath(d.dkey)
      if (!d.data.isEmpty) {
        JsonDfsBackend.writeToPath(path, d.data)
      } else {
        fs.delete(path, false)
      }
      d
    }

    def getProps(dkey: String): Future[MDomainUserSettings.DataMap_t] = future {
      val path = getPath(dkey)
      JsonDfsBackend.getAs[DataMap_t](path, fs) getOrElse emptyDataMap
    }
  }


  /** Бэкэнд для хранения данных модели в HBase. */
  class HBaseBackend extends Backend with ModelSerialJava {
    import io.suggest.model.MObject.{HTABLE_NAME_BYTES, CF_DPROPS}
    import io.suggest.model.SioHBaseAsyncClient._
    import io.suggest.model.HTapConversionsBasic._

    private val CF_DPROPS_B = CF_DPROPS.getBytes
    private def QUALIFIER = CF_DPROPS_B

    def dkey2key(dkey: String): Array[Byte] = dkey
    def deserialize(d: Array[Byte]) = deserializeTo[DataMap_t](d)

    def save(d: MDomainUserSettings): Future[MDomainUserSettings] = {
      val key = dkey2key(d.dkey)
      val putReq = new PutRequest(HTABLE_NAME_BYTES, key, CF_DPROPS_B, QUALIFIER, serialize(d.data))
      ahclient.put(putReq) map {_ => d}
    }

    def getProps(dkey: String): Future[DataMap_t] = {
      val key = dkey2key(dkey)
      val getReq = new GetRequest(HTABLE_NAME_BYTES, key)
        .family(CF_DPROPS_B)
        .qualifier(QUALIFIER)
      ahclient.get(getReq) map { kvs =>
        if (kvs.isEmpty) emptyDataMap else deserialize(kvs.head.value)
      }
    }
  }

}

