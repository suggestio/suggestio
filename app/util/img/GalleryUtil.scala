package util.img

import io.suggest.ym.model.common.MImgInfoMeta
import models.im._
import models.{DynImgArgs, Context, MImgInfoT}
import play.api.data.Forms._
import play.api.Play.{current, configuration}
import play.api.mvc.Call

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.07.14 9:46
 * Description: Утиль для работы с галереей картинок.
 */
object GalleryUtil {

  // Ширина/высота картинки галереи, отображаемой в ЛК на странице узла.
  val LK_NODE_GALLERY_SHOW_WIDTH_PX: Int  = configuration.getInt("lk.node.gallery.show.width.px") getOrElse 625
  val LK_NODE_GALLERY_SHOW_HEIGHT_PX: Int = configuration.getInt("lk.node.gallery.show.height.px") getOrElse 200

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


  /**
   * Генерация dyn-img ссылки на картинку галереи, которая отображается (обычно) откропанной в ЛК на странице узла.
   * @param imgId id картинки.
   * @param ctx Контекст рендера шаблонов.
   * @return Экземпляр Call, пригодный для заворачивания в ссылку.
   */
  def dynLkBigCall(imgId: String)(implicit ctx: Context): Call = {
    val oiik = OrigImgIdKey(imgId)
    val devPixelRatio = ctx.deviceScreenOpt
      .fold(DevPixelRatios.default)(_.pixelRatio)
    // Всегда ресайзим до необходимого отображаемого размера. Используем fg-качество для сжатия.
    // TODO Height должен быть необязательный, но максимум 200 пикселей.
    val newSz = MImgInfoMeta(
      width  = (LK_NODE_GALLERY_SHOW_WIDTH_PX * devPixelRatio.pixelRatio).toInt,
      height = (LK_NODE_GALLERY_SHOW_HEIGHT_PX * devPixelRatio.pixelRatio).toInt
    )
    var imOps: List[ImOp] = List(
      AbsResizeOp(newSz, ImResizeFlags.IgnoreAspectRatio),
      devPixelRatio.fgCompression.imQualityOp,
      ImInterlace.Plane
    )
    // Если необходимо, то сначала делаем кроп:
    // TODO Если картинка не кропаная, то кропать её принудительно на 200 пикселей по высоте?
    if (oiik.isCropped) {
      val crop = oiik.cropOpt.get
      imOps ::= AbsCropOp(crop)
    }
    val dynArgs = DynImgArgs(oiik.uncropped, imOps)
    DynImgUtil.imgCall(dynArgs)
  }

}
