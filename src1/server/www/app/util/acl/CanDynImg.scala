package util.acl

import javax.inject.{Inject, Singleton}
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.im.MImgT
import models.req.MFileReq
import play.api.mvc._
import japgolly.univeq._
import io.suggest.common.fut.FutureUtil.HellImplicits._
import io.suggest.err.HttpResultingException
import HttpResultingException._
import io.suggest.es.model.EsModel
import io.suggest.n2.edge.MPredicates
import io.suggest.proto.http.HttpConst
import play.api.http.{HttpErrorHandler, Status}
import play.api.inject.Injector
import util.cdn.CdnUtil

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.02.18 16:39
  * Description: ACL-проверка доступа к Img.dynImg() и похожим экшенам, связанным со сборкой динамических картинок.
  * Этот код проверяет dist (например, ссылки на s2 не должны приходить на s3).
  */
@Singleton
final class CanDynImg @Inject() (
                                  injector                : Injector,
                                )
  extends MacroLogsImpl
{ outer =>

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val aclUtil = injector.instanceOf[AclUtil]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  private lazy val cdnUtil = injector.instanceOf[CdnUtil]
  private lazy val errorHandler = injector.instanceOf[HttpErrorHandler]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]


  /** Проверка доступа к динамической картинке.
    *
    * @param mimg DynImgId
    * @return ActionBuilder, генерящия [[models.req.MFileReq]] для экшена.
    */
  def apply(mimg: MImgT): ActionBuilder[MFileReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MFileReq] {

      override def invokeBlock[A](request: Request[A], block: MFileReq[A] => Future[Result]): Future[Result] = {
        import esModel.api._

        /** Ответ клиенту, когда картинка не найдена или недоступна. */
        def _imageNotFoundThrow = throw HttpResultingException(
          errorHandler.onClientError( request, Status.NOT_FOUND, "Image not found." )
        )

        val isImgOrig = mimg.dynImgId.isOriginal

        lazy val logPrefix = s"(${mimg.dynImgId.mediaId})#${System.currentTimeMillis()}:"

        // Запустить в фоне считывание инстансов MNode и MMedia.
        (for {

          fileNodesMap <- mNodes.multiGetMapCache( mimg.dynImgId.mediaIdAndOrigMediaId.toSet )

          nodeOrig = {
            LOGGER.trace(s"$logPrefix orig?$isImgOrig fileNodesMap = $fileNodesMap")

            if (fileNodesMap.isEmpty)
              LOGGER.info(s"$logPrefix If fileNodesMap[${fileNodesMap.size}].isEmpty=${fileNodesMap.isEmpty}, but node must be here exist, ensure elasticsearch nodes connectivity.")

            fileNodesMap
              .get( mimg.dynImgId.origNodeId )
              .filter { mnode =>
                {
                  val r1 = mnode.common.isEnabled
                  if (!r1) LOGGER.warn(s"$logPrefix 404. Img node is disabled.")
                  r1
                } && {
                  val r2 = mnode.common.ntype ==* MNodeTypes.Media.Image
                  if (!r2) LOGGER.warn(s"$logPrefix 404. Node type is ${mnode.common.ntype}, but image expected.")
                  r2
                }
              }
              .getOrElse {
                // Узел не найден или выключен. Считаем, что картинка просто не существует
                LOGGER.trace(s"$logPrefix Img node not found or unrelated filtered-out.")
                _imageNotFoundThrow
              }
          }

          // Далее у нас два режима работы:
          (mmedia: MNode, respMediaOpt: Option[MNode]) = {
            val respMediaOpt = fileNodesMap.get( mimg.dynImgId.mediaId )
            respMediaOpt.fold {
              // Нет такой картинки в хранилище. dyn-картинка ещё не существует. Возможно, она генерится прямо сейчас.
              // Но вот вопрос: относится ли данная картинка к данному узлу? Это можно узнать с помощью картинки-оригинала:
              // Убедиться, что на руках НЕ оригинал. Иначе - ситуация непонятная получается.
              if (isImgOrig) {
                LOGGER.error(s"$logPrefix Img original mmedia not found, but node exist. Should never happen, possibly errors in database. mnode = ${nodeOrig.idOrNull}")
                _imageNotFoundThrow

              } else {
                LOGGER.trace(s"$logPrefix Derivative img missing, bypass action with original...")
                (nodeOrig, Option.empty[MNode])
              }

            } { mmedia =>
              LOGGER.trace(s"$logPrefix Found media#${mmedia.idOrNull} edgeMedia=${mmedia.edges.withPredicateIter(MPredicates.Blob.File).nextOption().orNull}")
              (mmedia, respMediaOpt)
            }
          }

          fileEdge = mmedia
            .edges
            .withPredicateIter( MPredicates.Blob.File )
            .nextOption()
            .getOrElse {
              LOGGER.warn(s"$logPrefix Missing edges with predicate ${MPredicates.Blob.File}. Possibly invalid/corrupted node#${mmedia.idOrNull}")
              _imageNotFoundThrow
            }

          edgeMedia = fileEdge
            .media
            .getOrElse {
              LOGGER.warn(s"$logPrefix Corrupted ${MPredicates.Blob.File} edge (missing edge.media):\n $fileEdge")
              _imageNotFoundThrow
            }

          storage = edgeMedia.storage getOrElse {
            LOGGER.warn(s"$logPrefix edgeMedia storage is undefined")
            _imageNotFoundThrow
          }

          result <- {
            val storCheckFut = cdnUtil.checkStorageForThisNode( storage )

            val user = aclUtil.userFromRequest( request )
            LOGGER.trace(s"$logPrefix Found img node#${nodeOrig.idOrNull}, user=${user.personIdOpt.orNull}")

            // Картинку можно отображать юзеру. Но нужно понять, на текущем узле она обслуживается или на каком-то другом.
            storCheckFut.flatMap {
              // Найдена картинка-оригинал вместо дериватива. Это тоже норм.
              case Right(storageInfoOpt) =>
                LOGGER.trace(s"$logPrefix Passed. Storage=${storageInfoOpt.fold("this")(_.toString)} respMedia=${respMediaOpt.orNull}")
                val req1 = MFileReq(
                  derivativeOpt     = respMediaOpt,
                  storageInfo       = storageInfoOpt,
                  mnode             = nodeOrig,
                  user              = user,
                  request           = request,
                  edgeMedia         = edgeMedia,
                  edge          = fileEdge,
                )
                block(req1)

              // Это не тот узел, а какой-то другой. Скорее всего, distUtil уже ругнулось в логи на эту тему.
              // По идее, это отработка какой-то ошибки в кластере, либо клиент подменил URL неправильным хостом.
              case Left(volLocs) =>
                LOGGER.warn(s"$logPrefix Media request NOT related to this node. media#${mmedia.idOrNull} storage is ${edgeMedia.storage}, volume locations = ${volLocs.mkString(", ")}")
                // Попытаться отредиректить на правильный узел.
                volLocs
                  .headOption
                  .fold( _imageNotFoundThrow ) { volLoc =>
                    val correctUrl = HttpConst.Proto.CURR_PROTO + cdnUtil.reWriteHostToCdn( volLoc.publicUrl ) + request.uri
                    LOGGER.info(s"$logPrefix Redirected user#${user.personIdOpt.orNull} to volume#$volLoc:\n $correctUrl")
                    Results.Redirect( correctUrl )
                  }
            }
          }

        } yield result)
          // И финальный перехват всех прерываний логики работы action-builder'а:
          .recoverHttpResEx
      }

    }
  }

}
