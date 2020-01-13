package util.acl

import javax.inject.{Inject, Singleton}
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.im.MImgT
import models.req.MDynImgReq
import play.api.mvc._
import japgolly.univeq._
import io.suggest.common.fut.FutureUtil.HellImplicits._
import io.suggest.err.HttpResultingException
import io.suggest.es.model.EsModel
import io.suggest.model.n2.edge.MPredicates
import io.suggest.proto.http.HttpConst
import play.api.http.{HttpErrorHandler, Status}
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
class CanDynImg @Inject() (
                            esModel                 : EsModel,
                            mNodes                  : MNodes,
                            aclUtil                 : AclUtil,
                            reqUtil                 : ReqUtil,
                            cdnUtil                 : CdnUtil,
                            errorHandler            : HttpErrorHandler,
                            implicit private val ec : ExecutionContext
                          )
  extends MacroLogsImpl
{ outer =>

  import esModel.api._

  /** Проверка доступа к динамической картинке.
    *
    * @param mimg DynImgId
    * @return ActionBuilder, генерящия [[models.req.MDynImgReq]] для экшена.
    */
  def apply(mimg: MImgT): ActionBuilder[MDynImgReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MDynImgReq] {

      override def invokeBlock[A](request: Request[A], block: MDynImgReq[A] => Future[Result]): Future[Result] = {

        /** Ответ клиенту, когда картинка не найдена или недоступна. */
        def _imageNotFoundThrow = throw HttpResultingException(
          errorHandler.onClientError( request, Status.NOT_FOUND, "Image not found." )
        )

        val isImgOrig = mimg.dynImgId.isOriginal

        lazy val logPrefix = s"(${mimg.dynImgId.mediaId})#${System.currentTimeMillis()}:"

        // Запустить в фоне считывание инстансов MNode и MMedia.
        (for {

          fileNodesMap <- mNodes.multiGetMapCache( mimg.dynImgId.mediaIdAndOrigMediaId.toSet )

          nodeOrig = fileNodesMap
            .get( mimg.dynImgId.origNodeId )
            .filter { mnode =>
              {
                val r1 = mnode.common.isEnabled
                if (!r1) LOGGER.debug(s"$logPrefix 404. Img node is disabled.")
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
              LOGGER.trace(s"$logPrefix Found media#${mmedia.idOrNull} edgeMedia=${mmedia.edges.withPredicateIter(MPredicates.File).nextOption().orNull}")
              (mmedia, respMediaOpt)
            }
          }

          edgeMedia = mmedia
            .edges
            .withPredicateIter( MPredicates.File )
            .nextOption()
            .flatMap(_.media)
            .getOrElse {
              LOGGER.warn(s"$logPrefix Missing edges with predicate ${MPredicates.File}. Possibly invalid/corrupted node#${mmedia.idOrNull}")
              _imageNotFoundThrow
            }

          result <- {
            val storCheckFut = cdnUtil.checkStorageForThisNode( edgeMedia.storage )

            val user = aclUtil.userFromRequest( request )
            LOGGER.trace(s"$logPrefix Found img node#${nodeOrig.idOrNull}, user=${user.personIdOpt.orNull}")

            // Картинку можно отображать юзеру. Но нужно понять, на текущем узле она обслуживается или на каком-то другом.
            storCheckFut.flatMap {
              // Найдена картинка-оригинал вместо дериватива. Это тоже норм.
              case Right(storageInfoOpt) =>
                LOGGER.trace(s"$logPrefix Passed. Storage=${storageInfoOpt.fold("this")(_.toString)} respMedia=${respMediaOpt.orNull}")
                val req1 = MDynImgReq(
                  derivedOpt        = respMediaOpt,
                  storageInfo       = storageInfoOpt,
                  mnode             = nodeOrig,
                  user              = user,
                  request           = request,
                  edgeMedia         = edgeMedia,
                )
                block(req1)

              // Это не тот узел, а какой-то другой. Скорее всего, distUtil уже ругнулось в логи на эту тему.
              // По идее, это отработка какой-то ошибки в кластере, либо клиент подменил URL неправильным хостом.
              case Left(volLocs) =>
                LOGGER.warn(s"$logPrefix DistUtil refused media request: NOT related to this node. media#${mmedia.idOrNull} storage is ${edgeMedia.storage}, volume locations = ${volLocs.mkString(", ")}")
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
          .recoverWith {
            case HttpResultingException(resFut) => resFut
          }
      }

    }
  }

}
