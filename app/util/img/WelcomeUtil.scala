package util.img

import com.google.inject.{Inject, Singleton}
import io.suggest.common.fut.FutureUtil
import io.suggest.model.n2.edge.MEdgeInfo
import models._
import models.im._
import models.madn.EditConstants
import models.mproj.ICommonDi
import models.msc.{MWelcomeRenderArgs, WelcomeRenderArgsT}
import util.PlayMacroLogsImpl
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
  mImg3                  : MImgs3,
  mCommonDi              : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  /** Ключ для картинки, используемой в качестве приветствия. */
  // TODO Удалить вслед за старой архитектурой.

  def welcomeImgIdKM = EditConstants.WELCOME_IMG_FN -> ImgFormUtil.img3IdOptM


  /** Обновление картинки и карточки приветствия. Картинка хранится в полу-рекламной карточке, поэтому надо ещё
    * обновить карточку и пересохранить её. */
  def updateWcFgImg(adnNode: MNode, newWelcomeImgOpt: Option[MImgT]): Future[Option[MEdge]] = {
    // Сохранить картинку, вернуть эдж. Нет картинки -- нет эджа.
    FutureUtil.optFut2futOpt(newWelcomeImgOpt) { fgMimg =>
      for (_ <- fgMimg.saveToPermanent) yield {
        val e = MEdge(MPredicates.WcLogo, fgMimg.rowKeyStr, info = MEdgeInfo(
          dynImgArgs = fgMimg.qOpt
        ))
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
      .map { e =>
        mImg3(e)
      }
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
                          (implicit ctx: Context): Future[Option[WelcomeRenderArgsT]] = {
    // дедубликация кода. Можно наверное через Future.filter такое отрабатывать.
    def _colorBg = colorBg(mnode)

    // Получить параметры (метаданные) фоновой картинки из хранилища картирок.
    val bgFut = mnode.edges
      .withPredicateIterIds( MPredicates.GalleryItem )
      .toStream
      .headOption
      .fold[Future[Either[String, IImgWithWhInfo]]] {
        Future successful _colorBg
      } { bgImgFilename =>
        val oiik = mImg3(bgImgFilename)
        val fut0 = oiik.original.getImageWH
        lazy val logPrefix = s"getWelcomeRenderArgs(${mnode.idOrNull}): "
        fut0.map {
          case Some(meta) =>
            Right(bgCallForScreen(oiik, screen, meta))
          case _ =>
            trace(s"getWelcomeRenderArgs(${mnode.idOrNull}): no welcome bg WH for " + bgImgFilename)
            colorBg(mnode)
        }
        .recover { case ex: Throwable =>
          error(logPrefix + "Failed to read welcome image data", ex)
          _colorBg
        }
      }

    val fgImgOpt = wcLogoImg(mnode)
    val fgMetaOptFut = FutureUtil.optFut2futOpt(fgImgOpt) { fgImg =>
      fgImg.getImageWH
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
      ImInterlace.Plane ::
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
