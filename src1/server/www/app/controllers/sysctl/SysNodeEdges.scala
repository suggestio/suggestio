package controllers.sysctl

import controllers.{ISioControllerApi, routes}
import io.suggest.es.model.{EsModelDi, MEsUuId}
import io.suggest.n2.edge.{MEdge, MNodeEdges}
import io.suggest.n2.node.{IMNodes, MNode}
import io.suggest.util.logs.IMacroLogs
import models.msys.MNodeEdgeIdQs
import models.req.{INodeEdgeReq, INodeReq}
import play.api.data.Form
import play.api.mvc.Result
import util.acl.{IIsSuNodeDi, IsSuNodeEdge}
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
  extends ISioControllerApi
  with IMacroLogs
  with IIsSuNodeDi
  with IMNodes
  with ISysMarketUtilDi
  with EsModelDi
{

  import sioControllerApi._
  import mCommonDi.{ec, csrf}
  import esModel.api._

  val isSuNodeEdge: IsSuNodeEdge

  /**
    * Сабмит формы-кнопки удаления эджа из узла.
    * @return Редирект, если всё ок.
    */
  def deleteEdgePost(qs: MNodeEdgeIdQs) = csrf.Check {
    isSuNodeEdge(qs).async { implicit request =>
      LOGGER.trace(s"deleteEdgePost($qs): Deleting edge ${request.medge} of node '''${request.mnode.guessDisplayNameOrIdOrEmpty}'''")

      // Сохранить собранный эдж.
      for {
        _ <- mNodes.tryUpdate( request.mnode )(
          MNode.edges
            .composeLens( MNodeEdges.out )
            .modify { edgesOut0 =>
              MNodeEdges.edgesToMap1(
                edgesOut0
                  .iterator
                  .filter( _ ne request.medge )
              )
            }
        )
      } yield {
        Redirect( routes.SysMarket.showAdnNode(qs.nodeId) )
          .flashing( FLASH.SUCCESS -> s"Удалён эдж #${qs.edgeId} из узла '''${request.mnode.guessDisplayNameOrIdOrEmpty}'''." )
      }
    }
  }


  /** Экшен запроса страницы с формой создания нового эджа на указанном узле. */
  def createEdgeGet(nodeId: MEsUuId) = csrf.AddToken {
    isSuNode(nodeId).async { implicit request =>
      val form = sysMarketUtil.edgeFormM
      _createEdgeBody(Ok, form)
    }
  }

  /** Рендер страницы создания эджа на узле. */
  private def _createEdgeBody(rs: Status, form: Form[MEdge])(implicit request: INodeReq[_]): Future[Result] = {
    rs( createEdgeTpl(form, request.mnode) )
  }

  /** Сабмит формы создания нового эджа на узле. */
  def createEdgePost(nodeId: MEsUuId) = csrf.AddToken {
    isSuNode(nodeId).async { implicit request =>
      def logPrefix = s"createEdgePost(${nodeId.id}):"

      sysMarketUtil.edgeFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(s"$logPrefix Failed to bind create form: ${formatFormErrors(formWithErrors)}")
          _createEdgeBody(NotAcceptable, formWithErrors)
        },
        {medge =>
          LOGGER.trace(s"$logPrefix Creating edge $medge on node ''${request.mnode.guessDisplayNameOrIdOrEmpty}''")
          for {
            _ <- mNodes.tryUpdate(request.mnode)(
              MNode.edges
                .composeLens( MNodeEdges.out )
                .modify( _ appended medge )
            )
          } yield {
            Redirect( routes.SysMarket.showAdnNode(nodeId) )
              .flashing( FLASH.SUCCESS -> s"Создан новый эдж на узле ''${request.mnode.guessDisplayNameOrIdOrEmpty}''." )
          }
        }
      )
    }
  }


  /** Экшен запроса страницы с формой редактирования эджа на узле. */
  def editEdgeGet(qs: MNodeEdgeIdQs) = csrf.AddToken {
    isSuNodeEdge(qs).async { implicit request =>
      val eform = sysMarketUtil.edgeFormM
        .fill( request.medge )
      _editEdgeBody(qs, Ok, eform)
    }
  }

  private def _editEdgeBody(qs: MNodeEdgeIdQs, rs: Status, ef: Form[MEdge])
                           (implicit request: INodeEdgeReq[_]): Future[Result] = {
    rs( editEdgeTpl(qs, ef, request.mnode) )
  }

  /** Экшен сабмита формы редактирования эджа. */
  def editEdgePost(qs: MNodeEdgeIdQs) = csrf.Check {
    isSuNodeEdge(qs).async { implicit request =>
      def logPrefix = s"editEdgePost($qs):"
      sysMarketUtil.edgeFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(s"$logPrefix Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
          _editEdgeBody(qs, NotAcceptable, formWithErrors)
        },
        {medge2 =>
          LOGGER.trace(s"$logPrefix Update of edge ${request.medge} using $medge2 on node ${request.mnode.guessDisplayNameOrIdOrEmpty}")

          // Запустить сохранение
          for {
            _ <- mNodes.tryUpdate(request.mnode)(
              // Функция замены эджа по id в инстансе узла.
              MNode.edges.modify { edges0 =>
                MNodeEdges.out.set(
                  MNodeEdges.edgesToMap1(
                    edges0.withIndexUpdated( qs.edgeId ) { e0 =>
                      sysMarketUtil.updateEdge(e0, medge2) ::
                        Nil
                    }
                  )
                )(edges0)
              }
            )
          } yield {
            // Отредиректить на sys-страницу узла.
            Redirect( routes.SysMarket.showAdnNode(qs.nodeId) )
              .flashing( FLASH.SUCCESS -> s"Обновлён эдж #${qs.edgeId} на узле ''${request.mnode.guessDisplayNameOrIdOrEmpty}''." )
          }
        }
      )
    }
  }

}
