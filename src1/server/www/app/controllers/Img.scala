package controllers

import java.time.ZonedDateTime

import _root_.util._
import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}

import io.suggest.async.{AsyncUtil, IAsyncUtilDi}
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.file.MimeUtilJvm
import io.suggest.img.MImgFmts
import io.suggest.img.crop.CropConstants
import io.suggest.model.n2.media.storage.IMediaStorages
import io.suggest.popup.PopupConstants
import io.suggest.svg.SvgUtil
import io.suggest.util.logs.{IMacroLogs, MacroLogsImpl}
import models.im._
import models.mctx.Context
import models.mproj.ICommonDi
import models.req.IReq
import org.apache.commons.io.FileUtils
import play.api.data.Forms._
import play.api.data._
import play.api.http.HttpEntity
import play.api.libs.Files.TemporaryFile
import play.api.mvc._
import play.twirl.api.Html
import util.acl._
import util.di.IMImg3Di
import util.img.detect.main.{ColorDetectWsUtil, IColorDetectWsUtilDi, MainColorDetector}
import util.img.{ImgCtlUtil, _}
import util.up.FileUtil
import views.html.img._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.04.13 14:45fo
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
  canDynImg                       : CanDynImg,
  override val origImageUtil      : OrigImageUtil,
  override val asyncUtil          : AsyncUtil,
  iMediaStorages                  : IMediaStorages,
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

  /** Сколько времени можно кешировать на клиенте результат dynImg() ? */
  private val CACHE_DYN_IMG_CLIENT_SECONDS = {
    val cacheDuration = if (isProd) {
      365.days
    } else {
      30.seconds
    }
    cacheDuration.toSeconds
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
      Ok(cropTpl(iik.dynImgId.fileName, width, height = height, imeta, id, iik.dynImgId.cropOpt))
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
              val mimgOrig = MImg3(localImg.dynImgId.original)
              val croppedImgFileName = {
                val imOps = List(cropOp)
                val mimg = mimgOrig.withDynOps(imOps)
                mimg.dynImgId.fileName
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
            NotFound("img does not exist: " + iik0.dynImgId.fileName)
        }
      }
    )
  }


  /** Экшен раздачи динамических картинок второго поколения.
    * Динамические картинки теперь привязаны к хостам, на которых они размещены.
    * И иначе работать теперь нельзя.
    *
    * @param mimg Описание запрашиваемой картинки.
    * @return 200 OK с картинкой.
    *         404, если нет картинки или она обслуживается не на этом хосте.
    */
  def dynImg(mimg: MImgT) = canDynImg(mimg).async { implicit request =>
    lazy val logPrefix = s"dynImg(${mimg.dynImgId.fileName})#${System.currentTimeMillis()}:"

    // Сначала обработать 304-кэширование, если есть что-то:
    val isNotModified = request.mmediaOpt.exists { mmedia =>
      request.headers.get(IF_MODIFIED_SINCE).exists { ifModifiedSince =>
        val dateCreated = mmedia.file.dateCreated
        val newModelInstant = withoutMs(dateCreated.toInstant.toEpochMilli)
        val r = isNotModifiedSinceCached(newModelInstant, ifModifiedSince)
        LOGGER.trace(s"$logPrefix isNotModified?$r dateCreated=$dateCreated")
        r
      }
    }

    // HTTP-заголовок для картинок.
    val cacheControlHdr =
      CACHE_CONTROL -> s"public, max-age=$CACHE_DYN_IMG_CLIENT_SECONDS, immutable"

    if (isNotModified) {
      // 304 Not modified, т.к. клиент уже скачивал эту картинку ранее.
      NotModified
        .withHeaders(cacheControlHdr)

    } else {

      // TODO Надо имя файла записать. Его нужно кодировать, а там какое-то play private api...
      //CONTENT_DISPOSITION -> s"inline; filename=$fileName"

      // Надо всё-таки вернуть картинку. Возможно, картинка ещё не создана. Уточняем:
      request.mmediaOpt.fold [Future[Result]] {
        // Готовой картинки сейчас не существует. Возможно, что она создаётся прямо сейчас.
        // TODO Использовать асинхронную streamed-готовилку dyn-картинок (которую надо написать!).
        for {
          localImg <- dynImgUtil.ensureLocalImgReady(mimg, cacheResult = false)
        } yield {
          val imgFile = mLocalImgs.fileOf(localImg)
          LOGGER.trace(s"$logPrefix 200 OK, file size = ${imgFile.length} bytes")

          Ok.sendFile(
            content   = imgFile,
            inline    = true,
            fileName  = { _ => mimg.dynImgId.fileName }
          )
            .as {
              Try( MimeUtilJvm.probeContentType(imgFile.toPath) )
                .toOption
                .flatten
                .get
            }
            .withHeaders(
              cacheControlHdr
            )
            .withDateHeaders(
              LAST_MODIFIED -> ZonedDateTime.now()
            )
        }

      } { mmedia =>
        // В базе уже есть готовая к раздаче картинка. Организуем akka-stream: swfs -> here -> client.
        // Кэшировать это скорее всего небезопасно, да и выигрыша мало (с локалхоста на локалхост), поэтому без кэша.

        // Пробрсывать в swfs "Accept-Encoding: gzip", если задан в запросе на клиенте.
        // Всякие SVG сжимаются на стороне swfs, их надо раздавать сжатыми.
        iMediaStorages
          .read( mmedia.storage, request.acceptCompressEncodings )
          .map { dataSource =>
            // Всё ок, направить шланг ответа в сторону юзера, пробросив корректный content-encoding, если есть.
            LOGGER.trace(s"$logPrefix Successfully opened data stream with len=${dataSource.sizeB}b origSz=${mmedia.file.sizeB}b")
            var respHeadersAcc = List( cacheControlHdr )

            // Пробросить content-encoding, который вернул media-storage.
            for (compression <- dataSource.compression) {
              respHeadersAcc ::= (
                CONTENT_ENCODING -> compression.httpContentEncoding
              )
            }

            Ok.sendEntity(
              HttpEntity.Streamed(
                data          = dataSource.data,
                contentLength = Some(dataSource.sizeB),
                contentType   = Some(mmedia.file.mime)
              )
            )
              .withHeaders( respHeadersAcc: _* )
              .withDateHeaders(
                // Раньше был ручной рендер даты, но в play появилась поддержка иного форматирования даты, пытаемся жить с ней:
                //LAST_MODIFIED -> DateTimeUtil.rfcDtFmt.format( mmedia.file.dateCreated.atZoneSimilarLocal(ZoneOffset.UTC) ),
                LAST_MODIFIED -> mmedia.file.dateCreated.toZonedDateTime
              )
          }
      }
        .recover { case ex: Throwable =>
          // TODO Пересобрать неисправную картинку, если не-оригинал?
          ex match {
            case _: NoSuchElementException =>
              LOGGER.error(s"$logPrefix Image not exist in ${request.storageInfo}")
              NotFound("Image unexpectedly missing.")
                .withHeaders(CACHE_CONTROL -> s"public, max-age=30")
            case _ =>
              LOGGER.error(s"$logPrefix Failed to read image from ${request.storageInfo}", ex)
              ServiceUnavailable("Internal error occured during fetching/creating an image.")
                .withHeaders(RETRY_AFTER -> "60")
          }
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
        val srcMime = MimeUtilJvm.probeContentType( fileRef.path ).get
        val imgFmt = MImgFmts.withMime(srcMime).get

        // Отрабатываем опциональный рендеринг html-поля с оверлеем.
        val mptmp = MLocalImg( MDynImgId.randomOrig(imgFmt) )
        lazy val ovlOpt = for (hrrr <- ovlRrr) yield {
          hrrr(mptmp.dynImgId.fileName, implicitly[Context])
        }
        val srcFile = fileRef.path.toFile
        // Далее, загрузка для svg и растровой графики расветвляется...
        val tmpFile = mLocalImgs.fileOf(mptmp)
        if (SvgUtil.maybeSvgMime(srcMime)) {
          // Это svg?
          if (SvgUtil.safeOpenWrap( SvgUtil.open(srcFile) ).nonEmpty) {
            // Это svg. Надо его сжать и переместить в tmp-хранилище.
            val newSvg = htmlCompressUtil.compressSvgFromFile(srcFile)
            FileUtils.writeStringToFile(tmpFile, newSvg)
            Ok( jsonTempOk(mptmp.dynImgId.fileName, routes.Img.dynImg(mptmp.toWrappedImg), ovlOpt) )
          } else {
            val reply = jsonImgError("SVG format invalid or not supported.")
            NotAcceptable(reply)
          }

        } else {
          // Это растровая картинка (jpeg, png, etc).
          try {
            val imgPrepareFut: Future[_] = {
              // Проверяем формат принятой картинки на совместимость: // TODO Это не нужно, оригинал можно ведь и не раздавать никогда.
              if (preserveUnknownFmt || MImgFmts.withMime(srcMime).isDefined) {
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
                mptmp.dynImgId.fileName,
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
                LOGGER.error(s"Calling MainColorDetector makes no sense, because websocket is disabled. Img was " + im.dynImgId.fileName)
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

