package models.mctx

import java.net.IDN
import java.time.{Instant, OffsetDateTime, ZoneId}
import com.google.inject.assistedinject.Assisted

import javax.inject.{Inject, Singleton}
import io.suggest.playx._
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.ctx.{CtxData, MCtxId, MCtxIds}
import io.suggest.dev.{MScreen, MScreenJvm}
import io.suggest.n2.node.MNode
import io.suggest.proto.http.HttpConst
import io.suggest.sc.ScConstants
import io.suggest.text.util.UrlUtil
import io.suggest.util.logs.MacroLogsImpl
import models.req.IReqHdr
import models.usr.MSuperUsers
import play.api.{Application, Configuration, Environment}
import play.api.http.HeaderNames._
import play.api.i18n.Messages
import play.api.inject.Injector
import play.api.mvc.Call
import util.adv.AdvUtil
import util.cdn.CdnUtil
import util.domain.Domains3pUtil
import util.i18n.JsMessagesUtil
import util.img.DynImgUtil
import util.jsa.init.ITargets
import util.n2u.N2NodesUtil
import util.support.SupportUtil

import scala.util.matching.Regex

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.13 12:10
 * Description: Объект контекста запроса для прозрачной передачи состояния запроса из контроллеров в шаблоны
 * и между шаблонами. Контроллеры просто расширяют этот трайт, а шаблоны просто объявляют тип возвращаемого значения
 * метода в implicit-списке параметров.
 */

/** Статическая поддержка для экземпляров [[Context]] и прочих вещей. В основном, тут всякие константы. */
@Singleton
final class ContextUtil @Inject() (
                                    env           : Environment,
                                    domains3pUtil : Domains3pUtil,
                                    configuration : Configuration
                                  ) {

  val isIpadRe = "iPad".r.unanchored

  /** Регэксп для поиска в query string параметра, который хранит параметры клиентского экрана. */
  val SCREEN_ARG_NAME_RE = s"a\\.(c\\.)?${ScConstants.ReqArgs.SCREEN_FN}(creen)?".r


  /** Самое дефолтовое имя главного домена. */
  private def MAIN_DOMAIN_DFLT = "suggest.io"

  /** На dev-системах удобно глобально выключить https в конфиге. */
  val HTTPS_DISABLED: Boolean = configuration.getOptional[Boolean]("sio.https.disabled").getOrElseFalse

  def HTTPS_ENABLED: Boolean = !HTTPS_DISABLED

  /** Дефолтовый протокол работы suggest.io. */
  def PROTO: String = {
    if (HTTPS_DISABLED) "http" else "https"
  }

  /** Хост:порт сайта suggest.io. */
  val HOST_PORT: String = configuration.getOptional[String]("sio.hostport.dflt").getOrElse(MAIN_DOMAIN_DFLT)

  /** Префикс абсолютных ссылок на сайт. */
  val URL_PREFIX: String = PROTO + "://" + HOST_PORT


  /** Основной хост и порт, на котором крутится выдача sio-market. */
  def SC_HOST_PORT: String = HOST_PORT
  def SC_URL_PREFIX: String = URL_PREFIX

  /** Генерация абсолютной ссылки через выдачу на основе строке относительной ссылки. */
  def toScAbsUrl(relUrl: String): String = {
    SC_URL_PREFIX + relUrl
  }
  /** Генерация абсолютной ссылки через выдачу на основе экземпляра Call. */
  def toScAbsUrl(call: Call): String = {
    toScAbsUrl( call.url )
  }

  /** Хост и порт, на котором живёт часть сервиса с ограниченным доступом. */
  def LK_HOST_PORT: String = HOST_PORT
  def LK_URL_PREFIX: String = URL_PREFIX


  /** Бывает, что необходимо заменить локалхост на 127.0.0.1. Например, при разработке под твиттер.
    * @param source Исходная строка, т.е. ссылка, или её префикс или хостнейм.
    * @return Подправленная исходная строка.
    */
  def devReplaceLocalHostW127001(source: String): String = {
    if (env.mode.isDev)
      source.replaceFirst("localhost", "127.0.0.1")
    else
      source
  }


  /** Список собственных хостов (доменов) системы suggest.io. */
  val SUGGESTIO_DOMAINS: Set[String] = {
    val hosts = Set.empty[String] +
      SC_HOST_PORT +
      MAIN_DOMAIN_DFLT +
      HOST_PORT +
      LK_HOST_PORT

    for (h <- hosts) yield {
      IDN.toASCII(
        UrlUtil.urlHostStripPort( h )
      )
    }
  }

  /** Относится ли хост в запросе к собственным хостам suggest.io. */
  def isSuggestioDomain(myHost: String): Boolean = {
    // По идее, надо бы фильтровать тут левые адреса, но пока надеемся на nginx
    // и на около-нулевую возможную опасность возможной уязвимости на фоне блокирующего резолва внутрях InetAddress.
    /*val inet = InetAddress.getByName(myHost)
    inet.isLinkLocalAddress ||
      inet.isLoopbackAddress ||
      inet.isMulticastAddress || */
      SUGGESTIO_DOMAINS.contains(myHost)
  }

  def domainNode3pOptFut(request: IReqHdr) = {
    domains3pUtil.find3pDomainNode(
      domain = request.headers
        .get(ORIGIN)
        .map( url => UrlUtil.urlHostStripPort(UrlUtil.url2dkey( url )) )
        .getOrElse( request.domain )
    )
  }

}

/** Интерфейс для DI-поля с инстансом [[ContextUtil]] внутри. */
trait IContextUtilDi {
  def ctxUtil: ContextUtil
}


/** Трейт-аддон для контроллеров, которым нужен доступ к сборке контекстов. */
trait ContextT { this: ITargets =>

  def contextFactory: Context2Factory

  /**
   * Выдать контекст. Неявно вызывается при вызове шаблона из контроллера.
   * @return Экземпляр [[Context]].
   */
  implicit final def getContext2(implicit
                                 request  : IReqHdr,
                                 messages : Messages,
                                 ctxData  : CtxData = CtxData.empty): Context = {
    // Получить js init targets с уровня контроллера, объеденить с остальными, залить их в data.
    val ctxData1 = ctxData.appendJsInitTargetsAll(
      jsiTgs(request),
    )
    // Собрать контекст с обновлёнными данными в ctxData.
    contextFactory
      .create(request, messages, ctxData1)
  }

}


object Context extends MacroLogsImpl {

  implicit final class ContextExt( private val ctx: Context ) extends AnyVal {

    def maybeLocalizeToUser(personOpt: Option[MNode], langCode2MessagesMap: Map[String, Messages]): Context = {
      (for {
        personNode    <- personOpt.iterator
        langCode      <- personNode.meta.basic.langs
        langMessages  <- langCode2MessagesMap.get( langCode )
      } yield {
        ctx.withMessages( langMessages )
      })
        .nextOption()
        .getOrElse {
          LOGGER.warn( s"localizeToUser(${personOpt.flatMap(_.id).orNull}): i18n failed for person, available langs = [${langCode2MessagesMap.keysIterator.mkString(", ")}]" )
          ctx
        }
    }

    /** Для быстрого задания значений r-параметров (path для возврата, см. routes) можно использовать этот метод. */
    def r = Some( ctx.request.uri )

    def userAgent: Option[String] = ctx.request.headers.get(USER_AGENT)

    def uaMatches(re: Regex): Boolean = {
      userAgent.exists { x =>
        re.pattern.matcher(x).find()
      }
    }

    def timeZone = ZoneId.systemDefault() // TODO Это не очень-то хорошая идея. Нужно из кукисов брать.

    def toOffsetTime(i: Instant): OffsetDateTime = {
      i.atZone( timeZone ).toOffsetDateTime
    }

    def protoUrlPrefix(protoOpt: Option[String] = None, host: String = ctx.request.host): String = {
      val sb = new StringBuilder(16)

      val P = HttpConst.Proto
      for (proto <- protoOpt) {
        sb.append( proto )
        if (ctx.request.isTransferSecure)
          sb.append('s')
        sb.append( P.COLON )
      }

      sb.append( P.CURR_PROTO )
        .append( host )
        .toString()
    }

    /** Собрать ссылку на веб-сокет с учетом текущего соединения. */
    def wsUrlPrefix: String = protoUrlPrefix( Some(HttpConst.Proto.WS) )

  }

}


/** Основная реализация контекста, с которой работают sio-контроллеры автоматически. */
final case class Context @Inject()(
                                    val api                          : ContextApi,
                                    @Assisted val data               : CtxData,
                                    @Assisted implicit val request   : IReqHdr,
                                    @Assisted implicit val messages  : Messages
                                  ) {

  def withMessages(messages: Messages) = copy(messages = messages)
  def withData(data1: CtxData) = copy(data = data1)

  // abstract val вместо def'ов в качестве возможной оптимизации обращений к ним со стороны scalac и jvm. Всегда можно вернуть def.

  /** Объект-timestamp текущего контекста. */
  lazy val instant = Instant.now()

  /** Целочисленный timestamp текущего контекста в миллисекундах. */
  lazy val timestamp: Long = instant.toEpochMilli

  /** Текущие дата-время. */
  implicit lazy val now: OffsetDateTime = {
    // TODO Текущее время сейчас привязано к часовому поясу сервера/jvm. Это не хорошо.
    // TODO Нужно выбирать часовой пояс исходя из текущего клиента. Но это наверное будет Future[OffsetDateTime/ZonedDateTime]?
    instant
      .atZone( this.timeZone )
      .toOffsetDateTime
  }

  lazy val isIpad: Boolean = this.uaMatches(api.ctxUtil.isIpadRe)


  /** Рандомный id, существующий в рамках контекста.
    * Использутся, когда необходимо как-то индентифицировать весь текущий рендер (вебсокеты, например). */
  lazy val ctxId: MCtxId = api.mCtxIds( request.user.personIdOpt )
  lazy val ctxIdStr: String = MCtxId.intoString(ctxId)


  /** Параметры экрана клиентского устройства. Эти данные можно обнаружить внутри query string. */
  lazy val deviceScreenOpt: Option[MScreen] = {
    (for {
      (k, v) <- request.queryString.iterator
      // TODO Искать по ключу, а не перебирать все ключи?
      if api.ctxUtil.SCREEN_ARG_NAME_RE.pattern.matcher(k).matches()
      bindedE <- MScreenJvm.devScreenQsb.bind(k, new Map.Map1(k, v))
      mscreen <- bindedE.toOption
    } yield mscreen)
      .nextOption()
  }

  /** Checking if current host is related to suggest.io hosts. */
  lazy val isSuggestioDomain = api.ctxUtil.isSuggestioDomain( request.domain )

  /** Shared 3p-domain node search between CorsUtil, ScSite and others. */
  lazy val domainNode3pOptFut = api.ctxUtil.domainNode3pOptFut( request )

}


/** Шаблонам бывает нужно залезть в util'ь, модели или ещё куда-нить.
  * DI препятствует этому, поэтому необходимо обеспечивать доступ с помощью класса-костыля. */
final class ContextApi @Inject() (
                                   injector: Injector,
                                 )
  extends IContextUtilDi
{
  override lazy val ctxUtil = injector.instanceOf[ContextUtil]
  lazy val cdn = injector.instanceOf[CdnUtil]
  lazy val dynImgUtil = injector.instanceOf[DynImgUtil]
  lazy val n2NodesUtil = injector.instanceOf[N2NodesUtil]
  lazy val mSuperUsers = injector.instanceOf[MSuperUsers]
  lazy val jsMessagesUtil = injector.instanceOf[JsMessagesUtil]
  lazy val mCtxIds = injector.instanceOf[MCtxIds]
  lazy val advUtil = injector.instanceOf[AdvUtil]
  lazy val supportUtil = injector.instanceOf[SupportUtil]
  lazy val current = injector.instanceOf[Application]
  lazy val domains3pUtil = injector.instanceOf[Domains3pUtil]
}


/** Guice factory для сборки контекстов с использованием DI.
  * У шаблонов бывает необходимость залезть в util/models к статическим вещам. */
trait Context2Factory {
  /**
   * Сборка контекста, код метода реализуется автоматом через Guice Assisted inject.
   * @see [[GuiceDiModule]] для тюнинга assisted-линковки контекста.
   */
  def create(implicit request: IReqHdr, messages: Messages, ctxData: CtxData): Context
}
