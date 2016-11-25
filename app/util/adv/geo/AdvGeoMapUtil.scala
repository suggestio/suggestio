package util.adv.geo

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import io.suggest.common.fut.FutureUtil
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.ym.model.common.AdnRights
import models.ISize2di
import models.adv.geo.mapf.{MAdvGeoMapNode, MAdvGeoMapNodeProps, MIconInfo}
import models.im.{MAnyImgs, MImgT}
import models.mctx.Context
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl
import util.cdn.CdnUtil
import util.img.{DynImgUtil, LogoUtil}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.11.16 15:01
  * Description:
  */
class AdvGeoMapUtil @Inject() (
  mNodes      : MNodes,
  logoUtil    : LogoUtil,
  cdnUtil     : CdnUtil,
  mAnyImgs    : MAnyImgs,
  dynImgUtil  : DynImgUtil,
  mCommonDi   : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import mCommonDi._
  import mNodes.Implicits._


  /**
    * Максимальное распараллелнивание при сборке логотипов для узлов.
    * Сборка логотипа в основном синхронная, поэтому можно распараллеливаться по-сильнее.
    */
  private def NODE_LOGOS_PREPARING_PARALLELISM = 16

  /** Размер логотипа (по высоте) на карте. */
  private val LOGO_HEIGHT_CSSPX = configuration.getInt("node.logo.on.map.height.px").getOrElse(20)


  private case class LogoInfo(logo: MImgT, wh: ISize2di)
  private case class NodeInfo(mnode: MNode, logoInfoOpt: Option[LogoInfo])

  /** Карта ресиверов, размещённых через lk-adn-map.
    *
    * @return Фьючерс с GeoJSON.
    */
  def rcvrNodesMap()(implicit ctx: Context): Source[MAdvGeoMapNode, NotUsed] = {
    // Ищем все ресиверы, размещённые на карте.
    val msearch = new MNodeSearchDfltImpl {
      override def outEdges: Seq[ICriteria] = {
        val cr = Criteria(
          predicates = Seq( MPredicates.AdnMap )
        )
        Seq(cr)
      }
      override def nodeTypes = Seq( MNodeTypes.AdnNode )
      override def withAdnRights = Seq( AdnRights.RECEIVER )
      // Это кол-во результатов за одну порцию скроллинга.
      override def limit = 30
    }

    // Начать выкачивать все подходящие узлы из модели:
    val nodesSource = mNodes.source[MNode](msearch)
    // TODO Opt направить этот поток в кэш узлов MNodeCache?

    lazy val logPrefix = s"rcvrNodesMap(${System.currentTimeMillis}):"

    val logosAndNodeSrc = nodesSource.mapAsyncUnordered(NODE_LOGOS_PREPARING_PARALLELISM) { mnode =>
      val logoInfoOptFut = logoUtil.getLogoOfNode(mnode).flatMap { logoOptRaw =>
        FutureUtil.optFut2futOpt(logoOptRaw) { logoRaw =>
          val fut = for {
            logo      <- logoUtil.getLogo4scr(logoRaw, LOGO_HEIGHT_CSSPX, None)
            localImg  <- dynImgUtil.ensureImgReady(logo, cacheResult = true)
            whOpt     <- mAnyImgs.getImageWH(localImg)
          } yield {
            whOpt.fold[Option[LogoInfo]] {
              LOGGER.warn(s"Unable to fetch WH of logo $logo for node ${mnode.idOrNull}")
              None
            } { wh =>
              Some(LogoInfo(logo, wh))
            }
          }

          // Подавлять ошибки рендера логотипа. Дефолтовщины хватит, главное чтобы всё было ок.
          fut.recover { case ex: Throwable =>
            LOGGER.error(s"$logPrefix Node[${mnode.idOrNull}] with possible logo[$logoRaw] failed to prepare the logo for map", ex)
            None
          }
        }
      }

      // Завернуть результат работы в итоговый контейнер, используемый вместо трейта.
      for (logoInfoOpt <- logoInfoOptFut) yield {
        NodeInfo(mnode, logoInfoOpt)
      }
    }

    // Отмаппить узлы в представление, годное для GeoJSON-сериализации. Финальную сериализацию организует контроллер.
    logosAndNodeSrc.mapConcat { nodeInfo =>
      // Собираем url отдельно и ровно один раз, чтобы сэкономить ресурсы.
      val iconInfoOpt = for (logoInfo <- nodeInfo.logoInfoOpt) yield {
        MIconInfo(
          url    = cdnUtil.dynImg(logoInfo.logo).url,
          width  = logoInfo.wh.width,
          height = logoInfo.wh.height
        )
      }
      val mnode = nodeInfo.mnode

      val props = MAdvGeoMapNodeProps(
        nodeId  = mnode.id.get,
        hint    = mnode.guessDisplayName,
        bgColor = mnode.meta.colors.bg
          .map(_.code),
        icon    = iconInfoOpt
      )

      mnode.edges
        .withPredicateIter( MPredicates.AdnMap )
        .flatMap( _.info.geoPoints )
        .map { geoPoint =>
          MAdvGeoMapNode(
            point = geoPoint,
            props = props
          )
        }
        .toStream // Это типа toImmutableIterable
    }
  }


}

