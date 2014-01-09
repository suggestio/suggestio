package util.domain_user_settings

import models.MDomainUserJson
import play.api.data.Forms._
import models.MDomainUserSettings.{DataMap_t, DataMapKey_t}
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.06.13 10:18
 * Description: Функции и константы для группы самых базовых настроек домена,
 * которые присутствовали ещё в эрланговском sioweb.
 */
object DUS_Basic {

  // Список основных ключей, используемых в карте данных
  val KEY_SHOW_IMAGES = "show_images"
  val KEY_SHOW_CONTENT_TEXT = "show_content_text"
  val KEY_SHOW_TITLE = "show_title"
  val KEY_RENDERER = "js_renderer"
  //val KEY_USE_DATE_SCORING = "use_date_scoring"

  // Параметры отображения title и content_text
  val SHOW_ALWAYS = "always"
  val SHOW_NEVER  = "never"
  val SHOW_IF_NO_IMAGES = "if_no_images"
  val SHOW_POSSIBLE_VALUES = List(SHOW_ALWAYS, SHOW_NEVER, SHOW_IF_NO_IMAGES)

  val showMapper = nonEmptyText(4, 13)
    .verifying("Invalid value", SHOW_POSSIBLE_VALUES.contains(_))


  // Рендереры
  val RRR_2012_SIMPLE = 1
  val RRR_2013_FULLSCREEN = 2
  val RRR_AVAILABLE = List(RRR_2012_SIMPLE, RRR_2013_FULLSCREEN)

  val rrrMapper = number(min=RRR_2012_SIMPLE, max=RRR_2013_FULLSCREEN)

  // Дефолтовые значения для параметров
  val defaults: DataMap_t = Map(
    KEY_SHOW_IMAGES       -> true,
    KEY_SHOW_TITLE        -> SHOW_ALWAYS,
    KEY_SHOW_CONTENT_TEXT -> SHOW_ALWAYS,
    //KEY_USE_DATE_SCORING  -> true,
    KEY_RENDERER          -> RRR_2013_FULLSCREEN
  )

  /** Хелпер для комбинируемых функций applyDomainSettings. */
  val applyBasicSettingsF: PartialFunction[(DataMapKey_t, String, DataMap_t), DataMap_t] = {
    case (k @ KEY_SHOW_IMAGES, v, data) =>
      boolean.bind(Map(k -> v)) match {
        case Left(_)  => data
        case Right(b) => data + (k -> b)
      }

    case (k @ (KEY_SHOW_TITLE | KEY_SHOW_CONTENT_TEXT), v, data) =>
      showMapper.bind(Map(k -> v)) match {
        case Left(_)   => data
        case Right(v1) => data + (k -> v1)
      }

    case (k @ KEY_RENDERER, v, data) =>
      rrrMapper.bind(Map(k -> v)) match {
        case Left(_)    => data
        case Right(rrr) => data + (k -> rrr)
      }
  }

}


trait DUS_Basic {
  import DUS_Basic._

  protected def getter[T <: Any](key:String) : T
  def dkey: String

  def showImages = getter[Boolean](KEY_SHOW_IMAGES)
  def showTitle = getter[String](KEY_SHOW_TITLE)
  def showContentText = getter[String](KEY_SHOW_CONTENT_TEXT)
  def renderer = getter[Int](KEY_RENDERER)
  //def useDateScoring = getter[Boolean](KEY_USE_DATE_SCORING)
  def json: Future[Option[MDomainUserJson]] = MDomainUserJson.getForDkey(dkey)
}
