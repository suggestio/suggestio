package util.compat

import akka.stream.Materializer
import io.suggest.es.model.{BulkProcessorListener, EsModel}
import io.suggest.n2.node.{MNode, MNodes}
import io.suggest.util.JmxBase
import io.suggest.util.logs.MacroLogsImpl
import models.mcal.MCalendars
import org.elasticsearch.index.query.QueryBuilders
import play.api.inject.Injector

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

final class CalendarsToNodes @Inject() (
                                         esModel      : EsModel,
                                         mCalendars   : MCalendars,
                                         mNodes       : MNodes,
                                         implicit private val mat: Materializer,
                                         implicit private val ec: ExecutionContext,
                                       )
  extends MacroLogsImpl
{

  /** Convert all MCalendars to MNodes. */
  def migrateCalendars(): Future[Int] = {
    import esModel.api._
    import mCalendars.Implicits._

    val logPrefix = "migrateCalendars():"

    val bp = mNodes.bulkProcessor(
      listener = new BulkProcessorListener( logPrefix ),
    )

    val countProcessed = new AtomicInteger(0)

    mCalendars
      .source( QueryBuilders.matchAllQuery() )
      .runForeach { mCalendar =>
        val nodeCalendar = MNode.calendar(
          id      = mCalendar.id,
          name    = mCalendar.name,
          calType = mCalendar.calType,
          data    = mCalendar.data,
        )

        bp.add( mNodes.prepareIndex(nodeCalendar).request() )
        LOGGER.debug(s"$logPrefix Processed calendar#${mCalendar.idOrNull}")

        countProcessed.incrementAndGet()
      }
      .andThen { _ =>
        bp.close()
      }
      .map { _ =>
        countProcessed.get()
      }
  }

}


trait CalendarsToNodesJmxMBean {
  def migrateCalendars(): String
}

final class CalendarsToNodesJmx @Inject() (injector: Injector)
  extends JmxBase
  with CalendarsToNodesJmxMBean
{

  private def calendarsToNodes = injector.instanceOf[CalendarsToNodes]
  implicit private def ec = injector.instanceOf[ExecutionContext]

  override def _jmxType = JmxBase.Types.COMPAT

  override def migrateCalendars(): String = {
    val fut = calendarsToNodes
      .migrateCalendars()
      .map { countProcessed =>
        s"$countProcessed calendars processed."
      }
    JmxBase.awaitString( fut )
  }

}

