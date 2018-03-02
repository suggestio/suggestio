package util.img

import javax.inject.{Inject, Singleton}

import io.suggest.common.geom.d2.MSize2di
import io.suggest.model.n2.edge.{MEdge, MPredicates}
import io.suggest.model.n2.node.MNode
import io.suggest.url.MHostInfo
import models.im._
import models.mctx.Context
import models.madn.EditConstants
import play.api.Configuration
import play.api.data.Forms._
import play.api.data.Mapping
import play.api.mvc.Call
import models.blk.szMulted
import util.cdn.CdnUtil

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.07.14 9:46
 * Description: Утиль для работы с галереей картинок.
 */
@Singleton
class GalleryUtil @Inject() (
                              dynImgUtil              : DynImgUtil,
                              imgFormUtil             : ImgFormUtil,
                              cdnUtil                 : CdnUtil,
                              configuration           : Configuration,
                              implicit private val ec : ExecutionContext
                            ) {

  // Ширина/высота картинки галереи, отображаемой в ЛК на странице узла.
  def LK_NODE_GALLERY_SHOW_WIDTH_PX = 625   //: Int  = configuration.getInt("lk.node.gallery.show.width.px") getOrElse 625
  def LK_NODE_GALLERY_SHOW_HEIGHT_PX = 200  //: Int = configuration.getInt("lk.node.gallery.show.height.px") getOrElse 200

  /** Максимально кол-во картинок в галереи. */
  val GALLERY_LEN_MAX = configuration.getOptional[Int]("adn.gallery.len.max") getOrElse 7

  def galleryM: Mapping[List[MImgT]] = {
    list(imgFormUtil.img3IdM)
      .verifying("error.gallery.too.large", _.lengthCompare(GALLERY_LEN_MAX) <= 0)
  }

  def galleryKM = EditConstants.GALLERY_FN -> galleryM

  def gallery2iiks(gallery: TraversableOnce[MEdge]): Iterator[MImgT] = {
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

  def galEdge2img(edge: MEdge): MImgT = {
    MImg3(edge)
  }


  /**
   * Асинхронное обновление галереи. Входы и выходы в форматах, удобных для работы.
   * @param newGallery Новая галерея (результат бинда формы).
   * @param oldGallery Старое содержимое галереи.
   * @return Фьючерс с новой галереи в формате старой галереи.
   */
  def updateGallery(newGallery: Seq[MImgT], oldGallery: Seq[String]): Future[Seq[MImgT]] = {
    imgFormUtil.updateOrigImgId(
      needImgs  = newGallery,
      oldImgIds = oldGallery
    )
  }


  /**
   * Генерация dyn-img ссылки на картинку галереи, которая отображается (обычно) откропанной в ЛК на странице узла.
   * @param mimg id картинки.
   * @param ctx Контекст рендера шаблонов.
   * @return Экземпляр Call, пригодный для заворачивания в ссылку.
   */
  def dynLkBigCall(mimg: MImgT)(implicit ctx: Context): Call = {
    val devPixelRatio = ctx.deviceScreenOpt
      .fold(DevPixelRatios.default)(_.pixelRatio)
    // Всегда ресайзим до необходимого отображаемого размера. Используем fg-качество для сжатия.
    // TODO Height должен быть необязательный, но максимум 200 пикселей.
    val newSz = MSize2di(
      width  = szMulted(LK_NODE_GALLERY_SHOW_WIDTH_PX,  devPixelRatio.pixelRatio),
      height = szMulted(LK_NODE_GALLERY_SHOW_HEIGHT_PX, devPixelRatio.pixelRatio)
    )
    var imOps: List[ImOp] = List(
      ImGravities.Center,
      AbsResizeOp( newSz, ImResizeFlags.FillArea ),
      ExtentOp( newSz ),
      devPixelRatio.fgCompression.imQualityOp,
      ImInterlaces.Plane
    )
    // Если необходимо, то сначала делаем кроп:
    // TODO Если картинка не кропаная, то кропать её принудительно на 200 пикселей по высоте?
    for (crop <- mimg.dynImgId.cropOpt)
      imOps ::= AbsCropOp(crop)

    val dynArgs = mimg.withDynOps(imOps)
    dynImgUtil.imgCall(dynArgs)
  }


  def galleryEdges(mnode: MNode): Iterator[MEdge] = {
    mnode.edges
      .withPredicateIter( MPredicates.GalleryItem )
  }

  def galleryImgs(mnode: MNode): Future[List[MImgT]] = {
    val res = galleryEdges(mnode)
      .map { MImg3.apply }
      .toList
    Future successful res
  }


  /** Рендер ссылок на картинки галлереи с учётом dist-cdn.
    *
    * @param galleryImgs Галерные картинки.
    * @param mediaHostsMapFut Карта media-хостов.
    * @param ctx Контекст рендера.
    * @return Фьючерс со списком ссылок в исходном порядке.
    */
  def renderGalleryCdn(galleryImgs: Seq[MImgT], mediaHostsMapFut: Future[Map[String, Seq[MHostInfo]]])(implicit ctx: Context): Future[Seq[Call]] = {
    if (galleryImgs.isEmpty) {
      Future.successful(Nil)
    } else {
      for (mediaHostsMap <- mediaHostsMapFut) yield {
        for (galImg <- galleryImgs) yield {
          renderGalleryItemCdn(galImg, mediaHostsMap)
        }
      }
    }
  }

  /** Рендер одного элемента галереи узла с учётом dist-cdn.
    *
    * @param galleryImg Галерейная картинка.
    * @param mediaHostsMap Карта media-хостов.
    * @param ctx Контекст рендера.
    * @return Ссылка на картинку галереи.
    */
  def renderGalleryItemCdn(galleryImg: MImgT, mediaHostsMap: Map[String, Seq[MHostInfo]])(implicit ctx: Context): Call = {
    cdnUtil.forMediaCall1(
      call          = dynLkBigCall(galleryImg),
      mediaHostsMap = mediaHostsMap,
      mediaIds      = galleryImg.dynImgId.mediaIdWithOriginalMediaId
    )
  }

}
