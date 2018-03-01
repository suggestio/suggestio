package util.img

import javax.inject.{Inject, Singleton}

import io.suggest.common.fut.FutureUtil
import io.suggest.common.geom.d2.ISize2di
import io.suggest.img.{MImgFmt, MImgFmts}
import io.suggest.model.n2.edge.{MEdge, MEdgeDynImgArgs, MEdgeInfo, MPredicates}
import io.suggest.model.n2.node.MNode
import io.suggest.util.logs.MacroLogsImpl
import models.im._
import models.madn.EditConstants
import models.mctx.Context
import models.mproj.ICommonDi
import models.mwc.MWelcomeRenderArgs
import util.cdn.CdnUtil
import util.showcase.ShowcaseUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.07.14 10:54
 * Description: Утиль для картинки/карточки приветствия.
 */
@Singleton
class WelcomeUtil @Inject() (
  scUtil                 : ShowcaseUtil,
  cdnUtil                : CdnUtil,
  mImgs3                 : MImgs3,
  imgFormUtil            : ImgFormUtil,
  mCommonDi              : ICommonDi
)
  extends MacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  /** Ключ для картинки, используемой в качестве приветствия. */
  // TODO Удалить вслед за старой архитектурой.

  def welcomeImgIdKM = EditConstants.WELCOME_IMG_FN -> imgFormUtil.img3IdOptM


  /** Обновление картинки и карточки приветствия. Картинка хранится в полу-рекламной карточке, поэтому надо ещё
    * обновить карточку и пересохранить её. */
  def updateWcFgImg(adnNode: MNode, newWelcomeImgOpt: Option[MImgT]): Future[Option[MEdge]] = {
    // Сохранить картинку, вернуть эдж. Нет картинки -- нет эджа.
    FutureUtil.optFut2futOpt(newWelcomeImgOpt) { fgMimg =>
      for (_ <- mImgs3.saveToPermanent(fgMimg)) yield {
        val e = MEdge(
          predicate = MPredicates.WcLogo,
          nodeIds   = Set(fgMimg.dynImgId.rowKeyStr),
          info = MEdgeInfo(
            dynImgArgs = Some(MEdgeDynImgArgs(
              dynFormat = fgMimg.dynImgId.dynFormat,
              dynOpsStr = fgMimg.dynImgId.qOpt
            ))
          )
        )
        Some(e)
      }
    }
  }


  /**
   * Извлечь логотип карточки приветствия.
   * @param mnode Отрабатываемый узел.
   * @return Опциональная картинка.
   */
  def wcLogoImg(mnode: MNode): Option[MImgT] = {
    mnode.edges
      .withPredicateIter( MPredicates.WcLogo )
      .map { MImg3.apply }
      .toStream
      .headOption
  }


  /**
   * Асинхронно собрать аргументы для рендера карточки приветствия.
   * @param mnode Узел, для которого нужно подготовить настройки рендера приветствия.
   * @param screen Настройки экрана, если есть.
   * @return Фьючерс с опциональными настройками. Если None, то приветствие рендерить не надо.
   */
  def getWelcomeRenderArgs(mnode: MNode, screen: Option[DevScreen])
                          (implicit ctx: Context): Future[Option[MWelcomeRenderArgs]] = {
    // дедубликация кода. Можно наверное через Future.filter такое отрабатывать.
    def _colorBg = colorBg(mnode)

    // Получить параметры (метаданные) фоновой картинки из хранилища картирок.
    val bgFut = mnode.edges
      .withPredicateIter( MPredicates.GalleryItem )
      .toStream
      .headOption
      .fold[Future[Either[String, MImgWithWhInfo]]] {
        Future.successful(_colorBg)
      } { bgEdge =>
        val dynImgId = MDynImgId(
          rowKeyStr = bgEdge.nodeIds.head,
          dynFormat = bgEdge.info.dynImgArgs.fold[MImgFmt](MImgFmts.JPEG)(_.dynFormat),
          dynImgOps = Nil  //TODO Реализовать парсинг операций. Пока это не используется, но всё равно надо бы: bgEdge.info.dynImgArgs.flatMap(_.dynOpsStr)
        )
        val oiik = MImg3(dynImgId)
        val fut0 = mImgs3.getImageWH( oiik.original )
        lazy val logPrefix = s"getWelcomeRenderArgs(${mnode.idOrNull}):"
        fut0.map {
          case Some(meta) =>
            Right(bgCallForScreen(oiik, screen, meta))
          case _ =>
            trace(s"$logPrefix no welcome bg WH for $bgEdge")
            colorBg(mnode)
        }
        .recover { case ex: Throwable =>
          error(s"$logPrefix Failed to read welcome image data", ex)
          _colorBg
        }
      }

    val fgImgOpt = wcLogoImg(mnode)
    val fgMetaOptFut = FutureUtil.optFut2futOpt(fgImgOpt) { fgImg =>
      mImgs3.getImageWH( fgImg )
    }
    val fgOptFut = for (fgMetaOpt <- fgMetaOptFut) yield {
      for (fgImg <- fgImgOpt; fgMeta <- fgMetaOpt) yield {
        MImgWithWhInfo(fgImg, fgMeta)
      }
    }

    for {
      bg1   <- bgFut
      fgOpt <- fgOptFut
    } yield {
      val wra = MWelcomeRenderArgs(
        bg      = bg1,
        fgImage = fgOpt,
        fgText  = Some( mnode.meta.basic.name )
      )
      Some(wra)
    }
  }


  /** Собрать ссылку на фоновую картинку. */
  private def bgCallForScreen(oiik: MImgT, screenOpt: Option[DevScreen], origMeta: ISize2di)
                             (implicit ctx: Context): MImgWithWhInfo = {
    val oiik2 = oiik.original
    screenOpt
      .flatMap { scr =>
        scr.maybeBasicScreenSize.map(_ -> scr)
      }
      .fold {
        MImgWithWhInfo(oiik, origMeta)
      } { case (bss, screen) =>
        val imOps = imConvertArgs(bss, screen)
        val dynArgs = oiik2.withDynOps(imOps)
        MImgWithWhInfo(dynArgs, bss)
      }
  }

  /** Сгенерить аргументы для генерации dyn-img ссылки. По сути, тут список параметров для вызова convert.
    * @param scrSz Размер конечной картинки.
    * @return Список ImOp в прямом порядке.
    */
  private def imConvertArgs(scrSz: BasicScreenSize, screen: DevScreen): Seq[ImOp] = {
    val gravity = ImGravities.Center
    val acc0: List[ImOp] = Nil
    val bgc = screen.pixelRatio.bgCompression
    val acc1 = gravity ::
      AbsResizeOp(scrSz, Seq(ImResizeFlags.FillArea)) ::
      gravity ::
      ExtentOp(scrSz) ::
      StripOp ::
      bgc.imQualityOp ::
      ImInterlaces.Plane ::
      bgc.chromaSubSampling ::
      acc0
    bgc
      .imGaussBlurOp
      .fold(acc1)(_ :: acc1)
  }

  /** внутренний метод для генерации ответа по фону приветствия в режиме цвета. */
  private def colorBg(adnNode: MNode) = {
    val bgColor = adnNode.meta.colors.bg.fold(scUtil.SITE_BGCOLOR_DFLT)(_.code)
    Left(bgColor)
  }

}
