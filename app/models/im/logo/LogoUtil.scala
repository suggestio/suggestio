package models.im.logo

import models.MPredicates
import io.suggest.sc.ScConstants
import io.suggest.ym.model.common.MImgInfoMeta
import models.IEdge
import models.blk._
import models.im._
import play.api.Play._
import util.img.ImgFormUtil
import util.xplay.CacheUtil
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.event.SiowebNotifier.Implicts.sn

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.10.14 18:37
 * Description: Утиль для работы с логотипами. Исторически, она была разбросана по всему проекту.
 */
object LogoUtil {

  /** Сколько секунд кешировать результат getLogoOfNode? */
  val GET_NODE_LOGO_CACHE_SECONDS = configuration.getInt("logo.of.node.cache.seconds") getOrElse 10

  /** Приведение ребра графа к метаданным изображения логотипа. */
  def edge2logoImg(medge: IEdge): MImgT = {
    MImg3(medge.nodeId, Nil)
  }

  // TODO Допилить этот метод, привязать его к контроллеру, разобраться с MImg.deleteAllFor(UUID), обновить маппинги форм.
  def updateLogoFor(adnNodeId: String, newLogo: LogoOpt_t): Future[Seq[MImgT]] = {
    /*val edgeSearchArgs = LogoEdgesSearch( adnNodeId )
    for {
      // Найти текущие логотипы через эджи:
      curEdges   <- MEdge.dynSearch(edgeSearchArgs)
      // Сохранить картинки-логотипы в хранилище.
      newLogos <- {
        val oldImgs = curEdges
          .iterator
          .map { edge2logoImg }
          .toIterable
        ImgFormUtil.updateOrigImgFull(needImgs = newLogo.toSeq, oldImgs = oldImgs)
      }
      // Обновить эджи:
      _ <- {
        MEdge.updateEdgesFrom(
          adnNodeId,
          Seq(MPredicates.Logo, MPredicates.Owns),
          oldToIds = curEdges.map(_.toId),
          newToIds = newLogos.map(_.rowKeyStr)
        )
      }
    } yield {
      newLogos
    }*/
    Future successful Nil
  }


  /** Получить логотипы нескольких узлов, вернув карту имеющихся логотипов.
    * Если какого-то запрошенного узла нет в карте, то значит он без логотипа. */
  def getLogoOfNodes(adnNodeIds: Seq[String]): Future[Map[String, MImgT]] = {
    /*val edgeSearchArgs = LogoEdgesSearch(adnNodeIds)
    for (medges <- MEdge.dynSearch(edgeSearchArgs)) yield {
      medges.iterator
        .map { medge =>
          medge.fromId -> edge2logoImg(medge)
        }
        .toMap
    }*/
    Future successful Map.empty
  }

  /**
   * Доставание картинки логотипа без подгонки под свойства экрана.
   * @param adnNodeId id узла, к которому прилинкован логотип.
   * @return Фьючерс с результатом: None -- логотип не выставлен.
   */
  def getLogoOfNode(adnNodeId: String): Future[LogoOpt_t] = {
    /*val edgeSearchArgs = LogoEdgesSearch( adnNodeId )
    for (medgeOpt <- MEdge.dynSearchOne(edgeSearchArgs)) yield {
      medgeOpt.map(edge2logoImg)
    }*/
    Future successful None
  }

  def getLogoOfNodeCached(adnNodeId: String): Future[LogoOpt_t] = {
    CacheUtil.getOrElse(adnNodeId + ".n2lo", GET_NODE_LOGO_CACHE_SECONDS) {
      getLogoOfNode(adnNodeId)
    }
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
      .map { Some.apply }
  }

  def getLogo4scr(logoImg: MImgT, screenOpt: Option[DevScreen]): Future[MImgT] = {
    // Код метода синхронный, но, как показывает практика, лучше сразу сделать асинхрон, чтобы потом всё не перепиливать.
    // Узнаём pixelRatio для дальнейших рассчетов.
    val pxRatio = screenOpt.flatMap(_.pixelRatioOpt)
      .getOrElse(DevPixelRatios.default)

    // Исходя из pxRatio нужно посчитать высоту логотипа
    val heightCssPx = ScConstants.Logo.HEIGHT_CSSPX
    val heightPx = szMulted(heightCssPx, pxRatio.pixelRatio)

    // Вернуть скомпленную картинку.
    val logoImg2 = logoImg.withDynOps(
      Seq(
        AbsResizeOp(MImgInfoMeta(heightPx, width = 0)),
        StripOp,
        pxRatio.fgCompression.imQualityOp
      )
    )
    Future successful logoImg2
  }

}
