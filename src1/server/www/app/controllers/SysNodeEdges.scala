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
import io.suggest.n2.media.storage.IMediaStorages
import views.html.sys1.market.edge.EditEdge2Tpl
import japgolly.univeq._
import models.mup.MUploadInfoQs
import play.api.mvc.Results
import scalaz.Validation
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
  import mCommonDi.{csrf, ec, current, errorHandler}

  private lazy val mNodes = current.injector.instanceOf[MNodes]
  private lazy val esModel = current.injector.instanceOf[EsModel]
  private lazy val fileUtil = current.injector.instanceOf[FileUtil]
  private lazy val uploadCtl = current.injector.instanceOf[Upload]
  private lazy val iMediaStorages = current.injector.instanceOf[IMediaStorages]


  /** Страница с формой редактирования эджа.
    *
    * @param qs Координата редактируемого эджа.
    * @return Страница с редактором эджа.
    */
  def editEdge(qs: MNodeEdgeIdQs) = csrf.AddToken {
    isSuNodeEdge(qs, noEdgeIdOk = true, canRdr = true) { implicit request =>
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
    isSuNodeEdge(qs, noEdgeIdOk = true).async( parse.json[MEdge] ) { implicit request =>
      import esModel.api._

      lazy val logPrefix = s"saveEdge($qs):"
      LOGGER.trace(s"$logPrefix ${if (request.edgeOpt.isEmpty) "Creating" else "Editing"} edge ${request.edgeOpt getOrElse ""} on node ''${request.mnode.guessDisplayNameOrIdOrEmpty}''")

      (for {
        mnode2 <- mNodes.tryUpdate(request.mnode)(
          MNode.node_meta_basic_dateEdited_RESET andThen
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
        LOGGER.trace(s"$logPrefix Edge saved ok.")
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

      import esModel.api._

      // Сохранить собранный эдж.
      for {
        // Запуск обновления текущего узла.
        mnode2 <- mNodes.tryUpdate( request.mnode )(
          MNode.node_meta_basic_dateEdited_RESET andThen
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

        // Если файловый эдж, то запустить поиск других узлов, которые могут ссылаться на текущий файл.
        isNeedDeleteFile <- fileUtil.isNeedDeleteFile( edge4delete, mnode2, reportDupEdge = true )

        // Запустить удаление неиспользуемого файла.
        _ <- fileUtil.deleteFileMaybe( isNeedDeleteFile )

      } yield {
        LOGGER.trace(s"$logPrefix Done.")
        NoContent
      }
    }
  }


  /** Подготовка к upload'у.
    *
    * @param qs Координаты эджа.
    * @return Ответ Upload-контроллера.
    */
  def prepareUploadFile(qs: MNodeEdgeIdQs) = csrf.Check {
    isSuNodeEdge(qs, noEdgeIdOk = true).async( uploadCtl.prepareUploadBp ) { implicit request =>
      val logPrefix = s"prepareUploadFile($qs)#${System.currentTimeMillis()}:"

      val someTrue = Some(true)
      uploadCtl.prepareUploadLogic(
        logPrefix = logPrefix,
        validated = Validation.success( request.body ),
        upInfo = MUploadInfoQs(
          nodeType    = None,
          existNodeId = request.mnode.id,
          systemResp  = someTrue,
          // Принимать MIME от браузера при несовпадении.
          // MIME будет отображено в форме, админ подправит сам при необходимости.
          obeyMime    = someTrue,
        ),
      )
    }
  }


  /** Раздача файла из эджа.
    *
    * @param qs Координата эджа.
    * @return Файл.
    */
  def openFile(qs: MNodeEdgeIdQs) = {
    isSuNodeEdge(qs).async { implicit request =>
      val edgeMedia = request
        .edgeOpt.get
        .media.get

      (for {
        dataSource <- iMediaStorages
          .client( edgeMedia.storage.storage )
          .read( edgeMedia.storage.data, request.acceptCompressEncodings )
      } yield {
        sioControllerApi.sendDataSource(
          dataSource  = dataSource,
          contentType = edgeMedia.file.mime,
        )
          .withHeaders(
            Results
              .contentDispositionHeader( inline = false, name = request.mnode.guessDisplayName )
              .toSeq: _*
          )
      })
        .recoverWith { case _: NoSuchElementException =>
          errorHandler.onClientError( request, statusCode = NOT_FOUND, message = "File not found" )
        }
    }
  }

}
