package util.mdr

import com.google.inject.Inject
import io.suggest.model.n2.edge.{MPredicates, MEdgeInfo, MEdge, MNodeEdges}
import models.MNode
import models.mdr.RefuseForm_t
import models.mproj.ICommonDi
import models.req.{IReqHdr, IAdReq}
import org.joda.time.DateTime
import util.PlayMacroLogsImpl

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.03.16 11:13
  * Description: Всякая утиль для контроллера [[controllers.SysMdr]]: формы, фунции и прочее.
  */
class SysMdrUtil @Inject()(
  mCommonDi         : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import mCommonDi._

  def someNow = Some(DateTime.now)

  /** Сборка эджа от текущего модератора.
    *
    * @param reasonOpt None значит всё хорошо,
    *                  Some() значит карточка не прошла модерацию по указанной причине.
    * @return Готовый экземляр MEdge
    */
  def mdrEdge(reasonOpt: Option[String] = None)(implicit request: IReqHdr): MEdge = {
    mdrEdgeI {
      MEdgeInfo(
        dateNi    = someNow,
        commentNi = reasonOpt,
        flag      = Some(reasonOpt.isEmpty)
      )
    }
  }

  /** Сборка эджа текущего модератора с указанной инфой по модерации. */
  def mdrEdgeI(einfo: MEdgeInfo)(implicit request: IReqHdr): MEdge = {
    MEdge(
      nodeId    = request.user.personIdOpt.get,
      predicate = MPredicates.ModeratedBy,
      info      = einfo
    )
  }

  /** Код обновления эджа модерации живёт здесь. */
  def updMdrEdge(info: MEdgeInfo)(implicit request: IAdReq[_]): Future[MNode] = {
    // Сгенерить обновлённые данные модерации.
    val mdr2 = mdrEdgeI(info)

    LOGGER.trace(s"_updMdrEdge() Mdr mad[${request.mad.idOrNull}] with mdr-edge $mdr2")

    // Запускаем сохранение данных модерации.
    MNode.tryUpdate(request.mad) { mad0 =>
      mad0.copy(
        edges = mad0.edges.copy(
          out = {
            val iter0 = mad0.edges.withoutPredicateIter( MPredicates.ModeratedBy )
            val iter2 = Iterator(mdr2)
            MNodeEdges.edgesToMap1(iter0 ++ iter2)
          }
        )
      )
    }
  }


  import play.api.data._
  import Forms._
  import util.FormUtil._


  def reasonM: Mapping[String] = {
    nonEmptyText(minLength = 4, maxLength = 1024)
      .transform(strTrimSanitizeF, strIdentityF)
  }

  def reasonKM = "reason" -> reasonM

   /** Маппинг минимальной refuse-формы. */
  def refuseFormM: RefuseForm_t = {
    Form( reasonKM )
  }

}


trait ISysMdrUtilDi {
  def sysMdrUtil: SysMdrUtil
}
