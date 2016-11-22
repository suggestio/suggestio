package util.adv.geo

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.ym.model.common.AdnRights
import models.adv.geo.mapf.{MAdvGeoMapNode, MAdvGeoMapNodeProps}
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.11.16 15:01
  * Description:
  */
class AdvGeoMapUtil @Inject() (
  mNodes      : MNodes,
  mCommonDi   : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import mCommonDi._
  import mNodes.Implicits._



  /** Карта ресиверов, размещённых через lk-adn-map.
    *
    * @return Фьючерс с GeoJSON.
    */
  def rcvrNodesMap(): Source[MAdvGeoMapNode, NotUsed] = {
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

    nodesSource
      // Отмаппить узлы в представление, годное для GeoJSON-сериализации. Финальную сериализацию организует контроллер.
      .mapConcat { mnode =>
        val props = MAdvGeoMapNodeProps(
          nodeId    = mnode.id.get,
          hint      = mnode.guessDisplayName,
          bgColor   = mnode.meta.colors.bg
            .map(_.code)
          //iconUrl   = None // TODO Грабить логотип узла асинхронно перед этой функцией.
        )
        mnode.edges
          .withPredicateIter( MPredicates.AdnMap )
          .flatMap(_.info.geoPoints)
          .map { geoPoint =>
            MAdvGeoMapNode(
              point = geoPoint,
              props = props
            )
          }
          .toStream   // Это типа toImmutableIterable
      }
  }


}

