package util.img

import com.google.inject.{Singleton, Inject}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.ym.model.common.MImgInfoMeta
import models.im._
import models.{IEdge, MEdge, MNode, Context}
import models.madn.EditConstants
import play.api.Configuration
import play.api.data.Forms._
import play.api.data.Mapping
import play.api.mvc.Call
import models.blk.szMulted

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.07.14 9:46
 * Description: Утиль для работы с галереей картинок.
 */
@Singleton
class GalleryUtil @Inject() (
  mImg3           : MImg3_,
  configuration   : Configuration
) {

  // Ширина/высота картинки галереи, отображаемой в ЛК на странице узла.
  val LK_NODE_GALLERY_SHOW_WIDTH_PX: Int  = configuration.getInt("lk.node.gallery.show.width.px") getOrElse 625
  val LK_NODE_GALLERY_SHOW_HEIGHT_PX: Int = configuration.getInt("lk.node.gallery.show.height.px") getOrElse 200

  /** Максимально кол-во картинок в галереи. */
  val GALLERY_LEN_MAX = configuration.getInt("adn.gallery.len.max") getOrElse 7

  def galleryM: Mapping[List[MImgT]] = {
    list(ImgFormUtil.img3IdM)
      .verifying("error.gallery.too.large",  { _.size <= GALLERY_LEN_MAX })
  }

  def galleryKM = EditConstants.GALLERY_FN -> galleryM

  def gallery2iiks(gallery: TraversableOnce[IEdge]): Iterator[MImgT] = {
    gallery
      .toIterator
      .map { medge =>
        galEdge2img(medge) -> medge.order.getOrElse(Int.MaxValue)
      }
      .toSeq
      .sortBy( _._2 )
      .iterator
      .map(_._1)
  }

  def galEdge2img(edge: IEdge): MImgT = {
    mImg3(edge)
  }


  /**
   * Асинхронное обновление галереи. Входы и выходы в форматах, удобных для работы.
   * @param newGallery Новая галерея (результат бинда формы).
   * @param oldGallery Старое содержимое галереи.
   * @return Фьючерс с новой галереи в формате старой галереи.
   */
  def updateGallery(newGallery: Seq[MImgT], oldGallery: Seq[String]): Future[Seq[MImgT]] = {
    ImgFormUtil.updateOrigImgId(needImgs = newGallery, oldImgIds = oldGallery)
  }


  /**
   * Генерация dyn-img ссылки на картинку галереи, которая отображается (обычно) откропанной в ЛК на странице узла.
   * @param imgId id картинки.
   * @param ctx Контекст рендера шаблонов.
   * @return Экземпляр Call, пригодный для заворачивания в ссылку.
   */
  def dynLkBigCall(imgId: String)(implicit ctx: Context): Call = {
    dynLkBigCall( mImg3(imgId) )
  }
  def dynLkBigCall(oiik: MImgT)(implicit ctx: Context): Call = {
    val devPixelRatio = ctx.deviceScreenOpt
      .fold(DevPixelRatios.default)(_.pixelRatio)
    // Всегда ресайзим до необходимого отображаемого размера. Используем fg-качество для сжатия.
    // TODO Height должен быть необязательный, но максимум 200 пикселей.
    val newSz = MImgInfoMeta(
      width  = szMulted(LK_NODE_GALLERY_SHOW_WIDTH_PX,  devPixelRatio.pixelRatio),
      height = szMulted(LK_NODE_GALLERY_SHOW_HEIGHT_PX, devPixelRatio.pixelRatio)
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
    val dynArgs = oiik.withDynOps(imOps)
    DynImgUtil.imgCall(dynArgs)
  }


  def galleryEdges(mnode: MNode): Iterator[MEdge] = {
    mnode.edges
      .withPredicateIter( MPredicates.GalleryItem )
  }

  def galleryImgs(mnode: MNode): Future[Seq[MImgT]] = {
    val res = galleryEdges(mnode)
      .map { mImg3.apply }
      .toSeq
    Future successful res
  }

}
