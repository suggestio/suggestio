package models

import collection.mutable
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

  def showImages = data.getOrElse[Boolean](KEY_SHOW_IMAGES, DFLT_SHOW_IMAGES)
  def showImages_= (value:Boolean) {
    value match {
      case DFLT_SHOW_IMAGES => data.remove(KEY_SHOW_IMAGES)
      case _                => data(KEY_SHOW_IMAGES) = value
    }
  }

  def showTitle  = data.getOrElse[String](KEY_SHOW_TITLE, DFLT_SHOW_TITLE)
  def showTitle_= (value:String) {
    value match {
      case DFLT_SHOW_TITLE => data.remove(KEY_SHOW_TITLE)
      case _               => data(KEY_SHOW_TITLE) = value
    }
  }

  def showContentText = data.getOrElse[String](KEY_SHOW_CONTENT_TEXT, DFLT_SHOW_CONTENT_TEXT)
  def showContentText_= (value:String) {
    value match {
      case DFLT_SHOW_CONTENT_TEXT => data.remove(KEY_SHOW_CONTENT_TEXT)
      case _ => data(KEY_SHOW_CONTENT_TEXT) = value
    }
  }

  def renderer = data.getOrElse[Int](KEY_RENDERER, DFLT_RENDERER)
  def renderer_= (value:Int) {
    value match {
      case DFLT_RENDERER => data.remove(KEY_RENDERER)
      case _ => data(KEY_RENDERER) = value
    }
  }

  def useDateScoring = data.getOrElse[Boolean](KEY_USE_DATE_SCORING, DFLT_USE_DATE_SCORING)
  def useDateScoring_= (value:Boolean) {
    value match {
      case DFLT_USE_DATE_SCORING => data.remove(KEY_USE_DATE_SCORING)
      case _ => data(KEY_USE_DATE_SCORING) = value
    }
  }

  /**
   * Сохранить карту в DFS. Если карта пуста, то удалить файл карты из хранилища.
   */
  def save {
    val path = getPath(dkey)
    if(!data.isEmpty) {
      JsonDfsBackend.writeTo(path, data)
    } else {
      fs.delete(path, false)
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
  val DFLT_SHOW_IMAGES = true
  val DFLT_SHOW_TITLE  = SHOW_ALWAYS
  val DFLT_SHOW_CONTENT_TEXT = SHOW_ALWAYS
  val DFLT_RENDERER = RRR_2013_FULLSCREEN
  val DFLT_USE_DATE_SCORING = true


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