package util.acl

import java.time.Instant

import controllers.Assets
import io.suggest.dt.DateTimeUtil
import io.suggest.util.logs.MacroLogsImplLazy
import javax.inject.{Inject, Singleton}
import models.req.MFileReq
import play.api.{Environment, Mode}
import play.api.inject.Injector
import play.api.mvc.{ActionRefiner, Result, Results}
import play.mvc.Http.HeaderNames
import japgolly.univeq._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.02.2020 16:36
  * Description: Накрутка для файло-раздавалок, чтобы организовать корректную поддержку HTTP 304 NotModified.
  */
@Singleton
final class IsFileNotModified @Inject()(
                                         injector: Injector,
                                       )
  extends MacroLogsImplLazy
{

  implicit private lazy val ec = injector.instanceOf[ExecutionContext]
  private def environment = injector.instanceOf[Environment]


  /** Сколько времени можно кешировать на клиенте результат dynImg() ? */
  private def CACHE_DYN_IMG_CLIENT_SECONDS = {
    val cacheDuration = if (environment.mode == Mode.Prod) {
      365.days
    } else {
      30.seconds
    }
    cacheDuration.toSeconds
  }


  /** Immutable not-modified cache-control header. */
  def CACHE_CONTROL_HDR =
    HeaderNames.CACHE_CONTROL -> s"public, max-age=$CACHE_DYN_IMG_CLIENT_SECONDS, immutable"

  /** Часто-используемое значение: */
  private def CACHE_CONTROL_HDRS = CACHE_CONTROL_HDR :: Nil


  def isNotModifiedSinceCached(modelTstampMs: Instant, ifModifiedSince: String): Boolean = {
    DateTimeUtil
      .parseRfcDate(ifModifiedSince)
      .exists { dt => !modelTstampMs.isAfter(dt.toInstant) }
  }


  def maybeNotModified[A](ctx304: Ctx304[A]): Option[Result] = {
    lazy val logPrefix = s"maybeNotModified(${ctx304.request.remoteClientAddress})#${System.currentTimeMillis()}:"

    if (ctx304.fileEtagExpectedOpt.isEmpty)
      LOGGER.warn(s"Missing file hash for etagging, file-node#${ctx304.request.derivativeOrOrig.idOrNull}\n fileMeta=${ctx304.request.edgeMedia.file}")

    val resOpt = (for {
      // Использовать sha1-хэш, который используется для ETag
      fileEtagExpected <- ctx304.fileEtagExpectedOpt
      etagsReqHdr <- ctx304.request.headers.get( HeaderNames.IF_NONE_MATCH )
      // Отфильтровываем какое-то полу-стандартное значение, которое гипотетически может прислать клиент, чтобы тут сразу перейти на ветвь сверки Last-Modified.
      if etagsReqHdr !=* "*"
      // ETag задан и доступен, больше никаких условий тут быть не должно: никаких запасных проверок Last-Modified.
    } yield {
      LOGGER.trace(s"$logPrefix etag expected=$fileEtagExpected req.header=${etagsReqHdr}")
      etagsReqHdr
        .split(",")
        .find(_.trim ==* fileEtagExpected)
        .map { _ =>
          LOGGER.trace(s"$logPrefix 304 NotModified, ${etagsReqHdr}")
          // В исходниках Assets-контроллера play, etag-успех возвращает 304 с этой пачкой хидеров:
          with304Headers( ctx304, Results.NotModified )
        }
    })
      .getOrElse {
        // ETag не найден или недоступен. Проверка по Last-Modified:
        for {
          ifModifiedSinceStr <- ctx304.request.headers.get( HeaderNames.IF_MODIFIED_SINCE )
          ifModifiedSince    <- Assets.parseModifiedDate(ifModifiedSinceStr)
          if !ctx304.fileLastModifiedZ.toInstant.isAfter( ifModifiedSince.toInstant )
        } yield {
          LOGGER.trace(s"$logPrefix NotModified, because ifModifinedSince=$ifModifiedSince fileLastMod=${ctx304.fileLastModifiedZ}")
          Results.NotModified
        }
      }

    if (resOpt.isEmpty)
      LOGGER.trace(s"$logPrefix Modified, returning None following next action.")

    resOpt
  }


  /** ActionRefiner, который запрещает исполнения экшена, возвращая 304 при совпадении необходимых условий.
    * Этот refiner не выставляет и не может выставлять кэширующие header'ы ответа.
    */
  class Refiner extends ActionRefiner[MFileReq, Ctx304] {

    override protected def executionContext = ec

    override protected def refine[A](request: MFileReq[A]): Future[Either[Result, Ctx304[A]]] = {
      // First check etag. Important, if there is an If-None-Match header, we MUST not check the
      // If-Modified-Since header, regardless of whether If-None-Match matches or not. This is in
      // accordance with section 14.26 of RFC2616.
      val ctx304 = Ctx304()( request )

      val respE = maybeNotModified( ctx304 )
        // Если NotModified-проверки не удались, то запустить экшен на испролнение.
        .toLeft( ctx304 )

      Future.successful( respE )
    }

  }


  def with304Headers( ctx: Ctx304[_], result: Result ): Result = {
    var hdrsAcc = CACHE_CONTROL_HDRS
    for (etag <- ctx.fileEtagExpectedOpt)
      hdrsAcc ::= (HeaderNames.ETAG -> etag)

    result
      .withHeaders( hdrsAcc: _* )
      .withDateHeaders(
        HeaderNames.LAST_MODIFIED -> ctx.fileLastModifiedZ,
      )
  }

}


case class Ctx304[A]()( implicit val request: MFileReq[A] ) {

  val fileEtagExpectedOpt = for {
    dlHash <- request.edgeMedia.file.hashesHex.dlHash
  } yield {
    s""""${dlHash.hexValue}""""
  }

  val fileLastModifiedZ = request.edge.info.dateNi
    .getOrElse( request.derivativeOrOrig.meta.basic.dateEditedOrCreated )
    .toZonedDateTime

}
