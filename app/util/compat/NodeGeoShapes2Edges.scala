package util.compat

import com.google.inject.Inject
import io.suggest.model.n2.edge._
import io.suggest.model.n2.node.MNodeTypes
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.util.JMXBase
import models.MNode
import models.mproj.ICommonDi

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.03.16 18:50
  * Description: Портирование node.geo.shapes в эджи.
  */

class NodeGeoShapes2Edges @Inject() (
  mCommonDi   : ICommonDi
) {

  import mCommonDi._

  def doIt(): Future[Int] = {

    val search = new MNodeSearchDfltImpl {
      // Типы узлов, на момент запуска работа касалась только узлов-кабинетов.
      override def nodeTypes = Seq( MNodeTypes.AdnNode )
    }

    val scroller = MNode.startScroll(
      queryOpt = search.toEsQueryOpt
    )

    // Запускаем обход модели узлов...
    MNode.updateAll(scroller) { mnode0 =>
      val mnode1 = if (mnode0.geo.shapes.isEmpty) {
        // Нет шейпов -- нет работы.
        null

      } else {

        // Есть шейпы, перенести их всех в эдж.
        mnode0.copy(
          edges = mnode0.edges.copy(
            out = {
              // Считаем, что этот код исполняется только один раз и первый раз. Без лишних проверок добавляем эдж.
              val e = MEdge(
                predicate = MPredicates.NodeLocation,
                nodeIdOpt = None,
                info = MEdgeInfo(
                  geoShapes = {
                    mnode0.geo.shapes
                      .iterator
                      .map(_.toEdgeGs)
                      .toList
                  }
                )
              )
              MNodeEdges.edgesToMap1(
                mnode0.edges.iterator ++ Iterator(e)
              )
            }
          ),
          // Удалить эджи из старого расположения
          geo = mnode0.geo.copy(
            shapes = Nil
          )
        )
      }

      Future.successful(mnode1)
    }
  }

}


trait NodeGeoShapes2EdgesJmxMBean {
  def doIt(): String
}
class NodeGeoShapes2EdgesJmx @Inject() (
  ngs2eUtil : NodeGeoShapes2Edges,
  mCommonDi : ICommonDi
)
  extends JMXBase
  with NodeGeoShapes2EdgesJmxMBean
{

  import mCommonDi._

  override def jmxName: String = "io.suggest:type=compat,name=" + getClass.getSimpleName.replace("Jmx", "")

  override def doIt(): String = {
    val fut = for (countUpdated <- ngs2eUtil.doIt()) yield {
      s"Updated $countUpdated nodes."
    }
    awaitString(fut)
  }
}

