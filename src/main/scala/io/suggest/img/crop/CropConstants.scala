package io.suggest.img.crop

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.04.15 10:09
 * Description: Константы для клиент-серверной системы кропа.
 */
object CropConstants {

  /** Форма для кропа запрашивается с сервера и обычно отображается в попапе.
    * Тут id тега верхнего уровня для верстки, приходящей с сервера. */
  def CROPPER_DIV_ID = "imgCropTool"

  /** Класс для кнопки в редакторе для активации кропа какого-то (текущего) изображения. */
  def CROP_IMAGE_BTN_CLASS = "js-crop-image-btn"

}

