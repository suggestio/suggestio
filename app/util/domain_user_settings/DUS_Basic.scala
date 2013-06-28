package util.domain_user_settings

import models.MDomainUserJson

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
  val KEY_USE_DATE_SCORING = "use_date_scoring"

  // Параметры отображения title и content_text
  val SHOW_ALWAYS = "always"
  val SHOW_NEVER  = "never"
  val SHOW_IF_NO_IMAGES = "if_no_images"

  // Рендереры
  val RRR_2012_SIMPLE = 1
  val RRR_2013_FULLSCREEN = 2
  val RRR_AVAILABLE = List(RRR_2012_SIMPLE, RRR_2013_FULLSCREEN)

  // Дефолтовые значения для параметров
  val defaults = Map[String, Any](
    KEY_SHOW_IMAGES       -> true,
    KEY_SHOW_TITLE        -> SHOW_ALWAYS,
    KEY_SHOW_CONTENT_TEXT -> SHOW_ALWAYS,
    KEY_RENDERER          -> RRR_2013_FULLSCREEN,
    KEY_USE_DATE_SCORING  -> true
  )
}


trait DUS_Basic {
  import DUS_Basic._

  protected def getter[T <: Any](key:String) : T

  def showImages = getter[Boolean](KEY_SHOW_IMAGES)
  def showTitle = getter[String](KEY_SHOW_TITLE)
  def showContentText = getter[String](KEY_SHOW_CONTENT_TEXT)
  def renderer = getter[Int](KEY_RENDERER)
  def useDateScoring = getter[Boolean](KEY_USE_DATE_SCORING)
  def json: Option[MDomainUserJson]
}
