package util.compat

import java.util.concurrent.atomic.AtomicInteger

import akka.event.Logging.LoggerInitialized
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.geo.MNodeGeo
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.util.JMXBase
import io.suggest.util.logs.MacroLogsImplLazy
import javax.inject.Inject
import models.mproj.ICommonDi
import japgolly.univeq._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.18 20:04
  * Description: Мигратор удаляемой модели MNodeGeo в эджи для
  */
class NodeGeo2EdgeMigrate @Inject()(
                                       mNodes     : MNodes,
                                       mCommonDi  : ICommonDi
                                     )
  extends MacroLogsImplLazy
{

  import mCommonDi._
  import mNodes.Implicits._

  def doIt(): Future[Int] = {
    val bpListener = new mNodes.BulkProcessorListener(getClass.getSimpleName)
    val bp = mNodes.bulkProcessor(bpListener)

    val msearch = new MNodeSearchDfltImpl {
      override def nodeTypes = MNodeTypes.AdnNode :: Nil
    }

    val p = MPredicates.NodeLocation
    val counter = new AtomicInteger(0)

    mNodes
      .source[MNode](msearch)
      .filter { mnode =>
        mnode.geo.point.nonEmpty && {
          val hasEdge = mnode.edges.withPredicateIter(MPredicates.NodeLocation).nonEmpty
          if (!hasEdge)
            LOGGER.warn(s"node#${mnode.idOrNull}: No ${p}-edge, but point ${mnode.geo.point.orNull} defined")
          hasEdge
        }
      }
      .runForeach { mnode0 =>
        counter.incrementAndGet()

        val gps = mnode0.geo.point.toSeq
        val mnode2 = mnode0.copy(
          geo = MNodeGeo.empty,
          edges = mnode0.edges.withOut(
            for (e <- mnode0.edges.out) yield {
              if (e.predicate ==* p) {
                // Залить точку из mnode.geo в e.info:
                e.withInfo(
                  e.info.withGeoPoints( gps )
                )
              } else {
                e
              }
            }
          )
        )
        bp.add( mNodes.prepareIndex(mnode2).request() )
      }
      .map { _ =>
        bp.close()
        counter.get()
      }
  }

}

trait MNodeGeo2EdgeMigrateJmxMBean {
  def doIt(): String
}
final class MNodeGeo2EdgeMigrateJmx @Inject() (
                                                nodeGeo2EdgeMigrate   : NodeGeo2EdgeMigrate,
                                                override implicit val ec       : ExecutionContext
                                              )
  extends JMXBase
  with MNodeGeo2EdgeMigrateJmxMBean
  with MacroLogsImplLazy
{
  override def jmxName = "io.suggest:type=compat,name=" + classOf[NodeGeo2EdgeMigrate].getSimpleName

  override def doIt(): String = {
    val strFut = for (countProcessed <- nodeGeo2EdgeMigrate.doIt()) yield {
      s"done, ${countProcessed} re-saved"
    }
    awaitString(strFut)
  }
}
