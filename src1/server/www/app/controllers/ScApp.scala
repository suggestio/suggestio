package controllers

import io.suggest.dev.MOsFamily
import io.suggest.err.HttpResultingException
import io.suggest.es.model.EsModel
import io.suggest.ext.svc.MExtService
import io.suggest.i18n.MsgCodes
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.node.{MNode, MNodes}
import io.suggest.pick.MimeConst
import io.suggest.pwa.manifest.{MPwaDisplayModes, MWebManifest}
import io.suggest.sc.app.{MScAppDlInfo, MScAppGetQs, MScAppGetResp}
import io.suggest.sc.pwa.MPwaManifestQs
import japgolly.univeq._
import javax.inject.Inject
import models.im.MFavIcons
import models.mproj.ICommonDi
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import util.acl.{MaybeAuth, MaybeAuthMaybeNode}
import util.billing.Bill2Conf
import util.cdn.{CdnUtil, CorsUtil}
import util.ext.ExtServicesUtil
import util.up.UploadUtil

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.18 17:00
  * Description: Контроллер экшенов для приложений и вёб-приложений выдачи.
  */
final class ScApp @Inject()(
                             sioControllerApi   : SioControllerApi,
                             mCommonDi          : ICommonDi,
                           ) {

  import mCommonDi.current.injector

  private lazy val maybeAuthMaybeNode = injector.instanceOf[MaybeAuthMaybeNode]
  private lazy val bill2Conf = injector.instanceOf[Bill2Conf]
  private lazy val corsUtil = injector.instanceOf[CorsUtil]
  private lazy val maybeAuth = injector.instanceOf[MaybeAuth]
  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private implicit lazy val ec = injector.instanceOf[ExecutionContext]
  private lazy val httpErrorHandler = injector.instanceOf[HttpErrorHandler]
  private lazy val extServicesUtil = injector.instanceOf[ExtServicesUtil]
  private lazy val cdnUtil = injector.instanceOf[CdnUtil]
  private lazy val uploadUtil = injector.instanceOf[UploadUtil]

  import sioControllerApi._


  /** Экшен раздачи json-манифеста с описаловом вёб-приложения.
    * @see [[https://developer.mozilla.org/en-US/docs/Web/Manifest]]
    *
    * @param qs Данные url qs.
    * @return 200 + JSON
    */
  def webAppManifest(qs: MPwaManifestQs) = {
    maybeAuth(U.PersonNode).async { implicit request =>
      // TODO Нужна локализация? И если нужна, то на уровне URL, или на уровне user-сессии?
      val sio = MsgCodes.`Suggest.io`
      val manifest = MWebManifest(
        name      = sio,
        // TODO Полное и короткое названия должны различаться.
        shortName = Some( sio ),
        startUrl  = routes.Sc.geoSite().url,
        display   = Some( MPwaDisplayModes.Standalone ),
        icons     = MFavIcons.linkRelIcons.map(_.icon),
      )

      Ok( Json.toJson( manifest ) )
        .as( MimeConst.WEB_APP_MANIFEST )
        // TODO Протюнить cache-control под реальную обстановку. 86400сек - это с потолка.
        .cacheControl(86400)
        .withHeaders( corsUtil.SIMPLE_CORS_HEADERS: _* )
    }
  }



  /** Поискать приложение под указанный узел.
    * Или приложение под suggest.io.
    * @return MScAppGetResp - Ссылка для скачивания/редиректа.
    */
  def appDownloadInfo(qs: MScAppGetQs) = {
    // Найти инфу по эджу в узле:
    val osFamilyAppDistrServices = qs.osFamily.appDistribServices
    def __findEdges(mnodeOpt: Option[MNode]) = {
      (for {
        mnode <- mnodeOpt.iterator
        appEdge <- mnode.edges.withPredicateIter( MPredicates.Application )
        // Поискать подходящий сервис дистрибуции под запрошенную платформу
        if (appEdge.info.osFamily contains[MOsFamily] qs.osFamily) && {
          appEdge.info.extService
            .fold(true) {
              osFamilyAppDistrServices.contains[MExtService]
            }
        }
      } yield {
        appEdge
      })
        .to( List )
    }

    maybeAuthMaybeNode( qs.onNodeId ).async { implicit request =>
      lazy val logPrefix = s"appDownLoadInfo($qs):"

      (for {

        // Поиск описанного эджа среди запрошенного узла и узла CBCA.
        appEdges <- {
          val thisNodeAppEdges0 = __findEdges( request.mnodeOpt )
          if (thisNodeAppEdges0.isEmpty) {
            // Нет в указанном ноде заданного эджа приложения. Поискать в общем узле suggest.io.
            import esModel.api._
            val commonNodeId = bill2Conf.CBCA_NODE_ID
            for {
              commonMnodeOpt <- mNodes.getByIdCache( commonNodeId )
            } yield {
              val r = __findEdges( commonMnodeOpt )
              LOGGER.trace(s"$logPrefix CBCA node#$commonNodeId, app.edge => ${r.mkString("\n ")}")
              r
            }
          } else {
            LOGGER.trace(s"$logPrefix Found expected application edge on requested node#${qs.onNodeId.orNull}:\n ${thisNodeAppEdges0.mkString("\n ")}")
            Future.successful(thisNodeAppEdges0)
          }
        }

        // Есть эдж. Если указание на файловый узел, то прочитать узел.
        dlInfos <- Future.traverse(appEdges) { appEdge =>
          appEdge.predicate match {

            // Отрендерить ответ с ссылкой на сервис.
            case MPredicates.Application.Distributor =>
              val extSvcOpt = appEdge.info.extService
              (for {
                extSvc <- extSvcOpt
                appId <- appEdge.info
                  .textNi
                  .orElse( appEdge.nodeIds.headOption )
              } yield {
                val r = MScAppDlInfo(
                  predicate = appEdge.predicate,
                  url       = extServicesUtil.applicationUrl( extSvc, appId ),
                  extSvc    = extSvcOpt,
                )
                LOGGER.trace(s"$logPrefix svc=$extSvc, appId=$appId =>\n URL: ${r.url}")
                Future.successful( r )
              })
                .getOrElse {
                  LOGGER.error(s"$logPrefix Failed to generate app.URL for app.distributor on edge:\n $appEdge")
                  throw HttpResultingException( httpErrorHandler.onClientError( request, FAILED_DEPENDENCY ) )
                }


            // Локальный файл. Найти id узла-файла в эдже
            case MPredicates.Application.FromFile =>
              import esModel.api._
              val fileNodeId = appEdge.nodeIds.head
              for {
                fileNodeOpt <- mNodes.getByIdCache( fileNodeId )
                fileNode = fileNodeOpt getOrElse {
                  val msg = s"Node not found: $fileNodeId"
                  LOGGER.error(s"$logPrefix $msg - invalid file-edge nodeId(s) or missing node.")
                  throw HttpResultingException( httpErrorHandler.onClientError(request, INTERNAL_SERVER_ERROR, msg) )
                }

                fileNodeEdgeMedia = (for {
                  fileEdge <- fileNode.edges
                    .withPredicateIter( MPredicates.File )
                  edgeMedia <- fileEdge.media.iterator
                } yield {
                  edgeMedia
                })
                  .nextOption()
                  .getOrElse {
                    val msg = s"File-node#$fileNodeId have no edge-media"
                    LOGGER.error(s"$logPrefix $msg ntype=${fileNode.common.ntype}\n ${fileNode.edges.out.mkString("\n ")}")
                    throw HttpResultingException( httpErrorHandler.onClientError(request, INTERNAL_SERVER_ERROR, msg) )
                  }

                // Для сборки cdn-dist ссылки, собрать карту хостов под файловые ноды:
                mediaHostsMapFut = cdnUtil.mediasHosts1( (fileNodeId, fileNodeEdgeMedia.storage) :: Nil )

                dlUrlQs = uploadUtil.mkDlQs(
                  fileNodeId = fileNodeId,
                  hashesHex  = uploadUtil.dlQsHashesHex( fileNodeEdgeMedia ),
                )
                dlUrlRelCall = routes.Upload.download( dlUrlQs )

                mediaHostsMap <- mediaHostsMapFut

                // Рендер ссылки для скачивания.
                dlCall = cdnUtil.forMediaCall1(
                  call          = dlUrlRelCall,
                  mediaHostsMap = mediaHostsMap,
                  mediaIds      = fileNodeId :: Nil,
                )

              } yield {
                val url = dlCall.absoluteURL()
                LOGGER.trace(s"$logPrefix Generated download link for node#${fileNodeId}:\n url = $url")

                MScAppDlInfo(
                  url       = url,
                  predicate = appEdge.predicate,
                  fileName  = fileNode.guessDisplayName,
                  fileSizeB = fileNodeEdgeMedia.file.sizeB,
                )
              }


            // Остальное - не поддерживается или вообще не должно тут вызываться.
            case other =>
              LOGGER.error(s"$logPrefix Unexpected app.predicate#$other")
              throw HttpResultingException( httpErrorHandler.onClientError( request, INTERNAL_SERVER_ERROR ) )

          }
        }

      } yield {
        val appGetResp = MScAppGetResp(
          dlInfos = dlInfos,
        )
        Ok( Json.toJson(appGetResp) )
      })
        .recoverWith {
          case HttpResultingException(resFut) =>
            resFut
        }
    }
  }

}
