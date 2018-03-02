package util.cdn

import javax.inject.{Inject, Singleton}

import controllers.routes
import io.suggest.file.up.MFile4UpProps
import io.suggest.model.n2.media.MMedia
import io.suggest.model.n2.media.storage._
import io.suggest.model.n2.media.storage.swfs.{SwfsStorage, SwfsStorages, SwfsVolumeCache}
import io.suggest.playx.ExternalCall
import io.suggest.proto.HttpConst
import io.suggest.swfs.client.proto.lookup.IVolumeLocation
import io.suggest.url.MHostInfo
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.Context
import models.mup.MSwfsFidInfo
import play.api.Configuration
import play.api.mvc.Call
import util.up.UploadUtil
import japgolly.univeq._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.10.14 18:31
  * Description: Утиль для работы с CDN.
  *
  * 2018-03-01 В дополнение к первому варианту CDN добавлена утиль для dist+cdn.
  * Сюда замержен небольшой DistUtil.
  */
@Singleton
class CdnUtil @Inject() (
                          corsUtil                  : CorsUtil,
                          configuration             : Configuration,
                          iMediaStorages            : IMediaStorages,
                          uploadUtil                : UploadUtil,
                          swfsVolumeCache           : SwfsVolumeCache,
                          implicit private val ec   : ExecutionContext
                        )
  extends MacroLogsImpl
{

  /** Прочитать из конфига список CDN-хостов для указанного протокола. */
  def getCdnHostsForProto(proto: String): List[String] = {
    configuration.getOptional[Seq[String]]("cdn.hosts." + proto)
      .fold (List.empty[String]) (_.toList)
  }

  /** Карта протоколов и списков CDN-хостов, которые готовые обслуживать запросы. */
  val CDN_PROTO_HOSTS: Map[String, List[String]] = {
    configuration.getOptional[Seq[String]]("cdn.protocols")
      .fold [TraversableOnce[String]] (HttpConst.Proto.HTTP :: HttpConst.Proto.HTTPS :: Nil) { protosRaw =>
        protosRaw
          .iterator
          .map(_.trim.toLowerCase)
      }
      .toIterator
      .map { proto =>
        proto -> getCdnHostsForProto(proto)
      }
      .filter { _._2.nonEmpty }
      .toMap
  }

  /** Раздавать ли шрифты через CDN? Дергается из шаблонов. Если Cors отключен, то этот параметр тоже отключается. */
  val FONTS_ENABLED: Boolean = {
    configuration.getOptional[Boolean]("cdn.fonts.enabled")
      .exists(_ && corsUtil.IS_ENABLED)
  }

  /** Отключено использование CDN на хостах: */
  val DISABLED_ON_HOSTS: Set[String] = {
    configuration.getOptional[Seq[String]]("cdn.disabled.on.hosts")
      .fold (Set.empty[String]) (_.toSet)
  }

  val HAS_ANY_CDN: Boolean = CDN_PROTO_HOSTS.nonEmpty


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

  if (DISABLED_ON_HOSTS.nonEmpty) {
    LOGGER.info(s"CDNs disabled on hosts: " + DISABLED_ON_HOSTS.mkString(", "))
  }


  /** Выбрать подходящий CDN-хост для указанного протокола. */
  def chooseHostForProto(protoLc: String): Option[String] = {
    CDN_PROTO_HOSTS
      .get(protoLc)
      .flatMap(_.headOption)    // TODO Выбирать рандомный хост из списка хостов.
  }

  def ctx2CdnHost(implicit ctx: Context): Option[String] = {
    chooseHostForProto( ctx.request.myProto )
  }

  /** Генератор вызовов к CDN или внутренних. */
  def forCall(c: Call)(implicit ctx: Context): Call = {
    if (!HAS_ANY_CDN || c.isInstanceOf[ExternalCall]) {
      c
    } else {
      val reqHost = ctx.request.host
      val urlPrefixOpt: Option[String] = if (DISABLED_ON_HOSTS.contains(reqHost)) {
        None
      } else {
        val protoLc = ctx.request.myProto
        for {
          cdnHost <- chooseHostForProto(protoLc)
          if {
            !DISABLED_ON_HOSTS.contains(reqHost) &&
              !(cdnHost equalsIgnoreCase reqHost)
          }
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
      import ctx.request
      val absUrl = call.absoluteURL()
      if (absUrl startsWith HttpConst.Proto.CURR_PROTO) {
        // Вот так бывает: протокол не указан, потому что forCall() больше не пишет протокол.
        // Значит, уже отсылка к CDN, и значит дописываем https:
        HttpConst.Proto.HTTPS_ + absUrl
      } else {
        absUrl
      }
    } else {
      call.url
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


  // TODO DIST_IMG Реализовать поддержку распределения media-файлов по нодам.

  /** Подготовить местечко для сохранения нового файла, вернув данные сервера для заливки файла.
    *
    * @param upProps Обобщённые данные по заливаемому файлу.
    * @return Фьючерс с результатом.
    */
  def assignDist(upProps: MFile4UpProps): Future[MAssignedStorage] = {
    val storageType = DIST_STORAGE
    val storageFacade = iMediaStorages.getModel( storageType ).asInstanceOf[SwfsStorages]
    val assignRespFut = storageFacade.assignNew()

    for {
      assignResp <- assignRespFut
    } yield {
      LOGGER.trace(s"assignDist[${System.currentTimeMillis()}]: Assigned ok:\n props = $upProps\n resp = $assignResp")
      val swfsAssignResp = assignResp._2
      MAssignedStorage(
        host    = swfsAssignResp.hostInfo,
        storage = assignResp._1
      )
    }
  }


  /** Проверка возможности аплоада прямо сюда на текущую ноду.
    *
    * @param storage Строка инфы в контексте данного хранилища.
    * @return Расширенные данные для аплоада, если Some.
    *         None, значит доступ закрыт.
    */
  def checkStorageForThisNode(storage: IMediaStorage): Future[Either[Seq[IVolumeLocation], MSwfsFidInfo]] = {
    // Распарсить Swfs FID из URL и сопоставить полученный volumeID с текущей нодой sio.
    lazy val logPrefix = s"checkStorageForThisNode($storage)#${System.currentTimeMillis()}:"

    storage match {
      case swfsStorage: SwfsStorage =>
        for {
          volLocs <- swfsVolumeCache.getLocations( swfsStorage.fid.volumeId )
        } yield {
          // Может быть несколько результатов, если у volume существуют реплики.
          // Нужно найти целевую мастер-шарду, которая располагается где-то очень близко к текущему локалхосту.
          val myExtHost = uploadUtil.MY_NODE_PUBLIC_URL
          volLocs
            .find { volLoc =>
              volLoc.publicUrl ==* myExtHost
              // Не проверяем nameInt/url, потому что там полу-рандомный порт swfs
            }
            .map { myVol =>
              LOGGER.trace(s"$logPrefix Ok, local vol = $myVol\n fid = ${swfsStorage.fid.toString}\n all vol locs = ${volLocs.mkString(", ")}")
              MSwfsFidInfo(swfsStorage.fid, myVol, volLocs)
            }
            .toRight {
              LOGGER.error(s"$logPrefix Failed to find vol#${swfsStorage.fid.volumeId} for fid='${swfsStorage.fid}' nearby. My=$myExtHost, storage=$storage Other available volumes considered non-local: ${volLocs.mkString(", ")}")
              volLocs
            }
        }
    }
  }


  /** Вернуть карту медиа-хосты для указанных media.
    *
    * @param medias Список интересующих media.
    * @return Карта mediaId -> hostname.
    */
  def mediasHosts(medias: Iterable[MMedia]): Future[Map[String, Seq[MHostInfo]]] = {
    if (medias.isEmpty) {
      Future.successful( Map.empty )
    } else {
      for {
        storages2hostsMap <- iMediaStorages.getStoragesHosts( medias.map(_.storage).toSet )
      } yield {
        LOGGER.trace(s"mediaHosts(${medias.size} medias): Done\n mediaIds = ${medias.iterator.map(_.idOrNull).mkString(", ")}\n storages = ${storages2hostsMap.keys.mkString(", ")}")
        medias
          .iterator
          .map { media =>
            media.id.get -> storages2hostsMap(media.storage)
          }
          .toMap
      }
    }
  }


  /** Аналог forCall() для dist-cdn.
    *
    * @param call Исходный url Call.
    * @param mediaIds id media-узла в карте узлов в порядке приоритета.
    * @param mediaHostsMap Результат mediaHosts().
    * @return Обновлённый Call с абсолютной ссылкой внутри.
    */
  def forMediaCall1(call: Call, mediaHostsMap: Map[String, Seq[MHostInfo]], mediaIds: TraversableOnce[String]): Call = {
    call match {
      case ext: ExternalCall =>
        throw new IllegalArgumentException("External calls cannot be here. Check code, looks like this method called twice: " + ext)
      case _ =>
        def logPrefix = s"forMediaCall($mediaIds):"
        val newCallIter = for {
          mediaId <- mediaIds.toIterator
          hosts   <- mediaHostsMap.get(mediaId)
          host    <- chooseMediaHost(mediaId, hosts)
        } yield {
          val url = distNodeCdnUrlNoCheck(host, call)
          LOGGER.trace(s"$logPrefix URL gen ok\n mediaId = $mediaId\n host = $host\n url => $url\n mediaHosts[${mediaHostsMap.size}] = ${mediaHostsMap.keys.mkString(", ")}")
          new ExternalCall( url )
        }
        // Отработать запасной вариант, когда внезапно нет хостов:
        newCallIter
          .toStream
          .headOption
          .getOrElse {
            LOGGER.warn(s"$logPrefix Not found any dist-CDN host\n mediaIds = [${mediaIds.mkString(" | ")}]\n mediaHosts = $mediaHostsMap\n orig call = $call")
            call
          }
    }
  }


  def chooseMediaHost(mediaId: String, hosts: Seq[MHostInfo]): Option[MHostInfo] = {
    // TODO Это неправильно. Надо выбирать на основе mediaId.
    hosts.headOption
  }

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

  /** На мастере надо перезаписывать CDN-адреса для нод.
    * s2.nodes.suggest.io => s2-suggest.cdnvideo.net
    * На локалхосте этого всего не надо.
    *
    * @param host Исходный хостнейм, которому может потребоваться перезапись.
    * @return Переписанный, либо исходный, хостнейм.
    */
  def reWriteHostToCdn(host: String): String = {
    REWRITE_FROM_TO.fold(host) { case (from, to) =>
      host.replaceAllLiterally(from, to)
    }
  }

}

/** Интерфейс для доступа к DI-полю с инстансом [[CdnUtil]]. */
trait ICdnUtilDi {
  def cdnUtil: CdnUtil
}
