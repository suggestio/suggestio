package controllers

import java.io.File

import io.suggest.model.ImgWithTimestamp
import models.im.{ImResizeFlags, AbsResizeOp, MLocalImg, MImg}
import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.mvc._
import _root_.util._
import play.api.libs.concurrent.Execution.Implicits._
import org.joda.time.{ReadableInstant, DateTime, Instant}
import play.api.Play, Play.{current, configuration}
import util.acl._
import util.img._
import play.api.libs.json._
import scala.concurrent.duration._
import models._
import net.sf.jmimemagic.{MagicMatch, Magic}
import scala.concurrent.Future
import views.html.img._
import play.api.data._, Forms._
import io.suggest.img.ConvertModes
import io.suggest.ym.model.common.MImgInfoMeta

import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.04.13 14:45
 * Description: Управление картинками, относящихся к поисковой выдаче и к разным другим вещам.
 * Изначально контроллер служил только для превьюшек картинок, и назывался "Thumb".
 */

object Img extends SioController with PlayMacroLogsImpl with TempImgSupport with BruteForceProtect {

  import LOGGER._

  /** Время кеширования thumb'а на клиенте. */
  val CACHE_THUMB_CLIENT_SECONDS = configuration.getInt("img.thumb.cache.client.seconds") getOrElse 36000

  /** Сколько времени кешировать temp-картинки на клиенте. */
  val TEMP_IMG_CACHE_SECONDS = {
    val cacheDuration = configuration.getInt("img.temp.cache.client.minutes").map(_ minutes) getOrElse {
      if (Play.isProd) {
        10 minutes
      } else {
        30 seconds
      }
    }
    cacheDuration.toSeconds.toInt
  }

  /** Сколько времени можно кешировать на клиенте оригинал картинки. */
  val CACHE_ORIG_CLIENT_SECONDS = {
    val cacheDuration = configuration.getInt("img.orig.cache.client.hours").map(_.hours) getOrElse {
      if (Play.isProd) {
        2 days
      } else {
        30 seconds
      }
    }
    cacheDuration.toSeconds.toInt
  }


  /**
   * Выдать картинку из HDFS. Используется для визуализации выдачи.
   * Валидность параметров проверяется в роутере регэкспами.
   * @param imageId Хеш-ключ картинки в хранилище домена.
   * @return 200 Ok и картинка.
   *         404 Not Found.
   */
  def getThumb(imageId: String) = Action.async { implicit request =>
    suppressQsFlood(routes.Img.getThumb(imageId)) {
      // Thumb'ы у нас [пока] только не кропаные, поэтому надо срезать crop-суффикс из imageId.
      ImgIdKey(imageId) match {
        case oiik: OrigImgIdKey =>
          MImgThumb2.getThumbByStrId(oiik.data.rowKey) map {
            case Some(its) =>
              serveImgMaybeCached(its, CACHE_THUMB_CLIENT_SECONDS)

            case None =>
              info(s"getThumb($imageId): 404 Not found")
              imgNotFound
          }

        // Если запрос идёт за itmp, то надо вернуть соответсвующий itmp, т.к. thumb'ов для них не предусмотрено.
        case tiik: TmpImgIdKey =>
          _getTempImg(tiik.filename)
      }
    }
  }


  /**
   * Раздача оригиналов сохраненных в HBase картинок.
   * @param filename id картинки.
   * @return Оригинал картинки.
   */
  def getOrig(filename: String) = {
    val oiik = OrigImgIdKey(filename)
    getOrigIik(oiik)
  }

  def getOrigIik(oiik: OrigImgIdKey) = Action.async { implicit request =>
    import oiik.filename
    suppressQsFlood(routes.Img.getOrig(filename)) {
      MUserImg2.getByStrId(oiik.data.rowKey, q = oiik.origQualifierOpt) map {
        case Some(its) =>
          serveImgMaybeCached(its, CACHE_ORIG_CLIENT_SECONDS)

        case None =>
          info(s"getOrig($filename): 404")
          imgNotFound
      }
    }
  }

  /** rfc date не содержит миллисекунд. Нужно округлять таймштамп, чтобы был 000 в конце. */
  private def withoutMs(timestampMs: Long): Instant = {
    val ims = timestampMs % 1000L
    new Instant(timestampMs - ims)
  }


  /**
   * Проверить значение If-Modified-Since в реквесте.
   * true - not modified, false иначе.
   */
  private def isModifiedSinceCached(modelTstampMs: ReadableInstant)(implicit request: RequestHeader): Boolean = {
    request.headers.get(IF_MODIFIED_SINCE)
      .fold(false)(isModifiedSinceCached(modelTstampMs, _))
  }
  private def isModifiedSinceCached(modelTstampMs: ReadableInstant, ims: String): Boolean = {
    DateTimeUtil.parseRfcDate(ims)
      .exists { dt => !(modelTstampMs isAfter dt) }
  }

  /** Обслуживание картинки. */
  private def serveImgMaybeCached(its: ImgWithTimestamp, cacheSeconds: Int)(implicit request: RequestHeader): Result = {
    // rfc date не содержит миллисекунд. Нужно округлять таймштамп, чтобы был 000 в конце.
    val modelInstant = withoutMs(its.timestampMs)
    val isCached = isModifiedSinceCached(modelInstant)
    if (isCached) {
      //trace("serveImg(): 304 Not Modified")
      NotModified
        .withHeaders(CACHE_CONTROL -> s"public, max-age=$cacheSeconds")
    } else {
      serveImgBytes(its.imgBytes, cacheSeconds, modelInstant)
    }
  }

  private def serveImgBytes(imgBytes: Array[Byte], cacheSeconds: Int, modelInstant: ReadableInstant): Result = {
    trace(s"serveImg(): 200 OK. size = ${imgBytes.length} bytes")
    serveImg(
      resultRaw = Ok(imgBytes),
      mm        = Magic.getMagicMatch(imgBytes),
      cacheSeconds = cacheSeconds,
      modelInstant = modelInstant
    )
  }

  private def serveImgFromFile(file: File, cacheSeconds: Int, modelInstant: ReadableInstant): Result = {
    // Enumerator.fromFile() вроде как асинхронный, поэтому запускаем его тут как можно раньше.
    val iteeResult = Ok.sendFile(file)
    trace(s"serveImgFromFile(${file.getParentFile.getName}/${file.getName}): 200 OK, file size = ${file.length} bytes.")
    serveImg(
      resultRaw     = iteeResult,
      mm            = Magic.getMagicMatch(file, false, true),
      cacheSeconds  = cacheSeconds,
      modelInstant  = modelInstant
    )
  }

  private def serveImg(resultRaw: Result, mm: MagicMatch, cacheSeconds: Int, modelInstant: ReadableInstant): Result = {
    val ct = Option(mm)
      .flatMap { mm => Option(mm.getMimeType) }
      // 2014.sep.26: В случае svg, jmimemagic не определяет правильно content-type, поэтому нужно ему помочь:
      .map {
        case textCt if SvgUtil.maybeSvgMime(textCt) => "image/svg+xml"
        case other => other
      }
      .getOrElse("image/unknown")   // Should never happen
    resultRaw
      .withHeaders(
        CONTENT_TYPE  -> ct,
        LAST_MODIFIED -> DateTimeUtil.rfcDtFmt.print(modelInstant),
        CACHE_CONTROL -> ("public, max-age=" + cacheSeconds)
      )
  }

  /** Загрузка сырой картинки для дальнейшей базовой обработки (кадрирования).
    * Картинка загружается в tmp-хранилище, чтобы её можно было оттуда оперативно удалить и иметь реалтаймовый доступ к ней. */
  def handleTempImg = IsAuth.async(parse.multipartFormData) { implicit request =>
    bruteForceProtected {
      _handleTempImg()
    }
  }

  /** Раздавалка картинок, созданных в [[handleTempImg]]. */
  // TODO Тут надо бы IsAuth, но он мешает работать программистам из-за использования audience_url в ряде случаев.
  def getTempImg(filename: String) = Action.async { implicit request =>
    suppressQsFlood(routes.Img.getTempImg(filename)) {
      _getTempImg(filename)
    }
  }

  private def _getTempImg(filename: String): Result = {
    // Надо бы добавить сюда поддержку if-modifier-since...
    MPictureTmp.find(filename) match {
      case Some(mptmp) =>
        val f = mptmp.file
        Ok.sendFile(f, inline = true)
          .withHeaders(
            LAST_MODIFIED -> DateTimeUtil.rfcDtFmt.print(f.lastModified),
            CACHE_CONTROL -> ("public, max-age=" + TEMP_IMG_CACHE_SECONDS)
          )

      case None => imgNotFound
    }
  }

  /**
   * Раздача произвольных картинок без проверки прав.
   * @param imgId ключ картинки
   * @return Один из различных экшенов обработки.
   */
  def getImg(imgId: String): Action[AnyContent] = {
    val iik = ImgIdKey(imgId)
    _getImg(iik)
  }

  def _getImg(iik: ImgIdKey) = {
    if (iik.isValid) {
      iik match {
        case tiik: TmpImgIdKey  => getTempImg(iik.filename)
        case oiik: OrigImgIdKey => getOrigIik(oiik)
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
  private[controllers] def jsonTempOk(filename: String, imgUrl: Call) = {
    JsObject(List(
      "status"     -> JsString("ok"),
      "image_key"  -> JsString(filename),
      "image_link" -> JsString(imgUrl.url)
    ))
  }


  /** Отрендерить оконный интерфейс для кадрирования картинки. */
  def imgCropForm(imgId: String, width: Int, height: Int, markerOpt: Option[String]) = {
    IsAuth.async { implicit request =>
      val iik0 = ImgIdKey(imgId)
      val iik = iik0.uncropped
      iik.getBaseImageWH map { imetaOpt =>
        val imeta: MImgInfoMeta = imetaOpt getOrElse {
          val stub = MImgInfoMeta(640, 480)
          warn("Failed to fetch image w/h metadata for iik " + iik + " . Returning stub metadata: " + stub)
          stub
        }
        Ok(cropTpl(iik.filename, width, height, markerOpt, imeta, iik0.cropOpt))
      }
    }
  }


  /** Маппинг данных кропа картинки. */
  private def imgCropFormM = Form(tuple(
    "imgId"  -> ImgFormUtil.imgIdM,
    "crop"   -> ImgFormUtil.imgCropM,
    "marker" -> optional(text(maxLength = 32)),
    "target" -> FormUtil.whSizeM
  ))

  /** Сабмит запроса на кадрирование картинки. В сабмите данные по исходной картинке и данные по кропу. */
  def imgCropSubmit = IsAuth.async { implicit request =>
    imgCropFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("imgCropSubmit(): Failed to bind form: " + formatFormErrors(formWithErrors))
        NotAcceptable("crop request parse failed")
      },
      {case (iik0, icrop, markerOpt, targetSz) =>
        // Запрашиваем исходную картинку:
        val preparedTmpImgFut = iik0.toTempPictOrig
        // 2014.oct.08 Нужно чинить кроп, т.к. форма может засабмиттить его с ошибками.
        val crop2Fut = iik0.getBaseImageWH.map { whOpt =>
          whOpt.fold(icrop) { wh =>
            ImgFormUtil.repairCrop(icrop, targetSz = targetSz, srcSz = wh)
          }
        }
        for {
          origMptmp <- preparedTmpImgFut
          crop2     <- crop2Fut
        } yield {
          val cropOpt = Some(crop2)
          val croppedMptmpData = origMptmp.data.copy(cropOpt = cropOpt)
          val croppedMptmp = MPictureTmp(data = croppedMptmpData)
          OrigImageUtil.convert(
            fileOld = origMptmp.file,
            fileNew = croppedMptmp.file,
            crop = cropOpt,
            mode = ConvertModes.STRIP
          )
          // TODO Нужно использовать dynImg.
          Ok(jsonTempOk(croppedMptmp.filename, routes.Img.getImg(croppedMptmp.filename)))
        }
      }
    )
  }


  /**
   * Запрос картинки с опрделёнными параметрами.
   * Ссылка на картинку формируется на сервере и имеет HMAC-подпись для защиты от модификации.
   * @param args Данные по желаемой картинке.
   * @return Картинки или 304 Not modified.
   */
  def dynImg(args: MImg) = Action.async { implicit request =>
    // TODO Нужна поддержка tmp img? Пока нет -- тут экзепшены.
    val rowKey = args.rowKey
    val notModifiedFut: Future[Boolean] = {
      request.headers.get(IF_MODIFIED_SINCE) match {
        case Some(ims) =>
          args.rawImgMeta map {
            case Some(imeta) =>
              val newModelInstant = withoutMs(imeta.timestampMs)
              isModifiedSinceCached(newModelInstant, ims)
            case None =>
              false
          }

        case None => Future successful false
      }
    }
    notModifiedFut flatMap {
      case true =>
        NotModified
          .withHeaders(CACHE_CONTROL -> s"public, max-age=$CACHE_ORIG_CLIENT_SECONDS")

      // Изменилась картинка. Выдать её. Если картинки нет, то создать надо на основе оригинала.
      case false =>
        args.toLocalImg flatMap {
          case Some(img) =>
            val modelInstant = withoutMs(img.file.lastModified)
            serveImgFromFile(img.file, CACHE_ORIG_CLIENT_SECONDS, modelInstant)

          // Картинки в указанном виде нету. Нужно сделать из оригинала.
          case None =>
            DynImgUtil.mkReadyImgToFile(args)
              .map { localImg2 =>
                val msRaw = DateTime.now.getMillis
                val newModelInstant = withoutMs(msRaw)
                val saveDt = newModelInstant.toDateTime
                // В фоне запускаем сохранение полученной картинки в базу.
                localImg2.toWrappedImg.saveToPermanent onComplete {
                  case Success(false) => warn(s"Image not saved into permanent image storage: $args / file = ${localImg2.file}")
                  case Failure(ex)    => error(s"Failed to save image $localImg2 into permanent storage", ex)
                  case _              => // do nothing - об успехах сообщать не нужно.
                }
                serveImgFromFile(localImg2.file, cacheSeconds = CACHE_ORIG_CLIENT_SECONDS, modelInstant = saveDt)

              }.recover {
                case ex: NoSuchElementException =>
                  warn(s"Orig unmodified image not found: id[${args.rowKeyStr}]")
                  imgNotFound
                case ex: Throwable =>
                  error(s"Unknown exception occured during fetchg/processing of source image id[$rowKey]\n  args = $args", ex)
                  imgNotFound
              }
        }
    }
  }

}



/** Функционал для поддержки работы с логотипами. Он является общим для ad, shop и mart-контроллеров. */
trait TempImgSupport extends SioController with PlayMacroLogsI {

  val TEMP_IMG_PREVIEW_SIDE_SIZE_PX = configuration.getInt("ctl.img.temp.preview.side.px") getOrElse 620

  /** Обработчик полученной картинки в контексте реквеста, содержащего необходимые данные. Считается, что ACL-проверка уже сделана.
    * @param preserveUnknownFmt Оставлено на случай поддержки всяких странных форматов.
    * @param request HTTP-реквест.
    * @return Экземпляр Result, хранящий json с данными результата.
    */
  def _handleTempImg(preserveUnknownFmt: Boolean = false)
                    (implicit request: Request[MultipartFormData[TemporaryFile]]): Result = {
    try {
      request.body.file("picture") match {
        case Some(pictureFile) =>
          val fileRef = pictureFile.ref
          val srcFile = fileRef.file
          val srcMagicMatch = Magic.getMagicMatch(srcFile, false)
          // Отрабатываем svg: не надо конвертить.
          val srcMime = srcMagicMatch.getMimeType
          if (SvgUtil maybeSvgMime srcMime) {
            // Это svg?
            if (SvgUtil isSvgFileValid srcFile) {
              // Это svg. Надо его сжать и переместить в tmp-хранилище.
              val newSvg = HtmlCompressUtil.compressSvgFromFile(srcFile)
              val mptmp = MLocalImg()
              FileUtils.writeStringToFile(mptmp.file, newSvg)
              Ok( Img.jsonTempOk(mptmp.filename, routes.Img.getImg(mptmp.filename)) )
            } else {
              val reply = Img.jsonImgError("SVG format invalid or not supported.")
              NotAcceptable(reply)
            }

          } else {
            try {
              val mptmp = MLocalImg()  // MPictureTmp.getForTempFile(fileRef.file, outFmt, marker)
              // TODO Вызывать jpegtran или другие вещи для lossless-обработки.
              // Конвертим в JPEG всякие левые форматы.
              if (preserveUnknownFmt || OutImgFmts.forImageMime(srcMime).isDefined) {
                FileUtils.moveFile(srcFile, mptmp.file)
              } else {
                OrigImageUtil.convert(srcFile, mptmp.file, ConvertModes.STRIP)
              }
              // Генерим уменьшенную превьюшку для отображения на экране.
              val imOps = List(
                AbsResizeOp(MImgInfoMeta(TEMP_IMG_PREVIEW_SIDE_SIZE_PX, TEMP_IMG_PREVIEW_SIDE_SIZE_PX), ImResizeFlags.OnlyShrinkLarger)
              )
              val im = MImg(mptmp.rowKey, imOps)
              Ok( Img.jsonTempOk(mptmp.filename, DynImgUtil.imgCall(im)) )
            } catch {
              case ex: Throwable =>
                LOGGER.debug(s"ImageMagick crashed on file $srcFile ; orig: ${pictureFile.filename} :: ${pictureFile.contentType} [${srcFile.length} bytes]", ex)
                val reply = Img.jsonImgError("Unsupported picture format.")
                NotAcceptable(reply)
            }
          }

        // В реквесте не найдена именованая часть, содержащая картинку.
        case None =>
          val reply = Img.jsonImgError("picture part not found in request.")
          NotAcceptable(reply)
      }

    } finally {
      // Удалить все файлы, которые были приняты в реквесте.
      request.body.files.foreach { f =>
        f.ref.file.delete()
      }
    }
  }

}

