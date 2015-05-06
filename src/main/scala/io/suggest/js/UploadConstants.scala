package io.suggest.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.04.15 15:49
 * Description: Константы для загрузки файлов: js и формы.
 */
object UploadConstants {

  /** css-класс-пометка для input'а, занимающегося загрузкой файлов. */
  def JS_FILE_UPLOAD_CLASS        = "js-file-upload"

  /** css-класс для контейнеров загрузки. */
  def JS_IMG_UPLOAD_CLASS         = "js-image-upload"

  /** Название аттрибута, где лежит счетчик для множественной загрузки элементов. */
  def ATTR_MULTI_INDEX_COUNTER    = "data-name-index"

  /** Название параметра в qs ссылки, где содержится индекс для генерируемого имени поля формы. */
  def NAME_INDEX_QS_NAME          = "_ni"

}
