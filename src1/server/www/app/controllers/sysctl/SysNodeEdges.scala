package controllers.sysctl

import controllers.{SioController, routes}
import io.suggest.model.es.MEsUuId
import io.suggest.model.n2.edge.{MEdge, MNodeEdges}
import io.suggest.model.n2.node.IMNodes
import models.msys.MNodeEdgeIdQs
import models.req.{INodeEdgeReq, INodeReq}
import play.api.data.Form
import play.api.mvc.Result
import util.PlayMacroLogsI
import util.acl.{IsSuNode, IsSuNodeEdge}
import util.sys.ISysMarketUtilDi
import views.html.sys1.market.edge._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.10.16 16:19
  * Description: Аддон для контроллера [[controllers.SysMarket]] с экшенами управления
  * эджами узлов.
  *
  * В экшенах активно используется es-версия узла в качестве параметра.
  * Порядок эджей нарушается до неузнаваемости при каждом
  */
trait SysNodeEdges
  extends SioController
  with PlayMacroLogsI
  with IsSuNode
  with IsSuNodeEdge
  with IMNodes
  with ISysMarketUtilDi
{

  import mCommonDi._

  /**
    * Сабмит формы-кнопки удаления эджа из узла.
    * @return Редирект, если всё ок.
    */
  def deleteEdgePost(qs: MNodeEdgeIdQs) = IsSuNodeEdgePost(qs).async { implicit request =>
    LOGGER.trace(s"deleteEdgePost($qs): Deleting edge ${request.medge} of node '''${request.mnode.guessDisplayNameOrIdOrEmpty}'''")

    val mnode2 = request.mnode.withEdges(
      request.mnode.edges.copy(
        out = MNodeEdges.edgesToMap1(
          request.mnode.edges
            .iterator
            .filter { e => e ne request.medge }
        )
      )
    )

    // Сохранить собранный эдж.
    for {
      _ <- mNodes.save(mnode2)
    } yield {
      Redirect( routes.SysMarket.showAdnNode(qs.nodeId) )
        .flashing( FLASH.SUCCESS -> s"Удалён эдж #${qs.edgeId} из узла '''${request.mnode.guessDisplayNameOrIdOrEmpty}'''." )
    }
  }


  /** Экшен запроса страницы с формой создания нового эджа на указанном узле. */
  def createEdgeGet(nodeId: MEsUuId) = IsSuNodeGet(nodeId).async { implicit request =>
    val form = sysMarketUtil.edgeFormM
    _createEdgeBody(Ok, form)
  }

  /** Рендер страницы создания эджа на узле. */
  private def _createEdgeBody(rs: Status, form: Form[MEdge])(implicit request: INodeReq[_]): Future[Result] = {
    rs( createEdgeTpl(form, request.mnode) )
  }

  /** Сабмит формы создания нового эджа на узле. */
  def createEdgePost(nodeId: MEsUuId) = IsSuNodeGet(nodeId).async { implicit request =>
    def logPrefix = s"createEdgePost(${nodeId.id}):"

    sysMarketUtil.edgeFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"$logPrefix Failed to bind create form: ${formatFormErrors(formWithErrors)}")
        _createEdgeBody(NotAcceptable, formWithErrors)
      },
      {medge =>
        LOGGER.trace(s"$logPrefix Creating edge $medge on node ''${request.mnode.guessDisplayNameOrIdOrEmpty}''")
        val saveFut = mNodes.tryUpdate(request.mnode) { mnode =>
          mnode.withEdges(
            mnode.edges.copy(
              out = mnode.edges.out ++ Seq(medge)
            )
          )
        }
        for (_ <- saveFut) yield {
          Redirect( routes.SysMarket.showAdnNode(nodeId) )
            .flashing( FLASH.SUCCESS -> s"Создан новый эдж на узле ''${request.mnode.guessDisplayNameOrIdOrEmpty}''." )
        }
      }
    )
  }


  /** Экшен запроса страницы с формой редактирования эджа на узле. */
  def editEdgeGet(qs: MNodeEdgeIdQs) = IsSuNodeEdgeGet(qs).async { implicit request =>
    val eform = sysMarketUtil.edgeFormM
      .fill( request.medge )
    _editEdgeBody(qs, Ok, eform)
  }

  private def _editEdgeBody(qs: MNodeEdgeIdQs, rs: Status, ef: Form[MEdge])
                           (implicit request: INodeEdgeReq[_]): Future[Result] = {
    rs( editEdgeTpl(qs, ef, request.mnode) )
  }

  /** Экшен сабмита формы редактирования эджа. */
  def editEdgePost(qs: MNodeEdgeIdQs) = IsSuNodeEdgePost(qs).async { implicit request =>
    def logPrefix = s"editEdgePost($qs):"
    sysMarketUtil.edgeFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"$logPrefix Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        _editEdgeBody(qs, NotAcceptable, formWithErrors)
      },
      {medge2 =>
        LOGGER.trace(s"$logPrefix Update of edge ${request.medge} using $medge2 on node ${request.mnode.guessDisplayNameOrIdOrEmpty}")

        // Заменить эдж в инстансе узла.
        val mnode2 = request.mnode.withEdges(
          request.mnode.edges.copy(
            MNodeEdges.edgesToMap1(
              request.mnode.edges
                .withIndexUpdated( qs.edgeId ) { e0 =>
                  Seq(
                    sysMarketUtil.updateEdge(e0, medge2)
                  )
                }
            )
          )
        )

        // Запустить сохранение
        for {
          _ <- mNodes.save(mnode2)
        } yield {
          // Отредиректить на sys-страницу узла.
          Redirect( routes.SysMarket.showAdnNode(qs.nodeId) )
            .flashing( FLASH.SUCCESS -> s"Обновлён эдж #${qs.edgeId} на узле ''${request.mnode.guessDisplayNameOrIdOrEmpty}''." )
        }
      }
    )
  }

}
