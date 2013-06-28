package models

import util.{DkeyModelT, DfsModelStaticT, SiobixFs}
import SiobixFs.fs
import io.suggest.model.JsonDfsBackend
import scala.concurrent.{Future, Await, future}
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import scala.util.{Failure, Success}
import util.domain_user_settings.DUS_Basic

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

// Есть несколько реализаций класса для разных задач. Тут трайт с общим кодом.
trait MDomainUserSettingsT extends DkeyModelT with DUS_Basic {

  import MDomainUserSettings._

  // Абстрактные значения экземпляров классов.
  val dkey: String
  val data: MDomainUserSettings.DataMap_t

  // Базовые геттеры значений
  def json: Option[MDomainUserJson]
  def jsonSync = MDomainUserJson.getForDkey(dkey)


  // Сеттеры. Вызываются через оборот "value.showImages = true"
  def json_=(data:String) = MDomainUserJson(dkey, data).save

  /**
   * Сохранить карту в DFS. Если карта пуста, то удалить файл карты из хранилища.
   */
  def save = {
    val path = getPath(dkey)
    if(!data.isEmpty) {
      JsonDfsBackend.writeTo(path, data)
    } else {
      fs.delete(path, false)
    }
    this
  }

  /**
   * Динамическая читалка значений из настроек или списка дефолтовых значений, если ничего не найдено.
   * @param key ключ настройки
   * @tparam T тип значения (обязательно)
   * @return значение указанного типа
   */
  protected def getter[T <: Any](key:String) : T = {
    data.get(key) match {
      case Some(value)  => value.asInstanceOf[T]
      case None         => defaults(key).asInstanceOf[T]
    }
  }

}


/**
 * Статическая реализация сабжа. Пригодна для быстрого сохранения данных в базу.
 * @param dkey ключ домена
 * @param data данные
 */
case class MDomainUserSettingsStatic(
  dkey : String,
  data : MDomainUserSettings.DataMap_t

) extends MDomainUserSettingsT {
  def json = jsonSync
}


/**
 * Реализация сабжа футуризованная. Карта данных и пользовательский json приходят асинхронно из DFS.
 * Синхронизация происходит через lazy val + Await future.
 * @param dkey ключ домена.
 */
case class MDomainUserSettingsFuturized(dkey:String) extends MDomainUserSettingsT {
  import MDomainUserSettings.{getData, futureAwaitDuration}

  /**
   * Враппер на Await.result чтобы избежать ненужной короткой блокировки, когда фьючерс уже готов.
   * @param fut фьючерс, который уже возможно завершился
   * @param duration макс длительность ожидания. По дефолту = futureAwaitDuration
   * @tparam T тип значения, с фьючерском которого работаем.
   * @return Значение. Или экзепшен.
   */
  protected def maybeAwait[T](fut:Future[T], duration: Duration = futureAwaitDuration): T = {
    if(fut.isCompleted) {
      fut.value.get match {
        case Success(v)  => v
        case Failure(ex) => throw ex
      }
    } else {
      Await.result(fut, duration)
    }
  }

  private val dataFuture = future(getData(dkey))
  lazy val data = maybeAwait(dataFuture)

  private val jsonFuture = future(jsonSync)
  lazy val json = maybeAwait(jsonFuture)
}


object MDomainUserSettings extends DfsModelStaticT {

  type DataMapKey_t = String
  type DataMapValue_t = Any
  type DataMap_t = Map[DataMapKey_t, DataMapValue_t]

  // При добавлении настроек сюда надо конкатенировать все новые карты дефолтовых групп значений.
  val defaults: DataMap_t = DUS_Basic.defaults

  val futureAwaitDuration = 3 seconds

  private val emptyDataMap: DataMap_t = Map()


  /**
   * Прочитать карту для ключа. Даже если ничего не сохранено, функция возвращает рабочий экземпляр класса.
   * @param dkey ключ домена
   * @return MDomainUserSettings, если такой есть в хранилище.
   */
  def getForDkey(dkey:String) : MDomainUserSettingsT = {
    MDomainUserSettingsStatic(dkey, getData(dkey))
  }

  /**
   * Быстро сгенерить объект, чьё состояние генерируется асинхронно.
   * @param dkey ключ домена
   * @return Моментальный MDomainUserSettings, чья карта данных приходит из hdfs в фоне.
   */
  def getForDkeyAsync(dkey:String) : MDomainUserSettingsT = {
    MDomainUserSettingsFuturized(dkey)
  }

  /**
   * Прочитать data для указанного добра.
   * @param dkey ключ домена.
   * @return карта данных пользовательских настроек.
   */
  def getData(dkey:String) : DataMap_t = {
    val path = getPath(dkey)
    JsonDfsBackend.getAs[DataMap_t](path, fs) getOrElse emptyDataMap
  }

  /**
   * Выдать пустые (дефолтовые) пользовательские настройки.
   * TODO тут всегда создается объект, хотя можно было бы снизить объем мусора.
   * @param dkey ключ домена
   * @return MDomainUserSettingsT, возвращающий дефолт по всем направлениям.
   */
  def empty(dkey:String): MDomainUserSettingsT = MDomainUserSettingsStatic(dkey, emptyDataMap)
}