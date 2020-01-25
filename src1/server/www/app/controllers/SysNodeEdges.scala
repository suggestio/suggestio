package controllers

import io.suggest.ctx.CtxData
import io.suggest.err.HttpResultingException
import io.suggest.es.model.EsModel
import io.suggest.init.routed.MJsInitTargets
import io.suggest.n2.edge.{MEdge, MNodeEdges}
import io.suggest.n2.edge.edit.{MEdgeEditFormInit, MNodeEdgeIdQs}
import io.suggest.n2.node.{MNode, MNodes}
import io.suggest.util.logs.MacroLogsImplLazy
import javax.inject.Inject
import models.mproj.ICommonDi
import play.api.libs.json.Json
import util.acl.IsSuNodeEdge
import io.suggest.n2.media.MEdgeMedia
import views.html.sys1.market.edge.EditEdge2Tpl
import japgolly.univeq._
import util.up.FileUtil

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.01.2020 17:35
  * Description: Контроллер управления эджами.
  */
final class SysNodeEdges @Inject() (
                                     isSuNodeEdge          : IsSuNodeEdge,
                                     sioControllerApi      : SioControllerApi,
                                     mCommonDi             : ICommonDi,
                                   )
  extends MacroLogsImplLazy
{

  import sioControllerApi._
  import mCommonDi.{csrf, ec, current}

  private lazy val mNodes = current.injector.instanceOf[MNodes]
  private lazy val esModel = current.injector.instanceOf[EsModel]
  private lazy val fileUtil = current.injector.instanceOf[FileUtil]


  /** Страница с формой редактирования эджа.
    *
    * @param qs Координата редактируемого эджа.
    * @return Страница с редактором эджа.
    */
  def editEdge(qs: MNodeEdgeIdQs) = csrf.AddToken {
    isSuNodeEdge(qs) { implicit request =>
      val state0 = MEdgeEditFormInit(
        edge   = request.edgeOpt,
        edgeId = qs,
      )

      val stateStr = Json
        .toJson( state0 )
        .toString()

      implicit val ctxData = CtxData(
        jsInitTargets = MJsInitTargets.EdgeEditForm :: Nil,
      )

      Ok( EditEdge2Tpl(stateStr, request.mnode) )
    }
  }


  /** Сабмит react-формы редактирования эджа.
    *
    * @param qs Координата редактируемого эджа.
    * @return
    */
  def saveEdge(qs: MNodeEdgeIdQs) = csrf.Check {
    isSuNodeEdge(qs).async( parse.json[MEdge] ) { implicit request =>
      import esModel.api._

      lazy val logPrefix = s"saveEdge($qs):"
      LOGGER.trace(s"$logPrefix ${if (request.edgeOpt.isEmpty) "Creating" else "Editing"} edge ${request.edgeOpt getOrElse ""} on node ''${request.mnode.guessDisplayNameOrIdOrEmpty}''")

      (for {
        mnode2 <- mNodes.tryUpdate(request.mnode)(
          MNode.edges
            .composeLens( MNodeEdges.out )
            .modify { edges0 =>
              qs.edgeId.fold {
                // Создание нового эджа.
                // Убедиться, что ещё нет добавляемого эджа в списке, т.к. бывают дублирующиеся запросы.
                if (edges0 contains request.body)
                  throw HttpResultingException( NotAcceptable(s"Node#${qs.nodeId} v${qs.nodeVsn} already have edge: ${request.body}") )
                // Добавить эдж в список эджей:
                edges0 appended request.body

              } { edgeId =>
                // Редактирование сущестующего эджа. Найти эдж в списке и заменить его новым.
                MNodeEdges.edgesToMap1(
                  for {
                    (e, i) <- edges0.iterator.zipWithIndex
                  } yield {
                    if (i ==* edgeId) {
                      request.body
                    } else {
                      e
                    }
                  }
                )
              }
            }
        )

        // Если в эдже есть файл, то надо решить, надо ли удалять старый файл.
        edgeMedia4deleteOpt <- request.edgeOpt.fold {
          Future.successful( Option.empty[MEdgeMedia] )
        } { edge =>
          fileUtil.isNeedDeleteFile( edge, mnode2, reportDupEdge = false )
        }

        // Удалить файл, если разрешено.
        _ <- fileUtil.deleteFileMaybe( edgeMedia4deleteOpt )

      } yield {
        Ok
      })
        .recoverWith {
          case HttpResultingException(httpResFut) => httpResFut
        }
    }
  }


  /** Сабмит удаления эджа.
    *
    * @param qs Координата эджа.
    * @return
    */
  def deleteEdge(qs: MNodeEdgeIdQs) = csrf.Check {
    isSuNodeEdge(qs).async { implicit request =>
      lazy val logPrefix = s"deleteEdgePost($qs)#${System.currentTimeMillis()}:"
      val edge4delete = request.edgeOpt.get
      LOGGER.trace(s"$logPrefix Deleting edge $edge4delete of node '''${request.mnode.guessDisplayNameOrIdOrEmpty}'''")

      // Если файловый эдж, тaо запустить поиск других узлов, которые могут ссылаться на текущий файл.
      val forDeleteOptFut = fileUtil.isNeedDeleteFile( edge4delete, request.mnode, reportDupEdge = true )

      import esModel.api._

      // Сохранить собранный эдж.
      for {
        // Запуск обновления текущего узла.
        _ <- mNodes.tryUpdate( request.mnode )(
          MNode.edges
            .composeLens( MNodeEdges.out )
            .modify { edgesOut0 =>
              MNodeEdges.edgesToMap1(
                edgesOut0
                  .iterator
                  .filterNot( request.edgeOpt.contains )
              )
            }
        )

        // Если это файловый эдж, то надо стереть и файл (если файл более нигде не используется в других узлах/эджах).
        delStorageFileOpt <- {
          LOGGER.debug(s"$logPrefix Node#${request.mnode.id.orNull} updated, edge#${qs.edgeId.orNull} deleted.")
          forDeleteOptFut
        }

        // Запустить удаление неиспользуемого файла.
        _ <- fileUtil.deleteFileMaybe( delStorageFileOpt )

      } yield {
        LOGGER.trace(s"$logPrefix Done.")
        NoContent
      }
    }
  }

}
