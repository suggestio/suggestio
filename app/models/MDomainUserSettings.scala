package models

import scala.collection.{immutable, mutable}
import util.{DfsModelStaticT, SiobixFs}
import SiobixFs.fs
import io.suggest.model.JsonDfsBackend
import scala.concurrent.{Await, future}
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.13 17:41
 * Description: Пользовательские настройки домена. Глобальны в рамках домена. Порт модели domain_data из старого sioweb.
 * Тут разные данные, которые выставляет админ сайта. Данные хранятся в виде карты Map[String,Any], которая
 * транслируется в json и обратно. Это немного усложняет код, но позволяет быстро расширять набор настроек без использования
 * версионизации классов и файлов json.
 *
 * Эти настройки вынесены из DomainSettings потому что последние изменяются кравлером по команде, а эти изменяются только
 * юзером.
 */

// Есть несколько реализаций класса для разных задач. Тут трайт с общим кодом.
trait MDomainUserSettingsT {

  import MDomainUserSettings._

  // Абстрактные значения экземпляров классов.
  val dkey: String
  val data: MDomainUserSettings.DataT

  // геттеры
  def showImages = getter[Boolean](KEY_SHOW_IMAGES)
  def showTitle = getter[String](KEY_SHOW_TITLE)
  def showContentText = getter[String](KEY_SHOW_CONTENT_TEXT)
  def renderer = getter[Int](KEY_RENDERER)
  def useDateScoring = getter[Boolean](KEY_USE_DATE_SCORING)
  def json: Option[MDomainUserJson]
  protected def jsonSync = MDomainUserJson.getForDkey(dkey)

  // Сеттеры. Вызываются через оборот "value.showImages = true"
  def showImages_=(value:Boolean) { setter(KEY_SHOW_IMAGES, value) }
  def showTitle_=(value:String) { setter(KEY_SHOW_TITLE, value) }
  def showContentText_=(value:String) { setter(KEY_SHOW_CONTENT_TEXT, value) }
  def renderer_=(value:Int) { setter(KEY_RENDERER, value) }
  def useDateScoring_=(value:Boolean) { setter(KEY_USE_DATE_SCORING, value) }
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


  /**
   * Динамически-типизированный хелпер для работы с картой настроек
   * @param key ключ настроек
   * @param value значение настройки
   * @tparam T тип значения (автоматически выводится из value)
   */
  protected def setter[T <: Any](key:String, value:T) {
    defaults.get(key) match {
      case dflt if dflt == value => data.remove(key)
      case _ => data(key) = value
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
  data : MDomainUserSettings.DataT

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

  private val dataFuture = future(getData(dkey))
  lazy val data = {
    if (dataFuture.isCompleted)
      dataFuture.value
    else
      Await.result(dataFuture, futureAwaitDuration)
  }

  private val jsonFuture = future(jsonSync)
  lazy val json = {
    if (jsonFuture.isCompleted)
      jsonFuture.value
    else
      Await.result(jsonFuture, futureAwaitDuration)
  }
}


object MDomainUserSettings extends DfsModelStaticT {

  // Список основных ключей, используемых в карте данных
  val KEY_SHOW_IMAGES = "show_images"
  val KEY_SHOW_CONTENT_TEXT = "show_content_text"
  val KEY_SHOW_TITLE = "show_title"
  val KEY_RENDERER = "js_renderer"
  val KEY_USE_DATE_SCORING = "use_date_scoring"

  type DataT = mutable.Map[String, Any]

  // Параметры отображения title и content_text
  val SHOW_ALWAYS = "always"
  val SHOW_NEVER  = "never"
  val SHOW_IF_NO_IMAGES = "if_no_images"

  // Рендереры
  val RRR_2012_SIMPLE = 1
  val RRR_2013_FULLSCREEN = 2
  val RRR_AVAILABLE = List(RRR_2012_SIMPLE, RRR_2013_FULLSCREEN)

  // Дефолтовые значения для параметров
  val defaults = immutable.Map[String, Any](
    KEY_SHOW_IMAGES       -> true,
    KEY_SHOW_TITLE        -> SHOW_ALWAYS,
    KEY_SHOW_CONTENT_TEXT -> SHOW_ALWAYS,
    KEY_RENDERER          -> RRR_2013_FULLSCREEN,
    KEY_USE_DATE_SCORING  -> true
  )

  val futureAwaitDuration = 3 seconds


  /**
   * Прочитать карту для ключа. Даже если ничего не сохранено, функция возвращает рабочий экземпляр класса.
   * @param dkey ключ домена
   * @return MDomainUserSettings, если такой есть в хранилище.
   */
  def getForDkey(dkey:String) = {
    MDomainUserSettingsStatic(dkey, getData(dkey))
  }

  /**
   * Прочитать data для указанного добра.
   * @param dkey ключ домена.
   * @return карта данных пользовательских настроек.
   */
  def getData(dkey:String) : DataT = {
    val path = getPath(dkey)
    JsonDfsBackend.getAs[DataT](path, fs).getOrElse(mutable.Map())
  }

}