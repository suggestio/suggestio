package util.img

import javax.inject.Inject
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.dev.{MPxRatios, MScreen}
import io.suggest.n2.node.MNode
import io.suggest.sc.ScConstants
import models.blk._
import models.im._
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.10.14 18:37
 * Description: Утиль для работы с логотипами. Исторически, она была разбросана по всему проекту.
 */
class LogoUtil @Inject() (
                           injector: Injector,
                         ) {

  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  /**
    * Доставание картинки логотипа без подгонки под свойства экрана.
   * @param mnode Узел, к которому прилинкован логотип.
   * @return Фьючерс с результатом: None -- логотип не выставлен.
   */
  def getLogoOfNode(mnode: MNode): Option[MImgT] = {
    mnode.Quick.Adn.logo
      .map { case (jdId, e) =>
        MImg3( MDynImgId.fromJdEdge(jdId, e) )
      }
  }

  /**
   * Подготовка логотипа выдачи для узла.
   * @param logoOpt Текущей логотип узла, если есть. Результат вызова getLogo().
   * @param screenOpt Данные по экрану клиента, если есть.
   * @return Фьючерс с картинкой, если логотип задан.
   */
  def getLogoOpt4scr(logoOpt: Option[MImgT], screenOpt: Option[MScreen]): Future[Option[MImgT]] = {
    logoOpt.fold [Future[Option[MImgT]]] {
      Future successful None
    } { logoImg =>
      getLogoOpt4scr(logoImg, screenOpt)
    }
  }
  def getLogoOpt4scr(logoImg: MImgT, screenOpt: Option[MScreen]): Future[Option[MImgT]] = {
    getLogo4scr(logoImg, screenOpt)
      .map { EmptyUtil.someF }
  }

  def getLogo4scr(logoImg: MImgT, screenOpt: Option[MScreen]): Future[MImgT] = {
    val outFmt = logoImg.dynImgId.imgFormat.get

    if (outFmt.isVector) {
      // Для SVG надо просто вернуть svg.
      Future.successful( logoImg.original )

    } else {
      // Для растра - подготовить размеры картинки:
      val heightCssPx = ScConstants.Logo.HEIGHT_CSSPX

      // Узнаём pixelRatio для дальнейших рассчетов.
      val pxRatio = screenOpt
        .map(_.pxRatio)
        .getOrElse(MPxRatios.default)

      // Код метода синхронный, но, как показывает практика, лучше сразу сделать асинхрон, чтобы потом всё не перепиливать.
      // Исходя из pxRatio нужно посчитать высоту логотипа
      val heightPx = szMulted(heightCssPx, pxRatio.pixelRatio)

      // Вернуть скомпленную картинку.
      val logoImg2 = logoImg.withDynOps(
        AbsResizeOp( MSize2di(height = heightPx, width = 0) ) ::
          StripOp ::
          ImCompression.forPxRatio( CompressModes.Fg, pxRatio)
            .toOps( outFmt )
      )
      Future.successful( logoImg2 )
    }
  }

  // TODO Это забор getLogo4scr надо выпилить. Есть FitImgMaker, его и надо юзать.

}
