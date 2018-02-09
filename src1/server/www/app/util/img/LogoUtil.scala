package util.img

import javax.inject.{Inject, Singleton}

import io.suggest.common.empty.EmptyUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.model.n2.edge.{MEdge, MPredicates}
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
  imgFormUtil               : ImgFormUtil,
  implicit private val ec   : ExecutionContext
) {

  /** Приведение ребра графа к метаданным изображения логотипа. */
  def edge2logoImg(medge: MEdge): MImgT = {
    MImg3( MDynImgId(medge.nodeIds.head, Nil) )
  }

  // TODO Допилить этот метод, привязать его к контроллеру, разобраться с MImg.deleteAllFor(UUID), обновить маппинги форм.
  def updateLogoFor(mnode: MNode, newLogo: LogoOpt_t): Future[Option[MImgT]] = {
    val oldImgs = mnode.edges
      .withPredicateIter( MPredicates.Logo )
      .map { edge2logoImg }
      .toIterable
    val newLogosFut = imgFormUtil.updateOrigImgFull(
      needImgs  = newLogo.toSeq,
      oldImgs   = oldImgs
    )
    newLogosFut
      .map { _.headOption }
  }


  /** Получить логотипы нескольких узлов, вернув карту имеющихся логотипов.
    * Если какого-то запрошенного узла нет в карте, то значит он без логотипа. */
  def getLogoOfNodes(nodes: TraversableOnce[MNode]): Future[Map[String, MImgT]] = {
    val res = nodes.toIterator
      .flatMap { mnode =>
        val eopt = mnode.edges
          .withPredicateIter( MPredicates.Logo )
          .toStream
          .headOption
        for {
          e     <- eopt
          id    <- mnode.id
        } yield {
          id -> edge2logoImg(e)
        }
      }
      .toMap
    Future successful res
  }

  /**
   * Доставание картинки логотипа без подгонки под свойства экрана.
   * @param mnode Узел, к которому прилинкован логотип.
   * @return Фьючерс с результатом: None -- логотип не выставлен.
   */
  def getLogoOfNode(mnode: MNode): Future[LogoOpt_t] = {
    val res = mnode.edges
      .withPredicateIter( MPredicates.Logo )
      .toStream
      .headOption
      .map { edge2logoImg }
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

    // Вернуть скомпленную картинку.
    val logoImg2 = logoImg.withDynOps(
      Seq(
        AbsResizeOp(MSize2di(height = heightPx, width = 0)),
        StripOp,
        pxRatio.fgCompression.imQualityOp
      )
    )
    Future.successful( logoImg2 )
  }

}
