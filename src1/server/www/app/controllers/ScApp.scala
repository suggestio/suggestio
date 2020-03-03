package controllers

import io.suggest.app.ios.{MIosItem, MIosItemAsset, MIosItemMeta, MIosManifest}
import io.suggest.dev.{MOsFamilies, MOsFamily}
import io.suggest.err.HttpResultingException
import io.suggest.es.model.EsModel
import io.suggest.ext.svc.MExtService
import io.suggest.i18n.MsgCodes
import io.suggest.ico.MLinkRelIcon
import io.suggest.img.MImgFmts
import io.suggest.n2.edge.{MEdge, MPredicates}
import io.suggest.n2.media.{MEdgeMedia, MFileMeta, MFileMetaHash}
import io.suggest.n2.node.{MNode, MNodes}
import io.suggest.pick.MimeConst
import io.suggest.plist.ApplePlistUtil
import io.suggest.pwa.manifest.{MPwaDisplayModes, MWebManifest}
import io.suggest.sc.app.{MScAppDlInfo, MScAppGetQs, MScAppGetResp}
import io.suggest.sc.pwa.MPwaManifestQs
import io.suggest.util.logs.MacroLogsImplLazy
import japgolly.univeq._
import javax.inject.Inject
import models.im.MFavIcons
import models.mproj.ICommonDi
import models.req.INodeOptReq
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.{Call, RequestHeader}
import util.acl.{MaybeAuth, MaybeAuthMaybeNode}
import util.billing.Bill2Conf
import util.cdn.{CdnUtil, CorsUtil}
import util.ext.ExtServicesUtil
import util.up.UploadUtil

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.18 17:00
  * Description: Контроллер экшенов для приложений и вёб-приложений выдачи.
  */
final class ScApp @Inject()(
                             sioControllerApi   : SioControllerApi,
                             mCommonDi          : ICommonDi,
                           )
  extends MacroLogsImplLazy
{

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
  private lazy val applePlistUtil = injector.instanceOf[ApplePlistUtil]

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
        icons     = MFavIcons.Icons().allIcons.map(_.icon),
      )

      Ok( Json.toJson( manifest ) )
        .as( MimeConst.WEB_APP_MANIFEST )
        // TODO Протюнить cache-control под реальную обстановку. 86400сек - это с потолка.
        .cacheControl(86400)
        .withHeaders( corsUtil.SIMPLE_CORS_HEADERS: _* )
    }
  }


  /** Поиск описанного эджа среди запрошенного узла и узла CBCA.
    *
    * result._1 = fromNodeIdOpt - содержит инфу, откуда взята инфа по приложению.
    * Это нужно передать клиенту, чтобы там сгенерить наиболее короткую ссылку для qr-кода.
    * @param qs query string.
    * @param request http-реквест с данными запрошенного узла.
    * @return Фьючерс с найденными эджами и опциональным id нестандартного узла.
    */
  private def _findAppEdge(qs: MScAppGetQs)(implicit request: INodeOptReq[_]): Future[(List[MEdge], Option[String])] = {
    lazy val logPrefix = s"_findAppEdge($qs):"

    // Найти инфу по эджу в узле:
    lazy val osFamilyAppDistrServices = qs.osFamily.appDistribServices
    def __findEdgesByServices(mnodeOpt: Option[MNode]): List[MEdge] = {
      (for {
        mnode <- mnodeOpt.iterator
        appEdge <- {
          var edges = mnode.edges
          // Если в qs задан предикат, то сначала отфильтровать по нему.
          for (qsPred <- qs.predicate)
            edges = edges.withPredicate( qsPred )
          // Доп.самоконтроль, чтобы не было выборки по не-Application вариантам.
          edges.withPredicateIter( MPredicates.Application )
        }
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
        // Сортируем по вручную-заданному порядку и по предикату, если порядок не задан.
        .sortBy { e =>
          (
            e.order getOrElse Int.MinValue,
            e.predicate match {
              case MPredicates.Application.FromFile     => 0
              case MPredicates.Application.Distributor  => 50
              case _ => 100
            }
          )
        }
    }

    val thisNodeAppEdges0 = __findEdgesByServices( request.mnodeOpt )
    if (thisNodeAppEdges0.isEmpty) {
      // Нет в указанном ноде заданного эджа приложения. Поискать в общем узле suggest.io.
      import esModel.api._
      val commonNodeId = bill2Conf.CBCA_NODE_ID
      for {
        commonMnodeOpt <- mNodes.getByIdCache( commonNodeId )
      } yield {
        val r = __findEdgesByServices( commonMnodeOpt )
        LOGGER.trace(s"$logPrefix CBCA node#$commonNodeId, app.edge => ${r.mkString("\n ")}")
        r -> Option.empty[String]
      }
    } else {
      LOGGER.trace(s"$logPrefix Found expected application edge on requested node#${qs.onNodeId.orNull}:\n ${thisNodeAppEdges0.mkString("\n ")}")
      Future.successful( thisNodeAppEdges0 -> qs.onNodeId )
    }
  }


  /** Сборка данных по файлу из указанного file node. */
  private def _nodeId2fileEdgeMedia(fileNodeId: String)
                                   (implicit request: RequestHeader): Future[(MNode, MEdge, MEdgeMedia)] = {
    import esModel.api._

    lazy val logPrefix = s"_nodeId2fileEdgeMedia($fileNodeId):"
    for {
      fileNodeOpt <- mNodes.getByIdCache( fileNodeId )
    } yield {
      val fileNode = fileNodeOpt getOrElse {
        val msg = s"Node not found: $fileNodeId"
        LOGGER.error(s"$logPrefix $msg - invalid file-edge nodeId(s) or missing node.")
        throw HttpResultingException( httpErrorHandler.onClientError(request, INTERNAL_SERVER_ERROR, msg) )
      }

      val (fileEdge, fileEdgeMedia) = (for {
        fileEdge <- fileNode.edges
          .withPredicateIter( MPredicates.File )
        edgeMedia <- fileEdge.media.iterator
      } yield {
        (fileEdge, edgeMedia)
      })
        .nextOption()
        .getOrElse {
          val msg = s"File-node#$fileNodeId have no edge-media"
          LOGGER.error(s"$logPrefix $msg ntype=${fileNode.common.ntype}\n ${fileNode.edges.out.mkString("\n ")}")
          throw HttpResultingException( httpErrorHandler.onClientError(request, INTERNAL_SERVER_ERROR, msg) )
        }

      (fileNode, fileEdge, fileEdgeMedia)
    }
  }


  /** Сборка ссылки на файл. */
  private def _mediaUrl(fileNodeId: String, fileEdgeMedia: MEdgeMedia): Future[Call] = {
    val mediaHostsMapFut = cdnUtil.mediasHosts1( (fileNodeId, fileEdgeMedia.storage) :: Nil )
    val dlUrlQs = uploadUtil.mkDlQs(
      fileNodeId = fileNodeId,
      hashesHex  = MFileMetaHash.toHashesHex( fileEdgeMedia.file.hashesHex.dlHash ),
    )
    val dlUrlRelCall = routes.Upload.download( dlUrlQs )

    for {
      mediaHostsMap <- mediaHostsMapFut
    } yield {
      // Рендер ссылки для скачивания.
      cdnUtil.forMediaCall1(
        call          = dlUrlRelCall,
        mediaHostsMap = mediaHostsMap,
        mediaIds      = fileNodeId :: Nil,
      )
    }
  }


  /** Поискать приложение под указанный узел.
    * Или приложение под suggest.io.
    *
    * @return Если qs.rdr == true, то возвращается редиретк.
    *         Иначе, MScAppGetResp() - ссылки для скачивания/редиректа.
    */
  def appDownloadInfo(qs: MScAppGetQs) = {
    maybeAuthMaybeNode( qs.onNodeId ).async { implicit request =>
      lazy val logPrefix = s"appDownLoadInfo($qs):"

      (for {
        // Найти эдж приложения согласно запрошенным требованиям:
        (appEdges, fromNodeIdOpt) <- _findAppEdge(qs)

        // Есть эдж. Если указание на файловый узел, то прочитать узел.
        dlInfos <- Future.traverse(appEdges) { appEdge =>
          (appEdge.predicate match {

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
                  osFamily  = appEdge.info.osFamily,
                  predicate = appEdge.predicate,
                  url       = extServicesUtil.applicationUrl( extSvc, appId ),
                  extSvc    = extSvcOpt,
                )
                LOGGER.trace(s"$logPrefix svc=$extSvc, appId=$appId =>\n URL: ${r.url}")
                Future.successful( r :: Nil )
              })
                .getOrElse {
                  LOGGER.error(s"$logPrefix Failed to generate app.URL for app.distributor on edge:\n $appEdge")
                  throw HttpResultingException( httpErrorHandler.onClientError( request, FAILED_DEPENDENCY ) )
                }


            // Локальный файл. Найти id узла-файла в эдже
            case MPredicates.Application.FromFile =>
              val fileNodeId = appEdge.nodeIds.head
              for {
                (fileNode, _, fileEdgeMedia) <- _nodeId2fileEdgeMedia( fileNodeId )

                dlCall <- {
                  if (qs.rdr) {
                    // Редирект - рендерим ссылку на закачку напрямую:
                    _mediaUrl(fileNodeId, fileEdgeMedia)

                  } else {
                    // Редирект не требуется. Значит идёт просто рендер списка вариантов. Рендерить непрямую ссылку
                    val dlInfoCall = routes.ScApp.appDownloadInfo(
                      MScAppGetQs(
                        osFamily    = qs.osFamily,
                        rdr         = true,
                        onNodeId    = fromNodeIdOpt,
                        predicate   = Some( appEdge.predicate ),
                      )
                    )
                    Future.successful( dlInfoCall )
                  }
                }

              } yield {
                val url = dlCall.absoluteURL()
                LOGGER.trace(s"$logPrefix Generated download link for node#${fileNodeId}:\n url = $url")

                val r = MScAppDlInfo(
                  url       = url,
                  predicate = appEdge.predicate,
                  fileName  = fileNode.guessDisplayName,
                  fromNodeIdOpt = fromNodeIdOpt,
                  osFamily  = appEdge.info.osFamily,
                  fileMeta  = Some( MFileMeta.hashesHex.modify(_.dlHash.toSeq)(fileEdgeMedia.file) ),
                )
                r :: Nil
              }


            // Остальное - не поддерживается или вообще не должно тут вызываться.
            case other =>
              LOGGER.error(s"$logPrefix Unexpected app.predicate#$other")
              throw HttpResultingException( httpErrorHandler.onClientError( request, INTERNAL_SERVER_ERROR ) )

          })
            // Подавить возможные ошибки в некоторых элементах.
            .recover { case ex: Throwable =>
              LOGGER.error(s"$logPrefix Failed to render app.dl.info for edge $appEdge", ex)
              Nil
            }
        }
          .map( _.flatten )

        res <- {
          if (qs.rdr) {
            // Запрашивается сразу редирект.
            dlInfos
              .headOption
              .fold {
                LOGGER.warn(s"$logPrefix No dlInfos found, 1 expected for rdr.")
                httpErrorHandler.onClientError(request, NOT_FOUND)
              } { dlInfo =>
                if (dlInfos.lengthIs > 1) {
                  val len = dlInfos.length
                  LOGGER.warn(s"$logPrefix Found $len dlInfos, but 1 expected for rdr:\n ${dlInfos.mkString("\n ")}")
                  httpErrorHandler.onClientError( request, MULTIPLE_CHOICES, s"$len results found, 1 expected." )
                } else {
                  Redirect( dlInfo.url )
                }
              }

          } else {
            val appGetResp = MScAppGetResp(
              dlInfos = dlInfos,
            )
            val resp = Ok( Json.toJson(appGetResp) )
            Future.successful( resp )
          }
        }

      } yield res)
        .recoverWith {
          case HttpResultingException(resFut) =>
            resFut
        }
    }
  }


  /** Экшен раздачи манифеста для скачивания.
    *
    * @param onNodeId id узла с приложением, если не дефолт.
    * @return Экшен, возвращающий Plist-манифест для установки приложения.
    */
  def iosInstallManifest(onNodeId: Option[String]) = {
    maybeAuthMaybeNode( onNodeId ).async { implicit request =>
      lazy val logPrefix = s"iosInstallManifest(${onNodeId getOrElse ""}):"

      val appEdgeQs = MScAppGetQs(
        osFamily  = MOsFamilies.Apple_iOS,
        rdr       = false,
        onNodeId  = onNodeId,
        predicate = Some( MPredicates.Application.FromFile ),
      )

      (for {

        (appEdges, _) <- _findAppEdge( appEdgeQs )

        // Есть хотя бы один эдж?
        _ = if ( appEdges.isEmpty ) {
          LOGGER.debug(s"$logPrefix Not found any app edges for $appEdgeQs")
          val respFut = httpErrorHandler.onClientError(request, NOT_FOUND, "No app found for manifesting.")
          throw HttpResultingException( respFut )
        }

        // Только один эдж? Или несколько?
        _ = if ( appEdges.lengthIs > 1 ) {
          LOGGER.warn(s"$logPrefix Too many (${appEdges.length}) app edges found for $appEdgeQs:\n ${appEdges.mkString("\n ")}")
          val respFut = httpErrorHandler.onClientError(request, MULTIPLE_CHOICES, "Two or more variants.")
          throw HttpResultingException( respFut )
        }

        appEdge = appEdges.head
        fileNodeId = appEdge.nodeIds.head

        _fileEdgeMediaFut = _nodeId2fileEdgeMedia( fileNodeId )

        ctx = getContext2

        itemAssets = {
          def _mkIconAbsUrl(relIcon: MLinkRelIcon) =
            cdnUtil.absUrl( cdnUtil.asset(relIcon.icon.src)(ctx) )(ctx)

          val favIcons = MFavIcons.Icons()

          // Параллельно, рассчитать ссылки на картинки.
          val fullSizeImageAsset = MIosItemAsset(
            kind = "full-size-image",  // тут надо 512х512
            url = Some( _mkIconAbsUrl( favIcons.appleTouchIcon512 ) ),
          )

          // TODO надо 57х57 !
          // TODO Когда будет 57х57 картинка, снести этот велосипед
          val displayImageAssetOpt = (for {
            ico <- favIcons.allIcons
            if (ico.imgFmt ==* MImgFmts.PNG) &&
              (ico.icon.sizes.exists { sz => sz.width < 200 })
          } yield {
            MIosItemAsset(
              kind = "display-image",
              url  = Some( _mkIconAbsUrl(ico) ),
            )
          })
            .headOption

          fullSizeImageAsset :: displayImageAssetOpt.toList
        }

        (fileMnode, _, fileEdgeMedia) <- _fileEdgeMediaFut
        pkgMediaCall <- _mediaUrl( fileNodeId, fileEdgeMedia )

      } yield {
        // Допускается хранить метаданные в узле в techInfo в виде JSON.
        val imeta: MIosItemMeta = (for {
          techInfo <- fileMnode.meta.basic.techName
          jsonParsed   <- Try( Json.parse(techInfo) ).toOption
          r <- jsonParsed.asOpt[MIosItemMeta]
        } yield {
          LOGGER.trace(s"$logPrefix Using saved meta-info: $r")
          r
        })
          .getOrElse {
            MIosItemMeta(
              bundleId  = "io.suggest.appsuggest",
              bundleVersion = "1.0.0",
              kind      = "software",
              title     = "Suggest.io",
              platformId = Some( "com.apple.platform.iphoneos" ),
            )
          }

        val manifest = MIosManifest(
          items = MIosItem(
            // TODO Пытаться парсить JSON из fileMnode.meta...techInfo, но надо решить проблему там с ошибочным двойным экранированием текста (SysMarket/SysMarketUtil).
            metadata = imeta,
            assets = MIosItemAsset(
              kind = "software-package",
              url  = Some( cdnUtil.absUrl( pkgMediaCall )(ctx) ),
            ) :: itemAssets,
          ) :: Nil,
        )

        val manifestJson = Json.toJson( manifest )

        // Вместо JSON отрендерить и вернуть Plist.
        val plist = applePlistUtil.toPlistDocument( manifestJson )
        Ok( plist )
          .as( XML )
          .cacheControl( 3600 )
      })
        .recoverWith {
          case HttpResultingException(respFut) => respFut
        }
    }
  }

}
