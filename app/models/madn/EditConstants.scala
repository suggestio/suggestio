package models.madn

import models.im.logo.LogoUtil
import play.api.data.Mapping
import util.img.ImgFormUtil.imgIdOptM
import LogoUtil.LogoOpt_t

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


  def logoKM: (String, Mapping[LogoOpt_t]) = {
    LOGO_IMG_FN -> imgIdOptM
  }

}
