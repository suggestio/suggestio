package io.suggest.adn.edit

import scalaz.{Validation, ValidationNel}
import scalaz.syntax.apply._


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.04.15 14:59
 * Description:
 */
object NodeEditConstants {

  /** Контейнер для системы управления карточкой приветствия. */
  def NODE_WELCOME_DIV_ID = "welcomeImage"

  /** id контейнера галереи. */
  def NODE_GALLERY_DIV_ID = "profileGallery"

  /** id контейнера для управления логотипом узла. */
  def NODE_LOGO_DIV_ID = "logo"


  object Name {
    def LEN_MAX = 64
    def LEN_MIN = 1

    /** Валидация названия узла. */
    def validateNodeName(raw: String): ValidationNel[String, String] = {
      val l = raw.length
      val ePrefix = "e.node.name.len.too"
      val minLenOk = Validation.liftNel(l)( _ > LEN_MAX, s"$ePrefix.big" )
      val maxLenOk = Validation.liftNel(l)( _ < LEN_MIN, s"$ePrefix.small" )
      (minLenOk |@| maxLenOk) { (_,_) => raw }
    }

  }

  /** id контейнера для кнопки сохранения. */
  def SAVE_BTN_CONTAINER_ID = "aer"

  /** id контейнера для формы. */
  def FORM_CONTAINER_ID = "aec"

}
