package util.acl

import javax.inject.{Inject, Singleton}

import io.suggest.model.n2.media.{MMedia, MMediasCache}
import io.suggest.model.n2.node.{MNodeTypes, MNodesCache}
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.im.MImgT
import models.req.MMediaOptNodeReq
import play.api.mvc._
import japgolly.univeq._
import io.suggest.common.fut.FutureUtil.HellImplicits._
import io.suggest.common.html.HtmlConstants
import io.suggest.proto.HttpConst
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
                            aclUtil                 : AclUtil,
                            reqUtil                 : ReqUtil,
                            mMediasCache            : MMediasCache,
                            cdnUtil                 : CdnUtil,
                            mNodesCache             : MNodesCache,
                            implicit private val ec : ExecutionContext
                          )
  extends MacroLogsImpl
{ outer =>

  /** Проверка доступа к динамической картинке.
    *
    * @param mimg DynImgId
    * @return ActionBuilder, генерящия [[models.req.MMediaOptNodeReq]] для экшена.
    */
  def apply(mimg: MImgT): ActionBuilder[MMediaOptNodeReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MMediaOptNodeReq] {

      override def invokeBlock[A](request: Request[A], block: MMediaOptNodeReq[A] => Future[Result]): Future[Result] = {
        // Запустить в фоне считывание инстансов MNode и MMedia.
        val mnodeOptFut = mNodesCache.getById( mimg.dynImgId.nodeId )
        val mmediaOptFut = mMediasCache.getById( mimg.dynImgId.mediaId )

        // TODO Различия формата с оригиналом тут не определить. Как выкручиваться из такой ситуации? Сделать dynFormat как Option?
        val isImgOrig = !mimg.dynImgId.hasImgOps

        // Сразу запрашиваем оригинал картинки. Он может и не понадобиться в данной логике, поэтому lazy.
        lazy val mmediaOrigOptFut = if (isImgOrig) {
          mmediaOptFut
        } else {
          mMediasCache.getById(mimg.dynImgId.original.mediaId)
        }

        lazy val logPrefix = s"(${mimg.dynImgId.mediaId})#${System.currentTimeMillis()}:"

        mnodeOptFut.flatMap {
          case Some( mnode ) if mnode.common.isEnabled && (mnode.common.ntype ==* MNodeTypes.Media.Image) =>
            if (!mnode.common.isEnabled) {
              LOGGER.debug(s"$logPrefix 404. Img node is disabled.")
              _imageNotFound

            } else if (mnode.common.ntype !=* MNodeTypes.Media.Image) {
              LOGGER.warn(s"$logPrefix 404. Node type is ${mnode.common.ntype}, but image expected.")
              _imageNotFound

            } else {
              val user = aclUtil.userFromRequest( request )
              LOGGER.trace(s"$logPrefix Found img node#${mnode.idOrNull}, user=${user.personIdOpt.orNull}")

              def __mediaFoundResp(mmedia: MMedia, respMediaOpt: Option[MMedia]): Future[Result] = {
                // Сравнить узел картинки с текущим узлом:
                cdnUtil.checkStorageForThisNode(mmedia.storage).flatMap {
                  // Найдена картинка-оригинал вместо дериватива. Это тоже норм.
                  case Right(storageInfo) =>
                    LOGGER.trace(s"$logPrefix Passed. Storage=$storageInfo respMedia=${respMediaOpt.orNull}")
                    val req1 = MMediaOptNodeReq(
                      mmediaOpt         = respMediaOpt,
                      mmediaOrigOptFut  = () => mmediaOrigOptFut,
                      storageInfo       = storageInfo,
                      mnode             = mnode,
                      user              = user,
                      request           = request
                    )
                    block(req1)

                  // Это не тот узел, а какой-то другой. Скорее всего, distUtil уже ругнулось в логи на эту тему.
                  // По идее, это отработка какой-то ошибки в кластере, либо клиент подменил URL неправильным хостом.
                  case Left(volLocs) =>
                    LOGGER.warn(s"$logPrefix DistUtil refused media request: NOT related to this node. media#${mmedia.idOrNull} storage is ${mmedia.storage}, volume locations = ${volLocs.mkString(", ")}")
                    // Попытаться отредиректить на правильный узел.
                    volLocs.headOption.fold(_imageNotFound) { volLoc =>
                      val correctUrl = HttpConst.Proto.CURR_PROTO + volLoc.publicUrl + HtmlConstants.SLASH + request.uri
                      LOGGER.info(s"$logPrefix Redirected user#${user.personIdOpt.orNull} to volume#$volLoc:\n $correctUrl")
                      Results.Redirect(correctUrl)
                    }
                }
              }

              // Картинку можно отображать юзеру. Но нужно понять, на текущем узле она обслуживается или на каком-то другом.
              mmediaOptFut.flatMap {
                // Нет такой картинки в хранилище. dyn-картинка ещё не существует. Возможно, она генерится прямо сейчас.
                // Но вот вопрос: относится ли данная картинка к данному узлу? Это можно узнать с помощью картинки-оригинала:
                case None =>
                  // Убедиться, что на руках НЕ оригинал. Иначе - ситуация непонятная получается.
                  if (isImgOrig) {
                    LOGGER.error(s"$logPrefix Img original mmedia not found, but node exist. Should never happen, possibly errors in database. mnode = $mnode")
                    _imageNotFound

                  } else {
                    LOGGER.trace(s"$logPrefix Derivative img missing, will try to use original...")
                    mmediaOrigOptFut.flatMap {
                      case Some(mmediaOrig) =>
                        __mediaFoundResp(mmediaOrig, respMediaOpt = None)

                      // Не найден оригинал, это какой-то косяк в MNodes, возможно заливка происходит прямо сейчас.
                      case None =>
                        // should never happen
                        LOGGER.error(s"$logPrefix Img medias not found, neither derivative, nor original. mnode is invalid: $mnode")
                        _imageNotFound
                    }
                  }


                // Уже есть такая картинка, хранится где-то в кластере. TODO Пора сравнивать media-сервер с текущим сервером.
                case respMediaSome @ Some(mmedia) =>
                  LOGGER.trace(s"$logPrefix Found media#${mmedia.idOrNull} wh=${mmedia.picture.whPx.orNull} mime=${mmedia.file.mime} sz=${mmedia.file.sizeB}b at storage ${mmedia.storage}")
                  __mediaFoundResp(mmedia, respMediaOpt = respMediaSome)
              }
            }

          // Узел не найден или выключен. Считаем, что картинка просто не существует
          case None =>
            LOGGER.trace(s"$logPrefix Img node not found")
            _imageNotFound
        }
      }


      /** Ответ клиенту, когда картинка не найдена или недоступна. */
      def _imageNotFound: Future[Result] = {
        Results.NotFound( "Image not found." )
      }

    }
  }

}
