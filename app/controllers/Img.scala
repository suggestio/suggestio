package controllers

import java.io.File

import _root_.util._
import _root_.util.async.AsyncUtil
import com.google.inject.{ImplementedBy, Inject, Singleton}
import io.suggest.common.geom.d2.ISize2di
import io.suggest.img.ConvertModes
import io.suggest.img.crop.CropConstants
import io.suggest.popup.PopupConstants
import io.suggest.svg.SvgUtil
import io.suggest.ym.model.common.MImgInfoMeta
import models.im._
import models.mctx.Context
import models.mfs.FileUtil
import models.mproj.ICommonDi
import models.req.IReq
import net.sf.jmimemagic.Magic
import org.apache.commons.io.FileUtils
import org.joda.time.{DateTime, ReadableInstant}
import play.api.data.Forms._
import play.api.data._
import play.api.libs.Files.TemporaryFile
import play.api.mvc._
import play.twirl.api.Html
import util.acl._
import util.di.{IDynImgUtil, IMImg3Di}
import util.img.detect.main.MainColorDetector
import util.img.{ImgCtlUtil, _}
import util.ws.WsDispatcherActors
import views.html.img._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.04.13 14:45
 * Description: Управление картинками, относящихся к поисковой выдаче и к разным другим вещам.
 * Изначально контроллер служил только для превьюшек картинок, и назывался "Thumb".
 */
@Singleton
class Img @Inject() (
  override val mainColorDetector  : MainColorDetector,
  override val mImgs3             : MImgs3,
  override val dynImgUtil         : DynImgUtil,
  override val imgCtlUtil         : ImgCtlUtil,
  override val wsDispatcherActors : WsDispatcherActors,
  override val mCommonDi          : ICommonDi
)
  extends SioController
  with PlayMacroLogsImpl
  with TempImgSupport
  with BruteForceProtectCtl
  with IsAuth
{

  import LOGGER._
  import imgCtlUtil._
  import mCommonDi._

  /** Сколько времени можно кешировать на клиенте оригинал картинки. */
  val CACHE_ORIG_CLIENT_SECONDS = {
    val cacheDuration = configuration.getInt("img.orig.cache.client.hours").map(_.hours) getOrElse {
      if (isProd) {
        2.days
      } else {
        30.seconds
      }
    }
    cacheDuration.toSeconds.toInt
  }


  // TODO Объеденить все эти serveImgFromFile, задействовать MLocalImg.mime для определения MIME.

  private def serveImgFromFile(file: File, cacheSeconds: Int, modelInstant: ReadableInstant): Result = {
    // Enumerator.fromFile() вроде как асинхронный, поэтому запускаем его тут как можно раньше.
    val resultRaw = Ok.sendFile(file, inline = true)
    trace(s"serveImgFromFile(${file.getParentFile.getName}/${file.getName}): 200 OK, file size = ${file.length} bytes.")
    val mmOpt = FileUtil.getMimeMatch(file)

    val ct = mmOpt
      .flatMap { mm => Option(mm.getMimeType) }
      // 2014.sep.26: В случае svg, jmimemagic не определяет правильно content-type, поэтому нужно ему помочь:
      .map {
        case textCt if SvgUtil.maybeSvgMime(textCt) => "image/svg+xml"
        case other => other
      }
      .getOrElse{
        LOGGER.warn(s"serveImg(): No MIME match found")
        "image/unknown"
      }   // Should never happen
    resultRaw
      .as(ct)
      .withHeaders(
        LAST_MODIFIED -> DateTimeUtil.rfcDtFmt.print(modelInstant),
        CACHE_CONTROL -> ("public, max-age=" + cacheSeconds)
      )
  }

  /** Отрендерить оконный интерфейс для кадрирования картинки. */
  def imgCropForm(imgId: String, width: Int, height: Int) = IsAuth.async { implicit request =>
    val iik = mImgs3(imgId).original
    for {
      imetaOpt <- mImgs3.getImageWH(iik)
    } yield {
      val imeta: ISize2di = imetaOpt getOrElse {
        val stub = MImgInfoMeta(640, 480)
        warn("Failed to fetch image w/h metadata for iik " + iik + " . Returning stub metadata: " + stub)
        stub
      }
      val id = CropConstants.CROPPER_DIV_ID
      Ok(cropTpl(iik.fileName, width, height = height, imeta, id, iik.cropOpt))
        .withHeaders(PopupConstants.HTTP_HDR_POPUP_ID -> id)
    }
  }


  /** Маппинг данных кропа картинки. */
  private def imgCropFormM = Form(tuple(
    "imgId"  -> ImgFormUtil.img3IdM,
    "crop"   -> ImgFormUtil.imgCropM,
    "target" -> FormUtil.whSizeM
  ))

  /** Сабмит запроса на кадрирование картинки. В сабмите данные по исходной картинке и данные по кропу. */
  def imgCropSubmit = IsAuth.async { implicit request =>
    imgCropFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("imgCropSubmit(): Failed to bind form: " + formatFormErrors(formWithErrors))
        NotAcceptable("crop request parse failed")
      },
      {case (iik0, icrop, targetSz) =>
        // Запрашиваем исходную картинку:
        val preparedTmpImgFut = mImgs3.toLocalImg(iik0)

        // 2014.oct.08 Нужно чинить кроп, т.к. форма может засабмиттить его с ошибками.
        val crop2Fut = for (whOpt <- mImgs3.getImageWH(iik0)) yield {
          whOpt.fold(icrop) { wh =>
            ImgFormUtil.repairCrop(icrop, targetSz = targetSz, srcSz = wh)
          }
        }

        preparedTmpImgFut flatMap {
          case Some(localImg) =>
            crop2Fut map { crop2 =>
              // Сгенерить id картинки. Собираем картинку на базе исходника, накатив только crop:
              val cropOp = AbsCropOp(crop2)
              val mimgOrig = mImgs3(localImg.rowKeyStr)
              val croppedImgFileName = {
                val imOps = List(cropOp)
                val mimg = mimgOrig.withDynOps(imOps)
                mimg.fileName
              }
              // Сгенерить новую dyn-ссылку на картинку. Откропать согласно запросу.
              // Т.к. это редактор, имеет смысл отресайзить оригинал до превьюшки.
              val previewCall = {
                val imOps = List(cropOp, _imgRszPreviewOp)
                val mimg = mimgOrig.withDynOps(imOps)
                dynImgUtil.imgCall(mimg)
              }
              Ok( jsonTempOk(croppedImgFileName, previewCall) )
            }

          case None =>
            NotFound("img does not exist: " + iik0.fileName)
        }
      }
    )
  }


  /**
   * Запрос картинки с опрделёнными параметрами.
   * Ссылка на картинку формируется на сервере и имеет HMAC-подпись для защиты от модификации.
   *
   * @param args Данные по желаемой картинке.
   * @return Картинки или 304 Not modified.
   */
  def dynImg(args: MImgT) = Action.async { implicit request =>
    val notModifiedFut: Future[Boolean] = {
      request.headers.get(IF_MODIFIED_SINCE) match {
        case Some(ims) =>
          args.rawImgMeta map {
            case Some(imeta) =>
              val newModelInstant = withoutMs( imeta.dateCreated.getMillis )
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
        dynImgUtil.ensureImgReady(args, cacheResult = false) map { localImg =>
          serveImgFromFile(
            file = localImg.file,
            cacheSeconds = CACHE_ORIG_CLIENT_SECONDS,
            modelInstant = new DateTime(localImg.file.lastModified)
          )
        } recover {
          case ex: NoSuchElementException =>
            debug("Img not found anywhere: " + args.fileName)
            NotFound("No such image.")
          case ex: Throwable =>
            error(s"Unknown exception occured during fetchg/processing of source image id[${args.rowKey}]\n  args = $args", ex)
            InternalServerError("Internal error occured during fetching/creating an image.")
        }
    }
  }

}



/** Функционал для поддержки работы с логотипами. Он является общим для ad, shop и mart-контроллеров. */
@ImplementedBy( classOf[Img] )
trait TempImgSupport
  extends SioController
  with PlayMacroLogsI
  with NotifyWs
  with MyConfName
  with IDynImgUtil
  with IMImg3Di
{

  import mCommonDi._

  def mainColorDetector: MainColorDetector

  /** DI-инстанс [[ImgCtlUtil]], т.е. статическая утиль для img-контроллеров. */
  val imgCtlUtil: ImgCtlUtil

  import imgCtlUtil._

  /** Размер генерируемой палитры. */
  val MAIN_COLORS_PALETTE_SIZE: Int = configuration.getInt(s"img.$MY_CONF_NAME.palette.size") getOrElse 8
  /** Размер возвращаемой по WebSocket палитры. */
  val MAIN_COLORS_PALETTE_SHRINK_SIZE: Int = configuration.getInt(s"img.$MY_CONF_NAME.palette.shrink.size") getOrElse 4

  val TEMP_IMG_PREVIEW_SIDE_SIZE_PX = configuration.getInt(s"img.$MY_CONF_NAME.temp.preview.side.px") getOrElse 620

  /** Настройка кеширования для  */
  protected def CACHE_COLOR_HISTOGRAM_SEC = 10


  /**
   * Запуск в фоне определения палитры и отправки уведомления по веб-сокету.
   *
   * @param im Картинка для обработки.
   * @param wsId id для уведомления.
   */
  def _detectPalletteWs(im: MImgT, wsId: String): Future[Histogram] = {
    // Кеширование ресурсоемких результатов работы MCD.
    val f = { () =>
      mainColorDetector.detectPaletteFor(im, maxColors = MAIN_COLORS_PALETTE_SIZE)
    }
    val cacheSec = CACHE_COLOR_HISTOGRAM_SEC
    val fut = if (cacheSec > 0) {
      cacheApiUtil.getOrElseFut("mcd." + im.rowKeyStr + ".hist", cacheSec.seconds)(f())
    } else {
      f()
    }
    fut.andThen {
      case Success(result) =>
        val res2 = if (MAIN_COLORS_PALETTE_SHRINK_SIZE < MAIN_COLORS_PALETTE_SIZE) {
          result.copy(
            sorted = result.sorted.take(MAIN_COLORS_PALETTE_SHRINK_SIZE)
          )
        } else {
          result
        }
        _notifyWs(wsId, res2)
      case Failure(ex) =>
        LOGGER.warn("Failed to execute color detector on tmp img " + im.fileName, ex)
    }
  }

  /** Обработчик полученной картинки в контексте реквеста, содержащего необходимые данные. Считается, что ACL-проверка уже сделана.
    *
    * @param preserveUnknownFmt Оставлено на случай поддержки всяких странных форматов.
    * @param request HTTP-реквест.
    * @param ovlRrr Overlay HTML renderer. Опциональный.
    * @param runEarlyColorDetector Запускать в фоне детектор палитры основных цветов.
    * @param wsId Для обратной связи с клиентом использовать вебсокет с этим id.
    * @return Экземпляр Result, хранящий json с данными результата.
    */
  def _handleTempImg(preserveUnknownFmt: Boolean = false, runEarlyColorDetector: Boolean = false,
                     wsId: Option[String] = None, ovlRrr: Option[(String, Context) => Html] = None,
                     mImgCompanion: IMImgCompanion = mImgs3)
                    (implicit request: IReq[MultipartFormData[TemporaryFile]]): Future[Result] = {
    // TODO Надо часть синхронной логики загнать в Future{}. Это нужно, чтобы скачанные данные из tmp удалялись автоматом.
    val resultFut: Future[Result] = request.body.file("picture") match {
      case Some(pictureFile) =>
        val fileRef = pictureFile.ref
        val srcFile = fileRef.file
        val srcMagicMatch = Magic.getMagicMatch(srcFile, false)
        // Отрабатываем svg: не надо конвертить.
        val srcMime = srcMagicMatch.getMimeType

        // Отрабатываем опциональный рендеринг html-поля с оверлеем.
        val mptmp = MLocalImg()
        lazy val ovlOpt = ovlRrr.map { hrrr =>
          hrrr(mptmp.fileName, implicitly[Context])
        }
        // Далее, загрузка для svg и растровой графики расветвляется...
        if (SvgUtil.maybeSvgMime(srcMime)) {
          // Это svg?
          if (SvgUtil.isSvgFileValid(srcFile)) {
            // Это svg. Надо его сжать и переместить в tmp-хранилище.
            val newSvg = htmlCompressUtil.compressSvgFromFile(srcFile)
            FileUtils.writeStringToFile(mptmp.file, newSvg)
            Ok( jsonTempOk(mptmp.fileName, routes.Img.dynImg(mptmp.toWrappedImg), ovlOpt) )
          } else {
            val reply = jsonImgError("SVG format invalid or not supported.")
            NotAcceptable(reply)
          }

        } else {
          // Это растровая картинка (jpeg, png, etc).
          try {
            // Конвертим в JPEG всякие левые форматы.
            val imgPrepareFut: Future[_] = {
              if (preserveUnknownFmt || OutImgFmts.forImageMime(srcMime).isDefined) {
                // TODO Вызывать jpegtran или другие вещи для lossless-обработки. В фоне, параллельно.
                Future {
                  FileUtils.moveFile(srcFile, mptmp.file)
                }(AsyncUtil.singleThreadIoContext)
              } else {
                Future {
                  // Использовать что-то более гибкое и полезное. Вдруг зальют негатив .arw какой-нить в hi-res.
                  OrigImageUtil.convert(srcFile, mptmp.file, ConvertModes.STRIP)
                }(AsyncUtil.singleThreadCpuContext)
              }
            }
            // Генерим уменьшенную превьюшку для отображения в форме редактирования чего-то.
            val imOps = List(_imgRszPreviewOp)
            val im = mImgCompanion.fromImg(mptmp, Some(imOps))
            val res2Fut = imgPrepareFut map { _ =>
              Ok( jsonTempOk(
                mptmp.fileName,
                dynImgUtil.imgCall(im),
                ovlOpt
              ) )
            }
            // Запускаем в фоне детектор цвета картинки и отправить клиенту через WebSocket.
            if (runEarlyColorDetector) {
              if (wsId.isDefined) {
                imgPrepareFut flatMap { _ =>
                  _detectPalletteWs(im, wsId.get)
                }
              } else {
                LOGGER.error(s"Calling MainColorDetector makes no sense, because websocket is disabled. Img was " + im.fileName)
              }
            }
            // Возвращаем ожидаемый результат:
            res2Fut
          } catch {
            case ex: Throwable =>
              LOGGER.debug(s"ImageMagick crashed on file $srcFile ; orig: ${pictureFile.filename} :: ${pictureFile.contentType} [${srcFile.length} bytes]", ex)
              val reply = jsonImgError("Unsupported picture format.")
              NotAcceptable(reply)
          }
        }

      // В реквесте не найдена именованая часть, содержащая картинку.
      case None =>
        val reply = jsonImgError("picture part not found in request.")
        NotAcceptable(reply)
    }

    resultFut.onComplete { _ =>
      // Удалить все файлы, которые были приняты в реквесте.
      request.body.files.foreach { f =>
        f.ref.file.delete()
      }
    }

    resultFut
  }


  /** IM Resize-операция по генерации превьюшки для картинки в редакторе. */
  def _imgRszPreviewOp = {
    AbsResizeOp(
      MImgInfoMeta(TEMP_IMG_PREVIEW_SIDE_SIZE_PX, TEMP_IMG_PREVIEW_SIDE_SIZE_PX),
      ImResizeFlags.OnlyShrinkLarger
    )
  }

}

