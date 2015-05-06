package io.suggest.img

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.04.15 14:35
 * Description: Константы для работы с картинками.
 */
object ImgConstants {

  /** CSS-класс-пометка для js, что нужно произвести удаление . */
  def JS_REMOVE_IMG_CLASS = "js-remove-image"

  /** CSS-класс-пометка для js, что тут должна быть превьюшка картинки. */
  def JS_PREVIEW_CLASS    = "js-preview"

  /** Ключ картинки должен сохранятся в input с указанным классом. */
  def JS_IMG_ID_CLASS     = "js-image-key"


  /** json: ключ поля статуса аплоада */
  def JSON_UPLOAD_STATUS  = "status"

  /** json: ключ изображения по базам suggest.io. */
  def JSON_IMG_KEY        = "image_key"

  /** json: ссылка на превьюшку изображения. */
  def JSON_IMG_THUMB_URI  = "image_link"

  /** json: верстка куска формы, которая требуется для отображения успешной загрузки. */
  def JSON_FORM_FIELD_HTML = "html"

}
