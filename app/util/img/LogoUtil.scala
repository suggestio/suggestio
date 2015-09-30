package util.img

import io.suggest.sc.ScConstants
import io.suggest.ym.model.common.{LogoImgOptI, MImgInfoMeta, MImgInfoT}
import models.im._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import models.blk.szMulted

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.10.14 18:37
 * Description: Утиль для работы с логотипами. Исторически, она была разбросана по всему проекту.
 */
object LogoUtil {

  type LogoOpt_t = Option[MImgT]

  def updateLogo(newLogo: LogoOpt_t, oldLogo: LogoOpt_t): Future[Option[MImgInfoT]] = {
    val oldImgs = oldLogo
      .map { ii => MImg(ii.fileName) }
      .toIterable
    ImgFormUtil.updateOrigImgFull(needImgs = newLogo.toSeq, oldImgs = oldImgs)
      .flatMap { vs => ImgFormUtil.optImg2OptImgInfo( vs.headOption ) }
  }

  /** Доставание картинки логотипа без подгонки под свойства экрана. */
  def getLogo(mnode: LogoImgOptI): Future[Option[MImgT]] = {
    val res = mnode.logoImgOpt.map { logoInfo =>
      MImg(logoInfo)
    }
    Future successful res
  }

  /**
   * Подготовка логотипа выдачи для узла.
   * @param mnode Узел, хранящий в себе данные по логотипу.
   * @param screenOpt Данные по экрану клиента, если есть.
   * @return Фьючерс с картинкой, если логотип задан.
   */
  def getLogo4scr(mnode: LogoImgOptI, screenOpt: Option[DevScreen]): Future[Option[MImgT]] = {
    // Код метода синхронный, но, как показывает практика, лучше сразу сделать асинхрон, чтобы потом всё не перепиливать.
    getLogo(mnode).map { mimgOpt =>
      mimgOpt.map { mimg0 =>
        // Узнаём pixelRatio для дальнейших рассчетов.
        val pxRatio = screenOpt.flatMap(_.pixelRatioOpt)
          .getOrElse(DevPixelRatios.default)

        // Исходя из pxRatio нужно посчитать высоту логотипа
        val heightCssPx = ScConstants.Logo.HEIGHT_CSSPX
        val heightPx = szMulted(heightCssPx, pxRatio.pixelRatio)

        // Вернуть скомпленную картинку.
        mimg0.withDynOps(
          Seq(
            AbsResizeOp(MImgInfoMeta(heightPx, width = 0)),
            StripOp,
            pxRatio.fgCompression.imQualityOp
          )
        )
      }
    }
  }

}
