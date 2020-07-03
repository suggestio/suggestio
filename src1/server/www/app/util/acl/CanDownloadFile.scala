package util.acl

import controllers.routes
import io.suggest.common.fut.FutureUtil
import io.suggest.err.HttpResultingException, HttpResultingException._
import io.suggest.es.model.EsModel
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.media.{MEdgeMedia, MFileMetaHash}
import io.suggest.n2.node.{MNodeTypes, MNodes}
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImplLazy
import javax.inject.Inject
import models.mup.{MDownLoadQs, MSwfsFidInfo}
import play.api.http.{HttpErrorHandler, Status}
import play.api.inject.Injector
import play.api.mvc.{Action, ActionBuilder, ActionRefiner, AnyContent, DefaultActionBuilder, Request, RequestHeader, Result, Results}
import util.cdn.CdnUtil
import util.up.UploadUtil
import japgolly.univeq._
import models.req.{MFileReq, MNodeEdgeOptReq, MSioUsers}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.02.2020 18:07
  * Description: Можно ли скачать файл?
  * Это вызывается на хосте *.nodes.suggest.io, поэтому сессия может быть недоступна.
  */
class CanDownloadFile @Inject()(
                                 injector: Injector,
                               )
  extends MacroLogsImplLazy
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  private lazy val defaultActionBuilder = injector.instanceOf[DefaultActionBuilder]
  private lazy val cdnUtil = injector.instanceOf[CdnUtil]
  private lazy val aclUtil = injector.instanceOf[AclUtil]
  private lazy val httpErrorHandler = injector.instanceOf[HttpErrorHandler]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]
  private lazy val uploadUtil = injector.instanceOf[UploadUtil]
  private lazy val mSioUsers = injector.instanceOf[MSioUsers]


  private def _throwNotFound(request: RequestHeader) =
    throw HttpResultingException( httpErrorHandler.onClientError(request, Status.NOT_FOUND) )


  private def _apply[A](dlQs: MDownLoadQs, request: Request[A])
                       (action: MFileReq[A] => Future[Result]): Future[Result] = {
    import esModel.api._

    val mreq = aclUtil.reqFromRequest( request )
    lazy val logPrefix = s"${dlQs.nodeId} [${mreq.remoteClientAddress}]:"

    if ( dlQs.validTillS.fold(false) { validTillS =>
      !uploadUtil.isTtlValid( validTillS, uploadUtil.rightNow() )
    }) {
      LOGGER.warn(s"$logPrefix Expired Link TTL: now=${uploadUtil.rightNow()} > link till=${dlQs.validTillS}")
      httpErrorHandler.onClientError( request, Status.PRECONDITION_FAILED, "Link TTL expired" )

    } else if (dlQs.clientAddr.fold(false)(_ !=* mreq.remoteClientAddress)) {
      LOGGER.warn(s"$logPrefix Client ip address mismatch, expected ${dlQs.clientAddr}, real ${mreq.remoteClientAddress}")
      httpErrorHandler.onClientError( request, Status.EXPECTATION_FAILED, "Client IP mismatch" )

    } else (for {
      mnodeOpt <- mNodes.getByIdAndCache( dlQs.nodeId )
      mnode = mnodeOpt getOrElse {
        // Узел не найден - вернуть 404.
        LOGGER.debug(s"$logPrefix Not found: node#${dlQs.nodeId}")
        _throwNotFound( request )
      }

      // Проверить, активен ли узел.
      _ = mnode.common.isEnabled || {
        val msg = s"Node#${dlQs.nodeId} is disabled."
        LOGGER.warn(s"$logPrefix $msg")
        throw HttpResultingException( httpErrorHandler.onClientError(request, Status.FORBIDDEN, msg) )
      }

      // Это media-узел?
      _ = (mnode.common.ntype eqOrHasParent MNodeTypes.Media) || {
        LOGGER.debug(s"$logPrefix Node#${dlQs.nodeId} of ntype#${mnode.common.ntype} is NOT media node.")
        _throwNotFound( request )
      }

      fileEdges = mnode.edges.withPredicate( MPredicates.Blob.File )

      // Найти файловый эдж
      (fileEdge, edgeMedia, storage) = (for {
        fileEdge1  <- fileEdges.out.iterator
        edgeMedia1 <- fileEdge1.media.iterator
        storage1   <- edgeMedia1.storage
      } yield {
        (fileEdge1, edgeMedia1, storage1)
      })
        .nextOption()
        .getOrElse {
          // (should never happen) Если media-узел без edge[File].edgeMedia, то это какая-то аномалия
          LOGGER.error(s"$logPrefix Media-node#${dlQs.nodeId} contains NO or invalid file edge!, file-edges:\n ${fileEdges.out.mkString(" \n")}")
          throw HttpResultingException( httpErrorHandler.onClientError(request, Status.INTERNAL_SERVER_ERROR) )
        }

      // Сверить хэши, заявленные в ссылке:
      if {
        val r = dlQs.hashesHex.forall { case (mhash, hashValue) =>
          edgeMedia.file.hashesHex.exists { fmh =>
            (fmh.hType ==* mhash) &&
            (fmh.hexValue ==* hashValue)
          }
        }

        if (!r) {
          // Ссылка всё-таки валидна, но устарела изнутри. Поэтому залить в ссылку новые хэши, отредиректив.
          val nodeId = dlQs.nodeId.id
          LOGGER.debug(s"$logPrefix Media-node#${dlQs.nodeId} found, but one (or more) URL hashes mismatches:\n URL: ${dlQs.hashesHex.mkString(" | ")}\n expected: ${edgeMedia.file.hashesHex.mkString(" | ")}. Recovering URL...")
          val mediaHostsMapFut = cdnUtil.mediasHosts1( (nodeId, storage) :: Nil )
          val dlQs2 = (MDownLoadQs.hashesHex set MFileMetaHash.toHashesHex( edgeMedia.file.hashesHex.dlHash ) )(dlQs)
          val dlUrlCall = routes.Upload.download( dlQs2 )
          val resFut = for {
            mediaHostsMap <- mediaHostsMapFut
          } yield {
            val url = cdnUtil.forMediaCall1(
              call          = dlUrlCall,
              mediaHostsMap = mediaHostsMap,
              mediaIds      = nodeId :: Nil,
            )
            LOGGER.trace(s"$logPrefix Rdr user#${dlQs.personId getOrElse ""} ${mreq.remoteClientAddress} =>\n $url")
            Results.Redirect(url)
          }
          throw HttpResultingException(resFut)
        } else {
          r
        }
      }

      // Проверить, соотносится ли запрос к файлу с текущим узлом?
      storageCheckE <- cdnUtil.checkStorageForThisNode( storage )

      // Поискать инфу по распределённому хранилищу:
      storageInfoOpt <- _getStorageInfo( edgeMedia )(request)

      user = mSioUsers( dlQs.personId )

      // TODO Когда будет проверка доступа юзера для скачивания, запилить её здесь.

      // Запуск экшена на исполнение, т.к. все проверки пройдены.
      dlReq = MFileReq(
        storageInfo   = storageInfoOpt,
        mnode         = mnode,
        edge          = fileEdge,
        edgeMedia     = edgeMedia,
        request       = mreq,
        // Юзера нет: это работа на хосте *.nodes.suggest.io, и кукисы сюда могут не передаваться.
        user          = user,
      )
      res <- action( dlReq )

    } yield {
      res
    })
      .recoverHttpResEx
  }


  private def _getStorageInfo( edgeMedia: MEdgeMedia )(implicit rh: RequestHeader): Future[Option[MSwfsFidInfo]] = {
    FutureUtil.optFut2futOpt( edgeMedia.storage ) { storage =>
      for {
        // Проверить, соотносится ли запрос к файлу с текущим узлом?
        storageCheckE <- cdnUtil.checkStorageForThisNode( storage )
      } yield {
        def logPrefix = s"_getStorageInfo($edgeMedia):"
        // Поискать инфу по распределённому хранилищу:
        storageCheckE.fold(
          {expectedVolumeLocations =>
            LOGGER.warn(s"$logPrefix DL request NOT related to current node.\n Current node = ${uploadUtil.MY_NODE_PUBLIC_URL}\n Storage volumes = ${expectedVolumeLocations.mkString(" | ")}")
            expectedVolumeLocations
              // Самозащита от неверных редиректов (seaweedfs нередко удивляла в прошлом).
              .find(_.publicUrl.nonEmpty)
              .fold {
                LOGGER.warn(s"$logPrefix No available volume locations for media ${edgeMedia.storage}\n file = ${edgeMedia.storage}")
                _throwNotFound(rh)
              } { vol0 =>
                LOGGER.debug(s"$logPrefix Redirecting from me ${uploadUtil.MY_NODE_PUBLIC_URL} => ${vol0.publicUrl}\n storage = ${edgeMedia.storage}\n file = ${edgeMedia.file}")
                val rdr = Results.Redirect( vol0.publicUrl + rh.uri, Status.SEE_OTHER )
                throw HttpResultingException( Future.successful(rdr) )
              }
          },
          identity,
        )
      }
    }
  }


  /** Сборка ActionBuilder'а, проверяющего возможность для аплоада файла. */
  def apply(upTg: MDownLoadQs): ActionBuilder[MFileReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MFileReq] {
      override def invokeBlock[A](request: Request[A], block: (MFileReq[A]) => Future[Result]): Future[Result] = {
        _apply(upTg, request)(block)
      }
    }
  }


  /** Сборка заворачивающего экшена, который проверяет возможность для аплоада файла. */
  def A[A](upTg: MDownLoadQs)(action: Action[A]): Action[A] = {
    defaultActionBuilder.async(action.parser) { request =>
      _apply(upTg, request)(action.apply)
    }
  }


  /** Конверсия из MNodeEdgeOptReq при условии, что все разрешени уже проверены. */
  class NodeEdgeOpt2MFileReq extends ActionRefiner[MNodeEdgeOptReq, MFileReq] {
    override protected def executionContext = ec
    override protected def refine[A](request: MNodeEdgeOptReq[A]): Future[Either[Result, MFileReq[A]]] = {
      for {
        req2Opt <- FutureUtil.optFut2futOptPlain(
          for {
            edge <- request.edgeOpt
            edgeMedia <- edge.media
          } yield {
            for {
              storageInfo <- _getStorageInfo(edgeMedia)(request)
            } yield {
              MFileReq(
                edge        = edge,
                edgeMedia   = edgeMedia,
                storageInfo = storageInfo,
                mnode       = request.mnode,
                user        = request.user,
                request     = request.request,
              )
            }
          }
        )

        res <- req2Opt
          .toRight {
            LOGGER.warn("fromNodeEdgeOptReq(): Invalid input: missing edge/edgeMedia or else.")
            httpErrorHandler.onClientError( request, Status.CONFLICT, "Missing edge/media." )
          }
          .fold(
            _.map(Left.apply),
            r => Future.successful( Right(r) )
          )

      } yield {
        res
      }
    }
  }

}
