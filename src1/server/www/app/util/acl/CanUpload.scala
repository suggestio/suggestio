package util.acl

import io.suggest.common.fut.FutureUtil
import javax.inject.Inject
import io.suggest.ctx.{MCtxId, MCtxIds}
import io.suggest.err.HttpResultingException
import io.suggest.es.model.EsModel
import io.suggest.n2.edge.{MEdgeFlags, MPredicates}
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.pick.MimeConst
import io.suggest.req.ReqUtil
import io.suggest.up.MUploadChunkQs
import io.suggest.util.logs.MacroLogsImplLazy
import japgolly.univeq._
import models.mup.{MUploadChunkReq, MUploadReq, MUploadTargetQs}
import models.req.MSioUsers
import play.api.http.{HttpErrorHandler, HttpVerbs, Status}
import play.api.inject.Injector
import play.api.mvc._
import play.mvc.Http.HeaderNames
import util.cdn.CdnUtil
import util.up.UploadUtil

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.10.17 11:34
  * Description: ACL-проверка на предмет возможности текущему юзеру производить заливку файла в suggest.io.
  */
class CanUpload @Inject()(
                           injector                   : Injector,
                         )
  extends MacroLogsImplLazy
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  private lazy val mSioUsers = injector.instanceOf[MSioUsers]
  private lazy val defaultActionBuilder = injector.instanceOf[DefaultActionBuilder]
  private lazy val cdnUtil = injector.instanceOf[CdnUtil]
  private lazy val mCtxIds = injector.instanceOf[MCtxIds]
  private lazy val httpErrorHandler = injector.instanceOf[HttpErrorHandler]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]
  private lazy val uploadUtil = injector.instanceOf[UploadUtil]


  /** Логика кода проверки прав, заворачивающая за собой фактический экшен, живёт здесь.
    * Это позволяет использовать код и в ActionBuilder, и в Action'ах.
    *
    * Проверка исполняется ВНЕ ПОЛЬЗОВАТЕЛЬСКОЙ СЕССИИ, на нодах *.nodes.suggest.io.
    *
    * @param upTg Описание данных аплоада.
    * @param request0 HTTP-реквест.
    * @param f Фунция фактического экшена.
    * @tparam A Тип BodyParser'а.
    * @return Фьючерс результата.
    */
  private def _file[A](upTg: MUploadTargetQs, ctxIdOpt: Option[MCtxId], request0: Request[A])
                      (f: MUploadReq[A] => Future[Result]): Future[Result] = {
    lazy val logPrefix = s"[${System.currentTimeMillis}]:"

    // 2017.oct.19 Для кукисов затянуты гайки, и они теперь точно не передаются на ноды. Берём данные сессии прямо из подписанного URL запроса.
    val user = mSioUsers( upTg.personId )
    if (ctxIdOpt.exists(ctxId => !mCtxIds.validate(ctxId, user.personIdOpt))) {
      val ctxId = ctxIdOpt.get
      // Юзер прислал неправильный ctxId. Такое возможно, если юзер перелогинился в одной вкладке, но не в другой. Либо попытка подмены.
      val msg = "CtxId is not valid."
      LOGGER.warn(s"$logPrefix $msg for user#${user.personIdOpt.orNull}, userMatchesCtxId?${user.personIdOpt ==* ctxId.personId}, raw ctxId = $ctxId")
      httpErrorHandler.onClientError(request0, Status.FORBIDDEN, msg)

    } else {
      // Если задан id узла для обновления, то поискать узел:
      val existNodeOptFut = upTg.info.existNodeId.fold {
        if (upTg.info.nodeType.isEmpty) {
          val msg = s"$logPrefix nodeType is empty, but existNodeId is empty. Node creation impossible w/o type."
          Future.failed( new IllegalStateException(msg) )
        } else {
          Future.successful( Option.empty[MNode] )
        }
      } { nodeId =>
        import esModel.api._

        for {
          mnodeOpt <- mNodes.getByIdCache( nodeId )
          if {
            val r = mnodeOpt.nonEmpty
            if (!r)
              LOGGER.error(s"$logPrefix Requested node#$nodeId not found.")
            r
          }
          mnode = mnodeOpt.get
          if {
            val wantedType = (upTg.info.nodeType getOrElse MNodeTypes.Media)
            val r = mnode.common.ntype eqOrHasParent wantedType

            if (r)
              LOGGER.trace(s"$logPrefix Target node#$nodeId found with expected type#${upTg.info.nodeType.fold("*")(_.toString)}")
            else
              LOGGER.error(s"$logPrefix Node#$nodeId has unexpected type#${mnode.common.ntype}. Should be #$wantedType")

            r
          }
        } yield {
          mnodeOpt
        }
      }

      (for {
        storageEith <- cdnUtil.checkStorageForThisNode( upTg.storage.storage )
        if storageEith.isRight
        existNodeOpt <- existNodeOptFut
        resp <- {
          LOGGER.trace(s"$logPrefix Allowed to process file upload, storage => $storageEith")
          val mreq = MUploadReq(
            swfsOpt       = storageEith.toOption.flatten,
            existNodeOpt  = existNodeOpt,
            request       = request0,
            user          = user,
          )
          f(mreq)
        }
      } yield {
        resp
      })
        .recoverWith { case ex: Throwable =>
          // Рядом с текущим узлом нет искомой swfs volume. Это значит, что юзер подменил хостнейм в сгенеренной ссылке,
          // и пытается залить файл мимо целевого сервера (либо какая-то ошибка в конфигурации).
          LOGGER.warn(s"$logPrefix Failed to validate SWFS upload args", ex)
          httpErrorHandler.onClientError(request0, Status.EXPECTATION_FAILED, s"Storage ${upTg.storage}:${upTg.storage.host.nameInt}:${upTg.storage.storage} looks unavailable for upload from ${uploadUtil.MY_NODE_PUBLIC_URL}.")
        }
    }
  }

  /** Сборка ActionBuilder'а, проверяющего возможность для аплоада файла. */
  def file(upTg: MUploadTargetQs, ctxIdOpt: Option[MCtxId]): ActionBuilder[MUploadReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MUploadReq] {
      override def invokeBlock[A](request: Request[A], block: (MUploadReq[A]) => Future[Result]): Future[Result] =
        _file(upTg, ctxIdOpt, request)(block)
    }
  }

  /** Сборка заворачивающего экшена, который проверяет возможность для аплоада файла. */
  def fileA[A](upTg: MUploadTargetQs, ctxIdOpt: Option[MCtxId])(action: Action[A]): Action[A] = {
    defaultActionBuilder.async(action.parser) { request =>
      _file(upTg, ctxIdOpt, request)(action.apply)
    }
  }


  /** Заливка куска файла. */
  private def _chunk[A](upTg: MUploadTargetQs, upChunkQs: MUploadChunkQs, request0: Request[A])
                       (f: MUploadChunkReq[A] => Future[Result]): Future[Result] = {
    val nowTtl = uploadUtil.rightNow()
    lazy val logPrefix = s"_chunk()#${nowTtl}:"
    LOGGER.trace(s"$logPrefix\n upTg = $upTg\n upChunkQs = $upChunkQs")

    if (!uploadUtil.isTtlValid(upTg.validTillS, nowTtl)) {
      LOGGER.warn(s"$logPrefix Link ttl expired. URL=${upTg.validTillS} NOW=$nowTtl")
      httpErrorHandler.onClientError( request0, Status.EXPECTATION_FAILED, s"Request TTL expired." )

    } else (for {
      // Для chunk-upload наличие узла обязательно: там хранится промежуточное состояние.
      existNodeOpt <- FutureUtil.optFut2futOpt( upTg.info.existNodeId ) { existNodeId =>
        import esModel.api._
        mNodes.getByIdCache( existNodeId )
      }
      existNode = existNodeOpt getOrElse {
        val msg = s"Node#${upTg.info.existNodeId.orNull} not found."
        LOGGER.warn( s"$logPrefix $msg")
        val res = httpErrorHandler.onClientError( request0, Status.FORBIDDEN, msg )
        throw HttpResultingException( res )
      }

      fileEdge = existNode.edges
        .withoutPredicate( MPredicates.Blob.File )
        .out
        .find { fileEdge =>
          // Идёт загрузка - выставлен InProgress-флаг.
          fileEdge.info.flags
            .exists(_.flag ==* MEdgeFlags.InProgress)
        }
        .getOrElse {
          val msg = s"Node#${upTg.info.existNodeId.orNull} not ready for upload"
          LOGGER.warn(s"$logPrefix $msg")
          val res = httpErrorHandler.onClientError( request0, Status.FORBIDDEN, msg )
          throw HttpResultingException( res )
        }

      edgeMedia = fileEdge.media getOrElse {
        LOGGER.warn(s"$logPrefix Node#${upTg.info.existNodeId.orNull} missing edgeMedia in fileEdge#$fileEdge")
        val res = httpErrorHandler.onClientError( request0, Status.FAILED_DEPENDENCY, "Missing upload information." )
        throw HttpResultingException( res )
      }

      totalSizeB = edgeMedia.file.sizeB getOrElse {
        LOGGER.warn(s"$logPrefix Node#${upTg.info.existNodeId.orNull} edgeMedia.file.size missing.")
        val res = httpErrorHandler.onClientError( request0, Status.LENGTH_REQUIRED, "Missing upload information." )
        throw HttpResultingException( res )
      }

      // Проверить, что в узел сейчас допускается частичная заливка одного chunk'а.
      // Это должен быть disabled-узел с Blob-эджем, содержащим полные данные закачки.
      if {
        // Для защиты от повторных параллельных (пере)загрузок в тот же узел, надо сверять
        // файл-хэши из signed-ссылки и сохранённые хэши в fileEdge.
        val isNodeUploading =
          upTg.fileProps.hashesHex.forall { urlFileHashHex =>
            edgeMedia.file.hashesHex.exists { fileMetaHash =>
              (urlFileHashHex.hType ==* fileMetaHash.hType) &&
              (fileMetaHash.hexValue ==* urlFileHashHex.hexValue)
            }
          }

        if (!isNodeUploading) {
          LOGGER.warn(s"$logPrefix Node#${upTg.info.existNodeId.orNull} hashesHex mismatch.")
          val res = httpErrorHandler.onClientError( request0, Status.FORBIDDEN, "Node not ready for upload." )
          throw HttpResultingException( res )
        }

        // Проверить валидность chunkQs: чтобы begin byte не выбегал за допустимые пределы, чтобы chunkSize был корректен.

        // totalSize должен хранится в edgeMedia ноды, поэтому в qs - необязателен. Но должен совпадать с хранимым в ноде.
        if ( !upChunkQs.totalSize.fold(true)(_ ==* totalSizeB) ) {
          LOGGER.warn(s"$logPrefix File totalSize qs=${upChunkQs.toString} != saved=$totalSizeB in node#${upTg.info.existNodeId.orNull}")
          val resFut = httpErrorHandler.onClientError( request0, Status.NOT_ACCEPTABLE, "Total size unexpected." )
          throw HttpResultingException( resFut )
        }

        val chunkNumber0 = upChunkQs.chunkNumber0
        val chunkStartAbsB = chunkNumber0 * upChunkQs.chunkSizeGeneral.value

        // begin byte chunk'а не выходит за пределы totalSizeB
        if (chunkStartAbsB >= totalSizeB) {
          LOGGER.warn(s"$logPrefix Chunk#${upChunkQs.chunkNumber} start.abs=$chunkStartAbsB b >= totalSize=$totalSizeB -- out of file byte-range")
          val resFut = httpErrorHandler.onClientError( request0, Status.NOT_ACCEPTABLE, "StartByte out of total size." )
          throw HttpResultingException(resFut)
        }

        // Проверить Content-Length в request-хидере, для POST/PUT-запросов.
        (request0.method match {
          case HttpVerbs.GET =>
            // Тело проверять не требуется.
            true

          case HttpVerbs.POST | HttpVerbs.PUT =>
            // Тело POST/PUT
            if (
              !request0.headers
                .get( HeaderNames.CONTENT_TYPE )
                .exists { contentType =>
                  contentType ==* MimeConst.APPLICATION_OCTET_STREAM
                }
            ) {
              val msg = "Octet contentType expected."
              LOGGER.warn(s"$logPrefix $msg")
              val resFut = httpErrorHandler.onClientError( request0, Status.UNSUPPORTED_MEDIA_TYPE, msg )
              throw HttpResultingException( resFut )
            }

            (for {
              contentLenStr <- request0.headers.get( HeaderNames.CONTENT_LENGTH )
              contentLen = contentLenStr.toLong
              if chunkStartAbsB + contentLen <= totalSizeB
            } yield {
              true
            })
              .getOrElse {
                val msg = "EndByte out of totalSize."
                LOGGER.warn(s"$logPrefix $msg")
                val res = httpErrorHandler.onClientError( request0, Status.REQUEST_ENTITY_TOO_LARGE, msg )
                throw HttpResultingException(res)
              }

          case other =>
            // should never happen
            val msg = s"Unexpected HTTP method: $other"
            LOGGER.warn(s"$logPrefix $msg")
            val resFut = httpErrorHandler.onClientError( request0, Status.NOT_IMPLEMENTED, msg )
            throw HttpResultingException(resFut)
        })
      }

      // Запустить chunk-запрос на исполнение.
      user = mSioUsers( upTg.personId )
      actionRes <- f(
        MUploadChunkReq(
          mnode         = existNode,
          fileEdge      = fileEdge,
          fileEdgeMedia = edgeMedia,
          request       = request0,
          user          = user,
        )
      )

    } yield {
      actionRes
    })
      .recoverWith {
        case HttpResultingException(resFut) => resFut
      }
  }

  /** Сборка ActionBuilder'а, проверяющего возможность для аплоада файла. */
  def chunk(upTg: MUploadTargetQs, chunkQs: MUploadChunkQs): ActionBuilder[MUploadChunkReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MUploadChunkReq] {
      override def invokeBlock[A](request: Request[A], block: (MUploadChunkReq[A]) => Future[Result]): Future[Result] =
        _chunk(upTg, chunkQs, request)(block)
    }
  }

}
