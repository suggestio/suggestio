package controllers

import java.io.File
import java.time.{Instant, ZoneOffset}

import _root_.util._
import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}

import io.suggest.async.{AsyncUtil, IAsyncUtilDi}
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.dt.DateTimeUtil
import io.suggest.img.crop.CropConstants
import io.suggest.popup.PopupConstants
import io.suggest.svg.SvgUtil
import io.suggest.util.logs.{IMacroLogs, MacroLogsImpl}
import models.im._
import models.mctx.Context
import models.mproj.ICommonDi
import models.req.IReq
import net.sf.jmimemagic.Magic
import org.apache.commons.io.FileUtils
import play.api.data.Forms._
import play.api.data._
import play.api.libs.Files.TemporaryFile
import play.api.mvc._
import play.twirl.api.Html
import util.acl._
import util.di.{IDynImgUtil, IMImg3Di}
import util.img.detect.main.{ColorDetectWsUtil, IColorDetectWsUtilDi, MainColorDetector}
import util.img.{ImgCtlUtil, _}
import util.up.FileUtil
import views.html.img._

import scala.concurrent.Future
import scala.concurrent.duration._

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
  override val colorDetectWsUtil  : ColorDetectWsUtil,
  override val mImgs3             : MImgs3,
  override val mLocalImgs         : MLocalImgs,
  override val dynImgUtil         : DynImgUtil,
  override val imgCtlUtil         : ImgCtlUtil,
  override val origImageUtil      : OrigImageUtil,
  override val asyncUtil          : AsyncUtil,
  fileUtil                        : FileUtil,
  isAuth                          : IsAuth,
  imgFormUtil                     : ImgFormUtil,
  override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with MacroLogsImpl
  with TempImgSupport
{

  import imgCtlUtil._
  import mCommonDi._

  /** Сколько времени можно кешировать на клиенте оригинал картинки. */
  private val CACHE_ORIG_CLIENT_SECONDS = {
    // TODO Кажется, этот параметр изменил свой смысл...
    val cacheDuration =if (isProd) {
      2.days
    } else {
      30.seconds
    }
    cacheDuration.toSeconds.toInt
  }


  // TODO Объеденить все эти serveImgFromFile, задействовать MLocalImg.mime для определения MIME.

  private def serveImgFromFile(file: File, cacheSeconds: Int, modelInstant: Instant): Result = {
    // Enumerator.fromFile() вроде как асинхронный, поэтому запускаем его тут как можно раньше.
    val resultRaw = Ok.sendFile(file, inline = true)
    LOGGER.trace(s"serveImgFromFile(${file.getParentFile.getName}/${file.getName}): 200 OK, file size = ${file.length} bytes.")
    val mmOpt = fileUtil.getMimeMatch(file)

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
        // Если форматтить просто modelInstant, то будет экзепшен: java.time.temporal.UnsupportedTemporalTypeException: Unsupported field: DayOfMonth
        // Это всплывает наружу излишне динамически-типизированная сущность такого API.
        LAST_MODIFIED -> DateTimeUtil.rfcDtFmt.format( modelInstant.atOffset(ZoneOffset.UTC) ),
        CACHE_CONTROL -> ("public, max-age=" + cacheSeconds + ", immutable, never-revalidate")
      )
  }

  /** Отрендерить оконный интерфейс для кадрирования картинки. */
  def imgCropForm(imgId: String, width: Int, height: Int) = isAuth().async { implicit request =>
    val iik = MImg3(imgId).original
    for {
      imetaOpt <- mImgs3.getImageWH(iik)
    } yield {
      val imeta: ISize2di = imetaOpt getOrElse {
        val stub = MSize2di(640, 480)
        LOGGER.warn("Failed to fetch image w/h metadata for iik " + iik + " . Returning stub metadata: " + stub)
        stub
      }
      val id = CropConstants.CROPPER_DIV_ID
      Ok(cropTpl(iik.fileName, width, height = height, imeta, id, iik.cropOpt))
        .withHeaders(PopupConstants.HTTP_HDR_POPUP_ID -> id)
    }
  }


  /** Маппинг данных кропа картинки. */
  private def imgCropFormM = Form(tuple(
    "imgId"  -> imgFormUtil.img3IdM,
    "crop"   -> imgFormUtil.imgCropM,
    "target" -> FormUtil.whSizeM
  ))

  /** Сабмит запроса на кадрирование картинки. В сабмите данные по исходной картинке и данные по кропу. */
  def imgCropSubmit = isAuth().async { implicit request =>
    imgCropFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug("imgCropSubmit(): Failed to bind form: " + formatFormErrors(formWithErrors))
        NotAcceptable("crop request parse failed")
      },
      {case (iik0, icrop, targetSz) =>
        // Запрашиваем исходную картинку:
        val preparedTmpImgFut = mImgs3.toLocalImg(iik0)

        // 2014.oct.08 Нужно чинить кроп, т.к. форма может засабмиттить его с ошибками.
        val crop2Fut = for (whOpt <- mImgs3.getImageWH(iik0)) yield {
          whOpt.fold(icrop) { wh =>
            imgFormUtil.repairCrop(icrop, targetSz = targetSz, srcSz = wh)
          }
        }

        preparedTmpImgFut flatMap {
          case Some(localImg) =>
            crop2Fut map { crop2 =>
              // Сгенерить id картинки. Собираем картинку на базе исходника, накатив только crop:
              val cropOp = AbsCropOp(crop2)
              val mimgOrig = MImg3(localImg.rowKeyStr)
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
      request.headers
        .get(IF_MODIFIED_SINCE)
        .fold( Future.successful(false) ) { ims =>
          for (imetaOpt <- mImgs3.rawImgMeta(args)) yield {
            imetaOpt.fold(false) { imeta =>
              val newModelInstant = withoutMs(imeta.dateCreated.toInstant.toEpochMilli)
              isModifiedSinceCached(newModelInstant, ims)
            }
          }
        }
    }

    notModifiedFut.flatMap {
      case true =>
        NotModified
          .withHeaders(CACHE_CONTROL -> s"public, max-age=$CACHE_ORIG_CLIENT_SECONDS, immutable")

      // Изменилась картинка. Выдать её. Если картинки нет, то создать надо на основе оригинала.
      case false =>
        val ensureFut = for {
          localImg <- dynImgUtil.ensureImgReady(args, cacheResult = false)
        } yield {
          val imgFile = mLocalImgs.fileOf(localImg)
          serveImgFromFile(
            file          = imgFile,
            cacheSeconds  = CACHE_ORIG_CLIENT_SECONDS,
            modelInstant  = Instant.ofEpochMilli( imgFile.lastModified() )
          )
        }

        ensureFut.recover {
          case _: NoSuchElementException =>
            LOGGER.debug("Img not found anywhere: " + args.fileName)
            NotFound("No such image.")
              .withHeaders(CACHE_CONTROL -> s"public, max-age=30")
          case ex: Throwable =>
            LOGGER.error(s"Unknown exception occured during fetchg/processing of source image id[${args.rowKey}]\n  args = $args", ex)
            ServiceUnavailable("Internal error occured during fetching/creating an image.")
              .withHeaders(RETRY_AFTER -> "60")
        }
    }
  }

}



/** Функционал для поддержки работы с логотипами. Он является общим для ad, shop и mart-контроллеров. */
@ImplementedBy( classOf[Img] )
trait TempImgSupport
  extends SioController
  with IMacroLogs
  with IColorDetectWsUtilDi
  with IDynImgUtil
  with IMImg3Di
  with IOrigImageUtilDi
  with IMLocalImgs
  with IAsyncUtilDi
{

  import mCommonDi._

  def mainColorDetector: MainColorDetector

  /** DI-инстанс [[ImgCtlUtil]], т.е. статическая утиль для img-контроллеров. */
  val imgCtlUtil: ImgCtlUtil

  import imgCtlUtil._

  private def TEMP_IMG_PREVIEW_SIDE_SIZE_PX = 620

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
                     mImgCompanion: IMImgCompanion = MImg3)
                    (implicit request: IReq[MultipartFormData[TemporaryFile]]): Future[Result] = {
    // TODO Надо часть синхронной логики загнать в Future{}. Это нужно, чтобы скачанные данные из tmp удалялись автоматом.
    val resultFut: Future[Result] = request.body.file("picture") match {
      case Some(pictureFile) =>
        val fileRef = pictureFile.ref
        val srcFile = fileRef.path.toFile
        val srcMagicMatch = Magic.getMagicMatch(srcFile, false)
        val srcMime = srcMagicMatch.getMimeType

        // Отрабатываем опциональный рендеринг html-поля с оверлеем.
        val mptmp = MLocalImg()
        lazy val ovlOpt = for (hrrr <- ovlRrr) yield {
          hrrr(mptmp.fileName, implicitly[Context])
        }
        // Далее, загрузка для svg и растровой графики расветвляется...
        val tmpFile = mLocalImgs.fileOf(mptmp)
        if (SvgUtil.maybeSvgMime(srcMime)) {
          // Это svg?
          if (SvgUtil.isSvgFileValid(srcFile)) {
            // Это svg. Надо его сжать и переместить в tmp-хранилище.
            val newSvg = htmlCompressUtil.compressSvgFromFile(srcFile)
            FileUtils.writeStringToFile(tmpFile, newSvg)
            Ok( jsonTempOk(mptmp.fileName, routes.Img.dynImg(mptmp.toWrappedImg), ovlOpt) )
          } else {
            val reply = jsonImgError("SVG format invalid or not supported.")
            NotAcceptable(reply)
          }

        } else {
          // Это растровая картинка (jpeg, png, etc).
          try {
            val imgPrepareFut: Future[_] = {
              // Проверяем формат принятой картинки на совместимость: // TODO Это не нужно, оригинал можно ведь и не раздавать никогда.
              if (preserveUnknownFmt || OutImgFmts.forImageMime(srcMime).isDefined) {
                // TODO Вызывать jpegtran или другие вещи для lossless-обработки. В фоне, параллельно.
                Future {
                  FileUtils.moveFile(srcFile, tmpFile)
                }(asyncUtil.singleThreadIoContext)
              } else {
                // Конвертим в JPEG всякие левые форматы.
                Future {
                  // Использовать что-то более гибкое и полезное. Вдруг зальют негатив .arw какой-нить в hi-res.
                  origImageUtil.convert(srcFile, tmpFile, strip = true)
                }(asyncUtil.singleThreadCpuContext)
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
                  colorDetectWsUtil.detectPalletteToWs(im, wsId.get)
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
        f.ref.path.toFile.delete()
      }
    }

    resultFut
  }


  /** IM Resize-операция по генерации превьюшки для картинки в редакторе. */
  def _imgRszPreviewOp = {
    AbsResizeOp(
      MSize2di(TEMP_IMG_PREVIEW_SIDE_SIZE_PX, TEMP_IMG_PREVIEW_SIDE_SIZE_PX),
      ImResizeFlags.OnlyShrinkLarger
    )
  }

}

