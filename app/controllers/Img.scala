package controllers

import play.api.mvc._
import util.{PlayMacroLogsImpl, SiobixFs, DateTimeUtil}
import org.apache.hadoop.fs.Path
import io.suggest.util.SioConstants._
import play.api.libs.concurrent.Execution.Implicits._
import io.suggest.model.{MUserImgOrig, MImgThumb}
import org.joda.time.Instant
import play.api.Play.current
import util.acl.IsAuth
import _root_.util.img._
import play.api.libs.json._
import scala.concurrent.duration._
import models.MPictureTmp
import io.suggest.model.ImgWithTimestamp
import net.sf.jmimemagic.Magic
import scala.concurrent.Future
import views.html.img._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.04.13 14:45
 * Description: Управление картинками, относящихся к поисковой выдаче и к разным другим вещам.
 * Изначально контроллер служил только для превьюшек картинок, и назывался "Thumb".
 */

object Img extends SioController with PlayMacroLogsImpl with TempImgSupport {

  import LOGGER._

  val domainsRoot   = SiobixFs.siobix_out_path
  val thumbsSubpath = new Path(THUMBS_SUBDIR)

  val CACHE_THUMB_CLIENT_SECONDS = {
    current.configuration.getInt("img.thumb.cache.client.seconds") getOrElse 36000
  }

  /** Сколько времени кешировать temp-картинки на клиенте. */
  val TEMP_IMG_CACHE_SECONDS = {
    val cacheMinutes = current.configuration.getInt("img.temp.cache.client.minutes") getOrElse 10
    val cacheDuration = cacheMinutes.minutes
    cacheDuration.toSeconds.toInt
  }


  /** Сколько времени можно кешировать на клиенте оригинал картинки. */
  val CACHE_ORIG_CLIENT_SECONDS = {
    val cacheDuration = current.configuration.getInt("img.orig.cache.client.hours").map(_.hours) getOrElse 2.days
    cacheDuration.toSeconds.toInt
  }

  /**
   * Выдать картинку из HDFS. Используется для визуализации выдачи.
   * Валидность параметров проверяется в роутере регэкспами.
   * @param dkey Ключ домена. Возможно содержит www и иной мусор.
   * @param imageId Хеш-ключ картинки в хранилище домена.
   * @return
   */
  def getThumb(dkey:String, imageId:String) = Action.async { implicit request =>
    suppressQsFlood(routes.Img.getThumb(dkey, imageId)) {
      MImgThumb.getThumbById(imageId) map {
        case Some(its) =>
          serveImgBytes(its, CACHE_THUMB_CLIENT_SECONDS)

        case None =>
          info(s"getThumb($dkey, $imageId): 404 Not found")
          imgNotFound
      }
    }
  }


  /**
   * Раздача оригиналов сохраненных в HBase картинок.
   * @param imgId id картинки.
   * @return Оригинал картинки.
   */
  def getOrig(imgId: String) = Action.async { implicit request =>
    suppressQsFlood(routes.Img.getOrig(imgId)) {
      MUserImgOrig.getById(imgId) map {
        case Some(its) =>
          serveImgBytes(its, CACHE_ORIG_CLIENT_SECONDS)

        case None =>
          info(s"getOrig($imgId): 404")
          imgNotFound
      }
    }
  }

  /** Обслуживание картинки. */
  private def serveImgBytes(its: ImgWithTimestamp, cacheSeconds: Int)(implicit request: RequestHeader) = {
    // rfc date не содержит миллисекунд. Нужно округлять таймштамп, чтобы был 000 в конце.
    val ims = its.timestamp % 1000L
    val ts0 = new Instant(its.timestamp - ims) // не lazy, ибо всё равно понадобиться хотя бы в одной из веток.
    val isCached = request.headers.get(IF_MODIFIED_SINCE) flatMap {
        DateTimeUtil.parseRfcDate
      } exists { dt =>
        !(ts0 isAfter dt)
      }
    if (isCached) {
      trace("serveImg(): 304 Not Modified")
      NotModified

    } else {
      trace(s"serveImg(): 200 OK. size = ${its.img.length} bytes")
      // Бывает, что в базе лежит не jpeg, а картинка в другом формате. Это тоже учитываем:.
      val magicMatch = Magic.getMagicMatch(its.img)
      Ok(its.img)
        .as(magicMatch.getMimeType)
        .withHeaders(
          LAST_MODIFIED -> DateTimeUtil.df.print(ts0),
          CACHE_CONTROL -> ("public, max-age=" + cacheSeconds)
        )
    }
  }

  /** Загрузка сырой картинки для дальнейшей базовой обработки (кадрирования).
    * Картинка загружается в tmp-хранилище, чтобы её можно было оттуда оперативно удалить и иметь реалтаймовый доступ к ней. */
  def handleTempImg = IsAuth(parse.multipartFormData) { implicit request =>
    _handleTempImg(OrigImageUtil, marker = None)
  }

  /** Раздавалка картинок, созданных в [[handleTempImg]]. */
  def getTempImg(filename: String) = IsAuth.async { implicit request =>
    suppressQsFlood(routes.Img.getTempImg(filename)) {
      // Надо бы добавить сюда поддержку if-modifier-since...
      MPictureTmp.find(filename) match {
        case Some(mptmp) =>
          val f = mptmp.file
          Ok.sendFile(f, inline = true)
            .withHeaders(
              LAST_MODIFIED -> DateTimeUtil.df.print(f.lastModified),
              CACHE_CONTROL -> ("public, max-age=" + TEMP_IMG_CACHE_SECONDS)
            )

        case None => imgNotFound
      }
    }
  }

  /**
   * Раздача произвольных картинок без проверки прав.
   * @param imgId ключ картинки
   * @return Один из различных экшенов обработки.
   */
  def getImg(imgId: String): Action[AnyContent] = {
    val iik = ImgIdKey(imgId)
    if (iik.isValid) {
      iik match {
        case tiik: TmpImgIdKey  => getTempImg(imgId)
        case oiik: OrigImgIdKey => getOrig(imgId)
      }
    } else {
      trace(s"invalid img id: " + iik)
      actionImgNotFound
    }
  }

  private def actionImgNotFound = Action { imgNotFound }
  private def imgNotFound = NotFound("No such image")

  /**
   * Для подавления http get flood атаки через запросы с приписыванием рандомных qs
   * и передачи ссылок публичным http-фетчерам.
   * @param onSuccess Если реквест прошел проверку, то тут генерация результата.
   * @param req Исходный реквест.
   * @return
   * @see [[https://www.linux.org.ru/forum/security/10389031]]
   * @see [[http://habrahabr.ru/post/215233/]]
   */
  private def suppressQsFlood(onProblem: => Call)(onSuccess: => Future[Result])(implicit req: RequestHeader): Future[Result] = {
    // TODO Надо отрабатывать неявно пустую qs (когда в ссылке есть ?, по после него конец ссылки).
    val rqs = req.rawQueryString
    if (rqs.length <= 1) {
      onSuccess
    } else {
      debug("suppressQsFlood(): Query string found in request, but it should not. Sending redirects... qs=" + rqs)
      MovedPermanently(onProblem.url)
    }
  }

  /** Выдать json ошибку по поводу картинки. */
  def jsonImgError(msg: String) = JsObject(Seq(
    "status" -> JsString("error"),
    "msg"    -> JsString(msg) // TODO Добавить бы поддержку lang.
  ))


  /** Ответ на присланную для предобработки картинку. */
  def jsonTempOk(filename: String) = {
    JsObject(List(
      "status"     -> JsString("ok"),
      "image_key"  -> JsString(filename),
      "image_link" -> JsString(routes.Img.getTempImg(filename).url)
    ))
  }

  /** Отрендерить оконный интерфейс для кадрирования картинки. */
  def imgCrop(imgId: String, width: Int, height: Int) = IsAuth.async { implicit request =>
    Ok(cropTpl(imgId, width, height))
  }

}
