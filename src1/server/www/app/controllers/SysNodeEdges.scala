package controllers

import io.suggest.ctx.CtxData
import io.suggest.err.HttpResultingException
import io.suggest.es.model.EsModel
import io.suggest.init.routed.MJsInitTargets
import io.suggest.n2.edge.{MEdge, MNodeEdges}
import io.suggest.n2.edge.edit.{MEdgeEditFormInit, MNodeEdgeIdQs}
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.media.storage.IMediaStorages
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.n2.node.{MNode, MNodes}
import io.suggest.util.logs.MacroLogsImplLazy
import javax.inject.Inject
import models.mproj.ICommonDi
import play.api.libs.json.Json
import util.acl.IsSuNodeEdge
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.n2.media.MEdgeMedia
import views.html.sys1.market.edge.EditEdge2Tpl
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

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
  private lazy val iMediaStorages = current.injector.instanceOf[IMediaStorages]


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
          _isNeedDeleteFile( edge, mnode2, reportDupEdge = false )
        }

        // Удалить файл, если разрешено.
        _ <- _deleteFile( edgeMedia4deleteOpt )

      } yield {
        Ok
      })
        .recoverWith {
          case HttpResultingException(httpResFut) => httpResFut
        }
    }
  }


  /** Надо ли стирать файл из хранилища?
    *
    * @param edge4delete
    * @param mnode Узел. Если удаление эджа, то исходный узел.
    *              Если редактирование эджа, то УЖЕ ОТРЕДАКТИРОВАННЫЙ узел.
    * @param reportDupEdge Надо ли ругаться в логи, если файл всё ещё существует в узле?
    * @return None - удалять ничего не надо.
    *         Some() - надо удалить указанный файл.
    */
  private def _isNeedDeleteFile(edge4delete: MEdge,
                                mnode: MNode,
                                reportDupEdge: Boolean): Future[Option[MEdgeMedia]] = {
    import esModel.api._

    lazy val logPrefix = s"isNeedDeleteFile(${edge4delete.predicate} ${edge4delete.media.map(_.storage.data.meta).orNull}, node#${mnode.idOrNull}, dup?$reportDupEdge):"

    (for {
      edgeMediaDelete <- edge4delete.media
      // Если нельзя это удалять физически, то и шерстить смысла нет.
      if {
        val r = edgeMediaDelete.storage.storage.canDelete
        if (!r)
          LOGGER.debug(s"$logPrefix Edge have file ${edgeMediaDelete.storage.data.meta}, but delete operation is not supported.")
        r
      }
    } yield {
      // Если есть ещё file-эджи внутри текущего узла, то поискать.
      // Вообще, других эджей быть не должно, но всё-равно страхуемся от ошибочных дубликатов эджа...
      val nodeHasSameFileEdge = (for {
        nodeEdge          <- mnode.edges.out.iterator
        if nodeEdge !===* edge4delete
        nodeEdgeMedia     <- nodeEdge.media.iterator
        if nodeEdgeMedia.storage isSameFile edgeMediaDelete.storage
      } yield {
        def msg = s"$logPrefix node#${mnode.idOrNull} contains file-node. Will NOT delete file."

        if (reportDupEdge)
          LOGGER.warn(s"$msg\n Duplicated edge:\n  $edge4delete\n duplicate edges:\n  $nodeEdgeMedia")
        else
          LOGGER.debug(msg)

        true
      })
        .nextOption()
        .getOrElseFalse

      if (nodeHasSameFileEdge) {
        Future.successful( None )
      } else {
        // Как и ожидалось, эджа с файлом внутри текущего узла нет. Поискать среди других узлов.
        LOGGER.trace(s"$logPrefix Search for ${edgeMediaDelete.storage} in other nodes...")

        for {
          isFileExistElsewhereIds <- mNodes.dynSearchIds(
            new MNodeSearch {
              override val outEdges: Seq[Criteria] = {
                val cr = Criteria(
                  fileStorType      = Set( edgeMediaDelete.storage.storage ),
                  fileStorMetaData  = Set( edgeMediaDelete.storage.data.meta ),
                )
                cr :: Nil
              }
              override val withoutIds = mnode.id.toList
              override def limit = 1
            }
          )
        } yield {
          val r = isFileExistElsewhereIds.isEmpty

          if (r) LOGGER.trace(s"$logPrefix Stored file ${edgeMediaDelete.storage} is not used anymore.")
          else LOGGER.warn(s"$logPrefix File ${edgeMediaDelete.storage} will not erased from storage: it is used on node ${isFileExistElsewhereIds.mkString(" ")}")

          Option.when( r )( edgeMediaDelete )
        }
      }
    })
      .getOrElse( Future.successful(None) )
  }


  /** Произвести удаления файла.
    *
    * @param media4deleteOpt Выхлоп _isNeedDeleteFile()
    * @return Фьючерс с каким-то неопределённым результатом удаления.
    */
  private def _deleteFile(media4deleteOpt: Option[MEdgeMedia]): Future[_] = {
    media4deleteOpt.fold [Future[_]] {
      Future.successful(None)
    } { media4delete =>
      LOGGER.trace(s"_deleteFile(${media4delete.storage.data.meta}) Will delete file $media4delete")
      val stor = media4delete.storage
      iMediaStorages
        .client( stor.storage )
        .delete( stor.data )
    }
  }


  /** Сабмит удаления эджа.
    *
    * @param qs Координата эджа.
    * @return
    */
  def deleteEdge(qs: MNodeEdgeIdQs) = csrf.Check {
    isSuNodeEdge(qs).async { implicit request =>
      import esModel.api._
      lazy val logPrefix = s"deleteEdgePost($qs)#${System.currentTimeMillis()}:"

      val edge4delete = request.edgeOpt.get
      LOGGER.trace(s"$logPrefix Deleting edge $edge4delete of node '''${request.mnode.guessDisplayNameOrIdOrEmpty}'''")

      // Если файловый эдж, тaо запустить поиск других узлов, которые могут ссылаться на текущий файл.
      val forDeleteOptFut = _isNeedDeleteFile( edge4delete, request.mnode, reportDupEdge = true )

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
        _ <- _deleteFile( delStorageFileOpt )

      } yield {
        LOGGER.trace(s"$logPrefix Done.")
        NoContent
      }
    }
  }

}
