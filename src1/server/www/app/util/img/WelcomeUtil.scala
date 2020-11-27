package util.img

import javax.inject.{Inject, Singleton}
import io.suggest.common.fut.FutureUtil
import io.suggest.dev.MScreen
import io.suggest.img.{MImgFormat, MImgFormats}
import io.suggest.n2.node.MNode
import io.suggest.util.logs.MacroLogsImpl
import models.im._
import models.mctx.Context
import models.mproj.ICommonDi
import models.mwc.MWelcomeRenderArgs
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
                              mImgs3                 : MImgs3,
                              mCommonDi              : ICommonDi
                            )
  extends MacroLogsImpl
{

  import LOGGER._
  import mCommonDi._


  /**
   * Извлечь логотип карточки приветствия.
   * @param mnode Отрабатываемый узел.
   * @return Опциональная картинка.
   */
  def wcFgImg(mnode: MNode): Option[MImgT] = {
    mnode.Quick.Adn.wcFg
      .map { case (jdId, e) =>
        MImg3( MDynImgId.fromJdEdge(jdId, e) )
      }
  }


  /**
   * Асинхронно собрать аргументы для рендера карточки приветствия.
   * @param mnode Узел, для которого нужно подготовить настройки рендера приветствия.
   * @param screen Настройки экрана, если есть.
   * @return Фьючерс с опциональными настройками. Если None, то приветствие рендерить не надо.
   */
  def getWelcomeRenderArgs(mnode: MNode, screen: Option[MScreen])
                          (implicit ctx: Context): Future[Option[MWelcomeRenderArgs]] = {
    // дедубликация кода. Можно наверное через Future.filter такое отрабатывать.
    def _colorBg = colorBg(mnode)
    lazy val logPrefix = s"getWelcomeRenderArgs(${mnode.idOrNull}):"

    // Получить параметры (метаданные) фоновой картинки из хранилища картирок.
    val bgFut = mnode.Quick.Adn.galImgs
      .headOption
      .fold[Future[Either[String, MImgWithWhInfo]]] {
        Future.successful(_colorBg)

      } { case (jdId, bgEdge) =>
        val dynImgId = MDynImgId.fromJdEdge(jdId, bgEdge)
        val mimg = MImg3(dynImgId)
        val mimgOrig = mimg.original
        val fut0 = mImgs3.getImageWH( mimgOrig )
        fut0.map {
          case Some(meta) =>
            val r = screen
              .flatMap { scr =>
                BasicScreenSizes
                  .includesSize( scr.wh )
                  .map(_ -> scr)
              }
              .fold {
                LOGGER.debug(s"$logPrefix No screen sz info for $screen. Returning original img.")
                MImgWithWhInfo(mimg, meta)
              } { case (bss, screen) =>
                val dynArgs = if (dynImgId.imgFormat.exists(_.isVector)) {
                  // Для svg - вернуть оригинал.
                  mimgOrig
                } else {
                  val outFmt = MImgFormats.JPEG
                  val imOps = bgImConvertArgs(outFmt, bss, screen)
                  mimgOrig.withDynImgId(
                    mimgOrig.dynImgId.copy(
                      imgFormat = Some( outFmt ),
                      imgOps    = imOps,
                    )
                  )
                }

                MImgWithWhInfo(dynArgs, bss)
              }
            Right(r)
          case _ =>
            trace(s"$logPrefix no welcome bg WH for $bgEdge")
            colorBg(mnode)
        }
        .recover { case ex: Throwable =>
          error(s"$logPrefix Failed to read welcome image data", ex)
          _colorBg
        }
      }

    val fgImgOpt = wcFgImg(mnode)
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


  /** Сгенерить аргументы для генерации dyn-img ссылки. По сути, тут список параметров для вызова convert.
    * @param scrSz Размер конечной картинки.
    * @return Список ImOp в прямом порядке.
    */
  private def bgImConvertArgs(outFmt: MImgFormat, scrSz: BasicScreenSize, screen: MScreen): Seq[ImOp] = {
    val gravity = ImGravities.Center
    val bgc = ImCompression.forPxRatio( CompressModes.Bg, screen.pxRatio )

    var acc =
      StripOp ::
      ImInterlaces.Plane ::
      bgc.toOps( outFmt )

    for (b <- bgc.blur) {
      acc ::= b
    }

    acc = gravity ::
      BackgroundOp( None ) ::
      AbsResizeOp(scrSz, Seq(ImResizeFlags.FillArea)) ::
      gravity ::
      ExtentOp(scrSz) ::
      acc

    acc
  }

  /** внутренний метод для генерации ответа по фону приветствия в режиме цвета. */
  private def colorBg(adnNode: MNode) = {
    val bgColor = adnNode.meta.colors.bg.fold(scUtil.SITE_BGCOLOR_DFLT)(_.code)
    Left(bgColor)
  }

}
