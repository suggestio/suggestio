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

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.04.13 14:45
 * Description: Управление картинками, относящихся к поисковой выдаче и к разным другим вещам.
 * Изначально контроллер служил только для превьюшек картинок, и назывался "Thumb".
 */

object Img extends SioController with PlayMacroLogsImpl {

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
    MImgThumb.getThumbById(imageId) map {
      case Some(its) =>
        serveImgBytes(its, CACHE_THUMB_CLIENT_SECONDS)

      case None =>
        info(s"getThumb($dkey, $imageId): 404 Not found")
        imgNotFound
    }
  }


  /**
   * Раздача оригиналов сохраненных в HBase картинок.
   * @param imageId id картинки.
   * @return Оригинал картинки.
   */
  def getOrig(imageId: String) = Action.async { implicit request =>
    MUserImgOrig.getById(imageId) map {
      case Some(its) =>
        serveImgBytes(its, CACHE_ORIG_CLIENT_SECONDS)

      case None =>
        info(s"getOrig($imageId): 404")
        imgNotFound
    }
  }

  /** Обслуживание картинки. */
  private def serveImgBytes(its: ImgWithTimestamp, cacheSeconds: Int)(implicit request: RequestHeader) = {
    val ts0 = new Instant(its.timestamp) // не lazy, ибо всё равно понадобиться хотя бы в одной из веток.
    val isCached = request.headers.get(IF_MODIFIED_SINCE) flatMap {
        DateTimeUtil.parseRfcDate
      } exists { dt =>
        ts0 isBefore dt
      }
    if (isCached) {
      trace("serveImg(): 304 Not Modified")
      NotModified

    } else {
      trace(s"serveImg(): 200 OK. size = ${its.img.length} bytes")
      Ok(its.img)
        .as("image/jpeg")
        .withHeaders(
          LAST_MODIFIED -> DateTimeUtil.df.print(ts0),
          CACHE_CONTROL -> ("public, max-age=" + cacheSeconds)
        )
    }
  }


  /** Загрузка сырой картинки для дальнейшей базовой обработки (кадрирования).
    * Картинка загружается в tmp-хранилище, чтобы её можно было оттуда оперативно удалить и иметь реалтаймовый доступ к ней. */
  def handleTempImg = IsAuth(parse.multipartFormData) { implicit request =>
    request.body.file("picture") match {
      case Some(pictureFile) =>
        val fileRef = pictureFile.ref
        val srcFile = fileRef.file
        val mptmp = MPictureTmp.getForTempFile(fileRef)
        try {
          OrigImageUtil.convert(srcFile, mptmp.file)
          val reply = JsObject(List(
            "status"     -> JsString("ok"),
            "image_key"  -> JsString(mptmp.key),
            "image_link" -> JsString(routes.Img.getTempImg(mptmp.key).url)
          ))
          Ok(reply)

        } catch {
          case ex: Throwable =>
            debug(s"ImageMagick crashed on file $srcFile ; orig: ${pictureFile.filename} :: ${pictureFile.contentType} [${srcFile.length} bytes]", ex)
            val reply = jsonImgError("Unsupported picture format.")
            BadRequest(reply)

        } finally {
          srcFile.delete()
        }

      case None =>
        val reply = jsonImgError("Picture not found in request.")
        NotAcceptable(reply)
    }
  }

  /** Раздавалка картинок, созданных в [[handleTempImg]]. */
  def getTempImg(key: String) = IsAuth { implicit request =>
    // Надо бы добавить сюда поддержку if-modifier-since...
    MPictureTmp.find(key) match {
      case Some(mptmp) =>
        val f = mptmp.file
        Ok.sendFile(f, inline=true)
          .withHeaders(
            LAST_MODIFIED -> DateTimeUtil.df.print(f.lastModified),
            CACHE_CONTROL -> ("public, max-age=" + TEMP_IMG_CACHE_SECONDS)
          )

      case None => imgNotFound
    }
  }

  /**
   * Раздача произвольных картинок без проверки прав.
   * @param key ключ картинки
   * @return Один из различных экшенов обработки.
   */
  def getImg(key: String): Action[AnyContent] = {
    val iik = ImgIdKey(key)
    if (iik.isValid) {
      iik match {
        case tiik: TmpImgIdKey  => getTempImg(key)
        case oiik: OrigImgIdKey => getOrig(key)
      }
    } else {
      trace(s"invalid img id: " + iik)
      actionImgNotFound
    }
  }

  private def actionImgNotFound = Action { imgNotFound }
  private def imgNotFound = NotFound("No such image")

  /** Выдать json ошибку по поводу картинки. */
  private def jsonImgError(msg: String) = JsObject(Seq(
    "status" -> JsString("error"),
    "msg"    -> JsString(msg) // TODO Добавить бы поддержку lang.
  ))

}
