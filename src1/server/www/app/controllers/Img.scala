package controllers

import java.time.{Instant, ZonedDateTime}

import io.suggest.dt.DateTimeUtil
import io.suggest.file.MimeUtilJvm
import io.suggest.model.n2.media.storage.IMediaStorages
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.{Inject, Singleton}
import models.im._
import models.mproj.ICommonDi
import play.api.http.HttpEntity
import play.api.mvc._
import util.acl.CanDynImg
import util.img.DynImgUtil

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
                      mLocalImgs                      : MLocalImgs,
                      dynImgUtil                      : DynImgUtil,
                      canDynImg                       : CanDynImg,
                      iMediaStorages                  : IMediaStorages,
                      override val mCommonDi          : ICommonDi
                    )
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi._

  def isNotModifiedSinceCached(modelTstampMs: Instant, ifModifiedSince: String): Boolean = {
    DateTimeUtil.parseRfcDate(ifModifiedSince)
      .exists { dt => !modelTstampMs.isAfter(dt.toInstant) }
  }

  /** rfc date не содержит миллисекунд. Нужно округлять таймштамп, чтобы был 000 в конце. */
  def withoutMs(timestampMs: Long): Instant = {
    Instant.ofEpochSecond(timestampMs / 1000)
  }


  /** Сколько времени можно кешировать на клиенте результат dynImg() ? */
  private val CACHE_DYN_IMG_CLIENT_SECONDS = {
    val cacheDuration = if (isProd) {
      365.days
    } else {
      30.seconds
    }
    cacheDuration.toSeconds
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

