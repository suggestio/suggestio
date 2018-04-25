package util.img

import javax.inject.{Inject, Singleton}

import io.suggest.common.empty.EmptyUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.model.n2.node.MNode
import io.suggest.sc.ScConstants
import models.blk._
import models.im._
import models.im.logo._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.10.14 18:37
 * Description: Утиль для работы с логотипами. Исторически, она была разбросана по всему проекту.
 */
@Singleton
class LogoUtil @Inject() (
                           implicit private val ec   : ExecutionContext
                         ) {

  /**
   * Доставание картинки логотипа без подгонки под свойства экрана.
   * @param mnode Узел, к которому прилинкован логотип.
   * @return Фьючерс с результатом: None -- логотип не выставлен.
   */
  def getLogoOfNode(mnode: MNode): Future[LogoOpt_t] = {
    val res = mnode.Quick.Adn.logo
      .map { case (jdId, e) =>
        MImg3( MDynImgId.fromJdEdge(jdId, e) )
      }
    Future.successful( res )
  }

  /**
   * Подготовка логотипа выдачи для узла.
   * @param logoOpt Текущей логотип узла, если есть. Результат вызова getLogo().
   * @param screenOpt Данные по экрану клиента, если есть.
   * @return Фьючерс с картинкой, если логотип задан.
   */
  def getLogoOpt4scr(logoOpt: LogoOpt_t, screenOpt: Option[DevScreen]): Future[LogoOpt_t] = {
    logoOpt.fold [Future[LogoOpt_t]] {
      Future successful None
    } { logoImg =>
      getLogoOpt4scr(logoImg, screenOpt)
    }
  }
  def getLogoOpt4scr(logoImg: MImgT, screenOpt: Option[DevScreen]): Future[LogoOpt_t] = {
    getLogo4scr(logoImg, screenOpt)
      .map { EmptyUtil.someF }
  }

  def getLogo4scr(logoImg: MImgT, screenOpt: Option[DevScreen]): Future[MImgT] = {
    val heightCssPx = ScConstants.Logo.HEIGHT_CSSPX
    getLogo4scr(logoImg, heightCssPx, screenOpt)
  }
  def getLogo4scr(logoImg: MImgT, heightCssPx: Int, screenOpt: Option[DevScreen]): Future[MImgT] = {
    // Узнаём pixelRatio для дальнейших рассчетов.
    val pxRatio = screenOpt
      .flatMap(_.pixelRatioOpt)
      .getOrElse(DevPixelRatios.default)
    getLogo4scr(logoImg, heightCssPx, pxRatio)
  }
  def getLogo4scr(logoImg: MImgT, heightCssPx: Int, pxRatio: DevPixelRatio): Future[MImgT] = {
    // Код метода синхронный, но, как показывает практика, лучше сразу сделать асинхрон, чтобы потом всё не перепиливать.
    // Исходя из pxRatio нужно посчитать высоту логотипа
    val heightPx = szMulted(heightCssPx, pxRatio.pixelRatio)
    val outFmt = logoImg.dynImgId.dynFormat

    // Вернуть скомпленную картинку.
    val logoImg2 = logoImg.withDynOps(
      AbsResizeOp( MSize2di(height = heightPx, width = 0) ) ::
        StripOp ::
        pxRatio.fgCompression.toOps( outFmt )
    )
    Future.successful( logoImg2 )
  }

  // TODO Это забор getLogo4scr надо выпилить. Есть FitImgMaker, его и надо юзать.

}
