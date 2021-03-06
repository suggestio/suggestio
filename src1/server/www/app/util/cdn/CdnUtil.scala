package util.cdn

import javax.inject.{Inject, Singleton}
import controllers.routes
import io.suggest.common.empty.OptionUtil
import io.suggest.n2.media.storage._
import io.suggest.n2.media.storage.swfs.SwfsVolumeCache
import io.suggest.playx.ExternalCall
import io.suggest.swfs.client.proto.lookup.IVolumeLocation
import io.suggest.url.MHostInfo
import io.suggest.util.logs.{MacroLogsDyn, MacroLogsImpl}
import models.mctx.Context
import models.mup.MSwfsFidInfo
import play.api.Configuration
import play.api.mvc.Call
import OptionUtil.BoolOptOps
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.media.MFileMeta
import io.suggest.n2.node.MNode
import io.suggest.proto.http.HttpConst
import util.up.UploadUtil
import japgolly.univeq._
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.10.14 18:31
  * Description: Утиль для работы с CDN и распределением-балансировки контента по серверам
  * (как внутри серверов s.io, так и внешней CDN).
  */
final class CdnUtil @Inject() (
                                injector                  : Injector,
                              )
  extends MacroLogsImpl
{

  private lazy val cdnConf = injector.instanceOf[CdnConf]
  private lazy val uploadUtil = injector.instanceOf[UploadUtil]
  private lazy val swfsVolumeCache = injector.instanceOf[SwfsVolumeCache]
  private lazy val iMediaStorages = injector.instanceOf[IMediaStorages]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]


  /** Выбрать подходящий CDN-хост для указанного протокола. */
  def chooseHostForProto(protoLc: String): Option[String] = {
    cdnConf
      .CDN_PROTO_HOSTS
      .get(protoLc)
      .flatMap(_.headOption)    // TODO Выбирать рандомный хост из списка хостов.
  }

  def ctx2CdnHost(implicit ctx: Context): Option[String] = {
    chooseHostForProto( ctx.request.myProto )
  }
  def ctx2CdnHostOrDflt(implicit ctx: Context): String = {
    chooseHostForProto( ctx.request.myProto )
      .getOrElse( ctx.api.ctxUtil.HOST_PORT )
  }

  /** Генератор вызовов к CDN или внутренних. */
  def forCall(c: Call)(implicit ctx: Context): Call = {
    if (cdnConf.isNoCdnHosts) {
      // CDN сейчас не задана, но она обычно есть на продакшене. Поэтому используем текущий хост в качестве CDN-хоста.
      val backendCdnHost = ctx.api.ctxUtil.HOST_PORT
      if (ctx.request.host equalsIgnoreCase backendCdnHost) {
        // Same host, no cdn - return fully-relative URL as-is.
        c
      } else {
        // 3rd-party host. Return proto-less URL with primary/backend HOST. So, cbca.ru site.html will contain CSS-links to suggest.io/.../x.css
        val nextUrlPrefix = ctx.protoUrlPrefix(
          host = backendCdnHost,
        )
        new ExternalCall( nextUrlPrefix + c.url )
      }

    } else if (c.isInstanceOf[ExternalCall]) {
      // Уже внешний Call, там уже хост должен быть прописан.
      c

    } else {
      val reqHost = ctx.request.host
      val urlPrefixOpt = OptionUtil.maybeOpt(!(cdnConf.DISABLED_ON_HOSTS contains reqHost)) {
        val protoLc = ctx.request.myProto
        for {
          cdnHost <- chooseHostForProto(protoLc)
          if !(cdnHost equalsIgnoreCase reqHost)
        } yield {
          // Не указываем протокол. Это хорошо, когда CDN работает по HTTP, а раздаёт по HTTPS.
          HttpConst.Proto.CURR_PROTO + cdnHost
        }
      }
      urlPrefixOpt.fold(c) { urlPrefix =>
        new ExternalCall(url = urlPrefix + c.url)
      }
    }
  }

  /** Вызов на asset через CDN. */
  def asset(file: String)(implicit ctx: Context): Call = {
    forCall( routes.Assets.versioned(file) )
  }


  /** Бывает, что нужно в зависимости от значения флага генерить полные и относительные ссылки.
    * Не очень уместный здесь код (к CDN напрямую не относится).
    *
    * @param forceAbsoluteUrl true -- нужна абсолютная ссылка. false -- хватит и относительной.
    * @param call исходный вызов.
    * @return Строка с ссылкой.
    */
  def maybeAbsUrl(forceAbsoluteUrl: Boolean)(call: Call)(implicit ctx: Context): String = {
    if (forceAbsoluteUrl) {
      absUrl( call )
    } else {
      call.url
    }
  }


  def absUrl(call: Call)(implicit ctx: Context): String = {
    import ctx.request
    val absUrl = call.absoluteURL(
      secure = ctx.request.isTransferSecure || {
        // Есть проблема с isTransferSecure: бывает неправильное false при https-запросах, ИДУЩИХ ЧЕРЕЗ CDN, которые
        // внутри идут на голый http://backend.suggest.io и вызывающий false в isTransferSecure, если X-Forwarded-Proto не указан.
        (cdnConf.DISABLED_ON_HOSTS contains ctx.request.host) &&
        ctx.api.ctxUtil.HTTPS_ENABLED
      },
    )
    if (absUrl startsWith HttpConst.Proto.CURR_PROTO) {
      // Вот так бывает: протокол не указан, потому что forCall() больше не пишет протокол.
      // Значит, уже отсылка к CDN, и значит дописываем https (или http, если локалхост):
      ctx.request.myProto + HttpConst.Proto.COLON + absUrl
    } else {
      absUrl
    }
  }


  /** Отмаппить внутренний хост sio на CDN-хост.
    * Синхронный и быстрый метод (как и все остальные здесь), для возможности эффективного использования в шаблонах.
    *
    * @param host Данные хоста-ноды sio-кластера (см. medias2hosts()).
    * @return Хостнейм CDN-хоста.
    *         Если CDN недоступна, то вернуть обычный public-адрес.
    */
  def distHost2cdnUrlPrefix(host: MHostInfo): String = {
    HttpConst.Proto.CURR_PROTO + reWriteHostToCdn(host.namePublic)
  }


  /** Сборка URL для dist-cdn с защитой от ExternalCall. */
  def distNodeCdnUrl(host: MHostInfo, call: Call): String = {
    call match {
      // В теории возможно, что метод будет вызван с инстансом ExternalCall.
      case extCall: ExternalCall =>
        throw new IllegalArgumentException(s"ExternalCall is unsupported: $extCall ; host=$host")
      case _ =>
        distNodeCdnUrlNoCheck(host, call)
    }
  }
  /** Сборка URL для dist-cdn без защиты от ExternalCall. */
  def distNodeCdnUrlNoCheck(host: MHostInfo, call: Call): String = {
    distHost2cdnUrlPrefix(host) + call.url
  }


  /** Используемое хранилище. */
  private def DIST_STORAGE: MStorage = MStorages.SeaWeedFs


  /** Подготовить местечко для сохранения нового файла, вернув данные сервера для заливки файла.
    *
    * @param upProps Обобщённые данные по заливаемому файлу.
    * @return Фьючерс с результатом.
    */
  def assignDist(upProps: MFileMeta): Future[MAssignedStorage] = {
    val assignFut = iMediaStorages
      .client( DIST_STORAGE )
      .assignNew()

    if (LOGGER.underlying.isTraceEnabled())
      for (assignedStorage <- assignFut)
        LOGGER.trace(s"assignDist[${System.currentTimeMillis()}]: Assigned ok:\n props = $upProps\n resp = $assignedStorage")

    assignFut
  }


  def toAssignedStorage( nodeId: String, storageInfo: MStorageInfo ): Future[Option[MAssignedStorage]] = {
    for {
      mediaHostsMap <- mediasHosts1( (nodeId -> storageInfo) :: Nil )
      hostInfos = mediaHostsMap
        .valuesIterator
        .flatten
        .to(LazyList)
    } yield {
      for {
        mediaHost <- chooseMediaHost( nodeId, hostInfos )
      } yield {
        MAssignedStorage(
          host    = mediaHost,
          storage = storageInfo,
        )
      }
    }
  }



  /** Проверка возможности аплоада прямо сюда на текущую ноду.
    *
    * @param storage Строка инфы в контексте данного хранилища.
    * @return Расширенные данные для аплоада, если Some.
    *         None, значит доступ закрыт.
    */
  def checkStorageForThisNode(storage: MStorageInfo): Future[Either[Seq[IVolumeLocation], Option[MSwfsFidInfo]]] = {
    // Распарсить Swfs FID из URL и сопоставить полученный volumeID с текущей нодой sio.
    lazy val logPrefix = s"checkStorageForThisNode($storage)#${System.currentTimeMillis()}:"

    storage.storage match {
      case MStorages.SeaWeedFs =>
        val fid = storage.data.swfsFid.get
        for {
          volLocs <- swfsVolumeCache.getLocations( fid.volumeId )
        } yield {
          // Может быть несколько результатов, если у volume существуют реплики.
          // Нужно найти целевую мастер-шарду, которая располагается где-то очень близко к текущему локалхосту.
          val myExtHost = uploadUtil.MY_NODE_PUBLIC_HOST
          if (volLocs.isEmpty)
            LOGGER.warn(s"$logPrefix Lost SWFS volume#${fid.volumeId} with 0 locations for fid#$fid. Bypassing error...")

          volLocs
            .find { volLoc =>
              volLoc.publicUrl ==* myExtHost
              // Не проверяем nameInt/url, потому что там полу-рандомный порт swfs
            }
            .map { myVol =>
              LOGGER.trace(s"$logPrefix Ok, local vol = $myVol\n fid = ${fid.toString}\n all vol locs = ${volLocs.mkString(", ")}")
              Some( MSwfsFidInfo(fid, myVol, volLocs) )
            }
            .toRight {
              LOGGER.error(s"$logPrefix Failed to find vol#${fid.volumeId} for fid='$fid' nearby. My=$myExtHost, storage=$storage Other available volumes considered non-local: ${volLocs.mkString(", ")}")
              volLocs
            }
        }

      case MStorages.ClassPathResource =>
        // Всегда и на любом хосте.
        Future.successful( Right(None) )
    }
  }


  /** Вернуть карту медиа-хосты для указанных media.
    *
    * @param fileNodes Список интересующих media.
    * @return Карта mediaId -> hostname.
    */
  def mediasHosts(fileNodes: Iterable[MNode]): Future[Map[String, Seq[MHostInfo]]] = {
    val node2storage = (for {
      fileNode  <- fileNodes.iterator
      nodeId    <- fileNode.id.iterator
      fileEdge  <- fileNode.edges
        .withPredicateIter( MPredicates.Blob.File )
      edgeMedia <- fileEdge.media.iterator
      storage   <- edgeMedia.storage
    } yield {
      (nodeId, storage)
    })
      .to( LazyList )

    mediasHosts1( node2storage )
  }

  def mediasHosts1(node2storage: Iterable[(String, MStorageInfo)]): Future[Map[String, Seq[MHostInfo]]] = {
    if (node2storage.isEmpty) {
      Future.successful( Map.empty )

    } else {
      // Есть ноды для анализа. Надо сгруппировать по типам стораджей.
      for {
        storages2hostsMaps <- Future.sequence {
          (for {
            (storType, nodeIdInfos) <- node2storage
              .groupBy( _._2.storage )
              .iterator
          } yield {
            val storClient = iMediaStorages.client( storType )
            val storInfos = nodeIdInfos.map( _._2.data )
            storClient.getStoragesHosts( storInfos )
          })
            .toSeq
        }
      } yield {
        LOGGER.trace(s"mediaHosts(${node2storage.size} medias): Done\n mediaIds = ${node2storage.iterator.flatMap(_._1).mkString(", ")}")
        val storages2hostsMap = storages2hostsMaps.reduce(_ ++ _)
        (for {
          (nodeId, storage) <- node2storage
          value <- storages2hostsMap.get( storage.data )
          // Нет необходимости возвращать пустой набор данных:
          if value.nonEmpty
        } yield {
          nodeId -> value
        })
          .toMap
      }
    }
  }


  /** Аналог forCall() для dist-cdn.
    *
    * @param call Исходный url Call.
    * @param mediaIds media id в карте узлов в порядке приоритета.
    *                 Первый элемент - media-id дериватива, если это дериватив.
    *                 Последний элемент -- это всегда media id оригинала! Это нужно для балансировки по хостам, даже когда id дериватива задан.
    * @param mediaHostsMap Результат mediaHosts().
    * @return Обновлённый Call с абсолютной ссылкой внутри.
    */
  def forMediaCall1(call: Call, mediaHostsMap: Map[String, Seq[MHostInfo]], mediaIds: Seq[String]): Call = {
    call match {
      case ext: ExternalCall =>
        throw new IllegalArgumentException("External calls cannot be here. Check code, looks like this method called twice: " + ext)
      case _ =>
        def logPrefix = s"forMediaCall($mediaIds):"
        (for {
          mediaId <- mediaIds.iterator
          hosts   <- mediaHostsMap.get( mediaId )
          host    <- chooseMediaHost(mediaIds.last, hosts)
        } yield {
          val url = distNodeCdnUrlNoCheck(host, call)
          LOGGER.trace(s"$logPrefix URL gen ok\n mediaId = $mediaId\n host = $host\n url => $url\n mediaHosts[${mediaHostsMap.size}] = ${mediaHostsMap.keys.mkString(", ")}")
          new ExternalCall( url )
        })
          // Отработать запасной вариант, когда внезапно нет хостов:
          .nextOption()
          .getOrElse {
            // debug, because lost files may occur. It not so unusual.
            LOGGER.debug(s"$logPrefix Not found any dist-CDN mediaHost[${mediaHostsMap.size}];\n mediaIds =\n  [${mediaIds.mkString("\n  ")}]\n orig call = $call")
            call
          }
    }
  }


  /** Выбор (и балансировка) медиа-хоста среди множества этих самых хостов.
    * Стараемся равномерно балансировать нагрузку, и картинки одного
    *
    * @param nodeId ключ для балансировки по хостам, если узлов > 1.
    *               Обычно - чистый id узла, без всяких media-суффиксов id.
    * @param hosts Список хостов, по которым возможна балансировка.
    * @return Выбранный медиа-хост, когда hosts не пустой.
    */
  def chooseMediaHost(nodeId: String, hosts: Seq[MHostInfo]): Option[MHostInfo] = {
    OptionUtil.maybeOpt(hosts.nonEmpty) {
      if (hosts.lengthCompare(1) == 0) {
        // Если только 1 хост, то балансировка не нужна.
        hosts.headOption
      } else {
        // Балансируем запросы по хостам с помощью стабильного nodeId: остаток от деления по кол-ву доступных хостов.
        val hostsCount = hosts.size
        val hostIdx = Math.abs( nodeId.hashCode % hostsCount )
        // TODO Opt Тут постоянно вызывается сортировка, но seaweedFS вроде бы всегда присылает отсортированные ответы.
        // Очевидная идея, чтобы хранить отсортированный список на уровне swfsVolCache, но тогда возникает проблема, что непонятно, какая нода ближайшая по мнению мастера, сообщившего хосты в таком порядке.
        val choosenHost = hosts.sortBy(_.nameInt).apply(hostIdx)
        LOGGER.trace(s"chooseMediaHost($nodeId): Choosen host ${choosenHost.namePublic} from $hostsCount hosts: [${hosts.iterator.map(_.namePublic).mkString(" | ")}]")
        Some( choosenHost )
      }
    }
  }

  /** На мастере надо перезаписывать CDN-адреса для нод.
    * s2.nodes.suggest.io => s2-suggest.cdnvideo.net
    * На локалхосте этого всего не надо.
    *
    * @param host Исходный хостнейм, которому может потребоваться перезапись.
    * @return Переписанный, либо исходный, хостнейм.
    */
  def reWriteHostToCdn(host: String): String = {
    cdnConf.REWRITE_FROM_TO.fold(host) { case (from, to) =>
      host.replace(from, to)
    }
  }

}


@Singleton
final class CdnConf @Inject()(
                               injector: Injector
                             )
  extends MacroLogsDyn
{

  private val configuration = injector.instanceOf[Configuration]
  private def corsUtil = injector.instanceOf[CorsUtil]

  /** Прочитать из конфига список CDN-хостов для указанного протокола. */
  def getCdnHostsForProto(proto: String): List[String] = {
    configuration.getOptional[Seq[String]]("cdn.hosts." + proto)
      .fold (List.empty[String]) (_.toList)
  }

  /** Карта протоколов и списков CDN-хостов, которые готовые обслуживать запросы. */
  val CDN_PROTO_HOSTS: Map[String, List[String]] = {
    configuration
      .getOptional[Seq[String]]("cdn.protocols")
      .fold [IterableOnce[String]] (HttpConst.Proto.HTTP :: HttpConst.Proto.HTTPS :: Nil) { protosRaw =>
        protosRaw
          .iterator
          .map(_.trim.toLowerCase)
      }
      .iterator
      .map { proto =>
        proto -> getCdnHostsForProto(proto)
      }
      .filter { _._2.nonEmpty }
      .toMap
  }

  /** Раздавать ли шрифты через CDN? Дергается из шаблонов. Если Cors отключен, то этот параметр тоже отключается. */
  val FONTS_ENABLED: Boolean = {
    corsUtil.IS_ENABLED &&
      configuration.getOptional[Boolean]("cdn.fonts.enabled").getOrElseTrue
  }

  /** Отключено использование CDN на хостах: */
  val DISABLED_ON_HOSTS: Set[String] = {
    configuration.getOptional[Seq[String]]("cdn.disabled.on.hosts")
      .fold (Set.empty[String]) (_.toSet)
  }

  def isNoCdnHosts = CDN_PROTO_HOSTS.isEmpty

  /** Правила перезаписи хостнеймов. */
  val REWRITE_FROM_TO: Option[(String, String)] = {
    for {
      fromToSeq <- configuration.getOptional[Seq[String]]("cdn.hosts.rewrite.from_to")
    } yield {
      val Seq(from, to) = fromToSeq.map(_.trim)
      LOGGER.info(s"Hostname rewriting activated: $from => $to")
      (from, to)
    }
  }


  // Печатаем карту в консоль при запуске.
  LOGGER.info {
    val sb = new StringBuilder("CDNs map (proto -> hosts...) is:")
    CDN_PROTO_HOSTS
      .foreach { case (proto, hosts) =>
        sb.append("\n  ")
          .append(proto)
          .append(": ")
        hosts foreach { host =>
          sb.append(host)
            .append(", ")
        }
      }
    sb.toString()
  }

  if (DISABLED_ON_HOSTS.nonEmpty)
    LOGGER.info(s"CDNs disabled on hosts: ${DISABLED_ON_HOSTS.mkString(", ")}")

}

/** Интерфейс для доступа к DI-полю с инстансом [[CdnUtil]]. */
trait ICdnUtilDi {
  def cdnUtil: CdnUtil
}
