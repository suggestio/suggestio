package models

import scala.collection.{immutable, mutable}
import util.SiobixFs
import SiobixFs.fs
import io.suggest.model.JsonDfsBackend
import org.apache.hadoop.fs.Path

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

case class MDomainUserSettings(
  dkey : String,
  data : MDomainUserSettings.DataT
) {
  import MDomainUserSettings._

  /*
   * Быстрое чтение различных элементов карты. Здесь эмулируется работа "как с переменными":
   * val si = mdus.showImages
   * mdus.showImages = false
   */

  def showImages = getter[Boolean](KEY_SHOW_IMAGES)
  def showImages_= (value:Boolean) { setter(KEY_SHOW_IMAGES, value) }

  def showTitle = getter[String](KEY_SHOW_TITLE)
  def showTitle_= (value:String) { setter(KEY_SHOW_TITLE, value) }

  def showContentText = getter[String](KEY_SHOW_CONTENT_TEXT)
  def showContentText_= (value:String) { setter(KEY_SHOW_CONTENT_TEXT, value) }

  def renderer = getter[Int](KEY_RENDERER)
  def renderer_= (value:Int) { setter(KEY_RENDERER, value) }

  def useDateScoring = getter[Boolean](KEY_USE_DATE_SCORING)
  def useDateScoring_= (value:Boolean) { setter(KEY_USE_DATE_SCORING, value) }

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


object MDomainUserSettings {

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

  /**
   * Сгенерить dfs-путь для указанного dkey
   * @param dkey
   * @return
   */
  protected def getPath(dkey:String) = {
    val filename = getClass.getCanonicalName
    new Path(SiobixFs.dkeyPathConf(dkey), filename)
  }


  /**
   * Прочитать карту для ключа. Даже если ничего не сохранено, функция возвращает рабочий экземпляр класса.
   * @param dkey ключ домена
   * @return
   */
  def getForDkey(dkey:String) : MDomainUserSettings = {
    val path = getPath(dkey)
    val data : DataT = JsonDfsBackend.getAs[DataT](path, fs).getOrElse(mutable.Map())
    MDomainUserSettings(dkey, data)
  }

}