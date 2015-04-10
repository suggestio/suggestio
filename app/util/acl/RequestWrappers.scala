package util.acl

import java.net.InetAddress

import models.event.MEvent
import models.event.search.MEventsSearchArgs
import models.msc.ScJsState
import models.usr.MPerson
import play.api.http.HeaderNames
import play.core.parsers.FormUrlEncodedParser
import util.PlayMacroLogsImpl
import util.acl.PersonWrapper._
import play.api.mvc._
import models._
import util.async.AsyncUtil
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.db.DB
import play.api.Play.current
import util.SiowebEsUtil.client


object SioWrappedRequest {
  implicit def request2sio[A](request: Request[A]): SioWrappedRequest[A] = {
    new SioWrappedRequest[A](request)
  }
}

/** Экранизация WrappedRequest[A] с примесями нужд s.io. */
class SioWrappedRequest[A](request: Request[A]) extends WrappedRequest(request) with SioRequestHeader

/** Абстрактный реквест, в рамках которого содержится инфа о текущем sio-юзере. */
abstract class AbstractRequestWithPwOpt[A](request: Request[A])
  extends SioWrappedRequest(request) with RichRequestHeader


object SioRequestHeader extends PlayMacroLogsImpl {

  import LOGGER._

  /** Скомпиленный регэксп для сплиттинга значения X_FORWARDED_FOR. */
  val X_FW_FOR_SPLITTER_RE = ",\\s*".r


  def lastForwarded(raw: String): String = {
    val splits = X_FW_FOR_SPLITTER_RE.split(raw)
    if (splits.length == 0)
      raw
    else
      splits.last
  }

  def firstForwarded(raw: String): String = {
    val splits = X_FW_FOR_SPLITTER_RE.split(raw)
    if (splits.length == 0)
      raw
    else
      splits.head
  }

  /** Последний из списка форвардов. Там обычно содержится оригинальный ip, если клиент не докинул туда свой. */
  def firstForwardedAddr(raw: String): String = {
    lazy val logPrefix = s"firstForwardedAddr($raw): "
    val splitsIter = X_FW_FOR_SPLITTER_RE.split(raw)
      .iterator
      .filter { addr =>
        try {
          // TODO Доставать только ip адреса, отсеивать хлам.
          InetAddress.getByName(addr)
          true
        } catch {
          case ex: Throwable =>
            warn(logPrefix + "Invalid forwarded address: " + addr)
            false
        }
      }
    if (splitsIter.hasNext) {
      splitsIter.next()
    } else {
      warn(logPrefix + "No more forwarding tokens. Returning raw value.")
      raw
    }
  }

  implicit def request2sio[A](request: Request[A]): SioRequestHeader = {
    SioWrappedRequest.request2sio(request)
  }
}


object RichRequestHeader {

  def apply(rh: RequestHeader): Future[RichRequestHeader] = {
    val _pwOpt = PersonWrapper.getFromRequest(rh)
    SioReqMd.fromPwOpt(_pwOpt).map { srm =>
      new RequestHeaderWrapper with RichRequestHeader {
        override def underlying   = rh
        override def pwOpt        = _pwOpt
        override def sioReqMd     = srm
      }
    }
  }
}

/** Вынос полей из [[AbstractRequestWithPwOpt]]. */
trait RichRequestHeader extends RequestHeader {
  def pwOpt: PwOpt_t
  def sioReqMd: SioReqMd
  def isSuperuser = PersonWrapper isSuperuser pwOpt
  def isAuth = pwOpt.isDefined
}


/** Расширение play RequestHeader функциями S.io. */
trait SioRequestHeader extends RequestHeader {
  import SioRequestHeader._

  /**
   * remote address может содержать несколько адресов через ", ". Например, если клиент послал своё
   * значение X_FORWARDED_FOR, то nginx допишет через запятую новый адрес и прокинет сюда.
   * Тут мы это исправляем, чтобы не было проблем в будущем.
   */
  abstract override lazy val remoteAddress: String = {
    headers
      .get(HeaderNames.X_FORWARDED_FOR)
      .map { firstForwardedAddr }
      .getOrElse { super.remoteAddress }
  }

  /** Кравлеры при индексации !#-страниц используют ссылки, содержащие что-то типа "?_escaped_fragment_=...". */
  lazy val ajaxEscapedFragment: Option[String] = {
    queryString
      .get("_escaped_fragment_")
      .flatMap(_.headOption)
      // TODO Нужно делать URL unescape тут?
  }

  /** Переданное js-состояние скрипта выдачи, закинутое в ajax escaped_fragment. */
  lazy val ajaxJsScState: Option[ScJsState] = {
    ajaxEscapedFragment
      .flatMap { raw =>
        try {
          val r = FormUrlEncodedParser.parseNotPreservingOrder(raw)
          Some(r)
        } catch {
          case ex: Exception =>
            LOGGER.debug("Failed to parse ajax-escaped fragment.", ex)
            None
        }
      }
      .flatMap { aefMap =>
        val qsb = ScJsState.qsbStandalone
        qsb.bind("", aefMap)
      }
      .flatMap {
        case Right(res) =>
          Some(res)
        case Left(errMsg) =>
          LOGGER.warn(s"_geoSiteResult(): Failed to bind ajax escaped_fragment '$ajaxEscapedFragment' from '$remoteAddress': $errMsg")
          None
      }
  }

}


/**
 * Wrapped-реквест для передачи pwOpt.
 * @param pwOpt Опциональный PersonWrapper.
 * @param request Исходнный реквест.
 * @tparam A Подтип реквеста.
 */
case class RequestWithPwOpt[A](pwOpt: PwOpt_t, request: Request[A], sioReqMd: SioReqMd)
  extends AbstractRequestWithPwOpt(request)


/** Админство магазина. */
abstract class AbstractRequestForShopAdm[A](request: Request[A]) extends AbstractRequestWithPwOpt(request) {
  def shopId: String
}


/** Метаданные, относящиеся запросу. Сюда попадают данные, которые необходимы везде и требует асинхронных действий.
  * @param usernameOpt Отображаемое имя юзера, если есть. Формируются на основе данных сессии и данных из
  *                    [[MPerson]] и [[models.MPersonIdent]].
  * @param billBallanceOpt Текущий денежный баланс узла.
  * @param nodeUnseenEvtsCnt Кол-во новых событий у узла.
  */
case class SioReqMd(
  usernameOpt       : Option[String] = None,
  billBallanceOpt   : Option[MBillBalance] = None,
  nodeUnseenEvtsCnt : Option[Int] = None
)
object SioReqMd {
  /** Простая генерация srm на основе юзера. */
  def fromPwOpt(pwOpt: PwOpt_t): Future[SioReqMd] = {
    PersonWrapper.findUserName(pwOpt) map { usernameOpt =>
      SioReqMd(usernameOpt = usernameOpt)
    }
  }

  /** Генерация srm для юзера в рамках личного кабинета. */
  def fromPwOptAdn(pwOpt: PwOpt_t, adnId: String): Future[SioReqMd] = {
    // Получить кол-во непрочитанных сообщений для узла.
    val newEvtsCntFut: Future[Int] = {
      val args = MEventsSearchArgs(
        ownerId     = Some(adnId),
        onlyUnseen  = true
      )
      MEvent.dynCount(args)
        .map { _.toInt }
    }
    // Получить баланс узла.
    val bbOptFut = Future {
      DB.withConnection { implicit c =>
        MBillBalance.getByAdnId(adnId)
      }
    }(AsyncUtil.jdbcExecutionContext)
    // Собрать результат.
    for {
      usernameOpt <- PersonWrapper.findUserName(pwOpt)
      bbOpt       <- bbOptFut
      newEvtCnt   <- newEvtsCntFut
    } yield {
      SioReqMd(
        usernameOpt       = usernameOpt,
        billBallanceOpt   = bbOpt,
        nodeUnseenEvtsCnt = Some(newEvtCnt)
      )
    }
  }

}


/** Враппер над RequestHeader. */
trait RequestHeaderWrapper extends RequestHeader {
  def underlying: RequestHeader
  override def id             = underlying.id
  override def secure         = underlying.secure
  override def uri            = underlying.uri
  override def remoteAddress  = underlying.remoteAddress
  override def queryString    = underlying.queryString
  override def method         = underlying.method
  override def headers        = underlying.headers
  override def path           = underlying.path
  override def version        = underlying.version
  override def tags           = underlying.tags
}

case class RequestHeaderAsRequest(underlying: RequestHeader) extends Request[Nothing] with RequestHeaderWrapper {
  override def body: Nothing = {
    throw new UnsupportedOperationException("This is request headers wrapper. Body never awailable here.")
  }
}


