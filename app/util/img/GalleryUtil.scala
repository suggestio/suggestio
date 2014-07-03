package util.img

import models.MImgInfoT
import play.api.data.Forms._
import play.api.Play.{current, configuration}
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.07.14 9:46
 * Description: Утиль для работы с галереей картинок.
 */
object GalleryUtil {

  /** Максимально кол-во картинок в галереи. */
  val GALLERY_LEN_MAX = configuration.getInt("adn.gallery.len.max") getOrElse 7

  val galleryKM = "gallery" -> list(ImgFormUtil.imgIdJpegM)
    .verifying("error.gallery.too.large",  { _.size <= GALLERY_LEN_MAX })

  def gallery2iiks(gallery: List[String]) = {
    gallery.map { OrigImgIdKey.apply }
  }

  def gallery4s(gallery: List[ImgIdKey]) = {
    gallery.map { iik => ImgInfo4Save(iik, withThumb = true) }
  }

  def gallery2filenames(gallery: List[MImgInfoT]) = {
    gallery.map(_.filename)
  }
}
