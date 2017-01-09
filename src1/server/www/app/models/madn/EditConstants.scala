package models.madn

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.05.15 10:15
 * Description: Константы для редактора узла.
 */
object EditConstants {

  val GALLERY_FN = "gallery"

  def galleryFn(index: Int) = GALLERY_FN + "[" + index + "]"

  val LOGO_IMG_FN = "logoImgId"

  val WELCOME_IMG_FN = "welcomeImgId"

}
