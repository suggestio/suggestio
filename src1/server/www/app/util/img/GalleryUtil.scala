package util.img

import io.suggest.adn.edit.NodeEditConstants
import javax.inject.{Inject, Singleton}
import io.suggest.common.geom.d2.MSize2di
import io.suggest.dev.MPxRatios
import io.suggest.n2.node.MNode
import io.suggest.url.MHostInfo
import models.im._
import models.mctx.Context
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
                              cdnUtil                 : CdnUtil,
                              implicit private val ec : ExecutionContext
                            ) {

  /**
   * Генерация dyn-img ссылки на картинку галереи, которая отображается (обычно) откропанной в ЛК на странице узла.
   * @param mimg id картинки.
   * @param ctx Контекст рендера шаблонов.
   * @return Экземпляр Call, пригодный для заворачивания в ссылку.
   */
  def dynLkBigCall(mimg: MImgT)(implicit ctx: Context): Call = {
    val devPixelRatio = ctx.deviceScreenOpt
      .fold(MPxRatios.default)(_.pxRatio)
    // Всегда ресайзим до необходимого отображаемого размера. Используем fg-качество для сжатия.
    // TODO Height должен быть необязательный, но максимум 200 пикселей.
    val newSz = MSize2di(
      width  = szMulted( NodeEditConstants.Gallery.WIDTH_PX,  devPixelRatio.pixelRatio),
      height = szMulted( NodeEditConstants.Gallery.HEIGHT_PX, devPixelRatio.pixelRatio)
    )

    val outFmt = mimg.dynImgId.dynFormat

    var imOps: List[ImOp] =
      ImGravities.Center ::
      BackgroundOp( None ) ::
      AbsResizeOp( newSz, ImResizeFlags.FillArea ) ::
      ExtentOp( newSz ) ::
      ImInterlaces.Plane ::
      ImCompression.forPxRatio( CompressModes.Fg, devPixelRatio)
        .toOps( outFmt )

    // Если необходимо, то сначала делаем кроп:
    // TODO Если картинка не кропаная, то кропать её принудительно на 200 пикселей по высоте?
    for (crop <- mimg.dynImgId.cropOpt)
      imOps ::= AbsCropOp(crop)

    val dynArgs = mimg.withDynOps(imOps)
    dynImgUtil.imgCall(dynArgs)
  }


  def galleryImgs(mnode: MNode): Future[List[MImgT]] = {
    // List исторически используется в исходниках, хотя по логике тут должна быть Seq
    val res = mnode.Quick.Adn.galImgs
      .iterator
      .map { case (jdId, galImgEdge) =>
        MImg3( MDynImgId.fromJdEdge(jdId, galImgEdge) )
      }
      .toList
    Future.successful( res )
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
      mediaIds      = galleryImg.dynImgId.mediaIdAndOrigMediaId
    )
  }

}
