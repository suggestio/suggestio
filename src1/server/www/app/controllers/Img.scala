package controllers

import io.suggest.file.MimeUtilJvm
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.{Inject, Singleton}
import models.im._
import play.api.inject.Injector
import util.acl.{CanDynImg, IsFileNotModified}
import util.img.DynImgUtil

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.04.13 14:45fo
 * Description: Управление картинками, относящихся к поисковой выдаче и к разным другим вещам.
 * Изначально контроллер служил только для превьюшек картинок, и назывался "Thumb".
 */
@Singleton
final class Img @Inject() (
                            isFileNotModified               : IsFileNotModified,
                            canDynImg                       : CanDynImg,
                            injector                        : Injector,
                            sioCtlApi                       : SioControllerApi,
                          )
  extends MacroLogsImpl
{

  import sioCtlApi._

  private lazy val mLocalImgs = injector.instanceOf[MLocalImgs]
  private lazy val dynImgUtil = injector.instanceOf[DynImgUtil]
  private def uploadCtl = injector.instanceOf[Upload]
  private def errorHandler = injector.instanceOf[ErrorHandler]
  implicit private def ec = injector.instanceOf[ExecutionContext]


  def dynImg(mimg: MImgT) = _dynImg( mimg, returnBody = true )
  def dynImgHead(mimg: MImgT) = _dynImg( mimg, returnBody = false )


  /** Экшен раздачи динамических картинок второго поколения.
    * Динамические картинки теперь привязаны к хостам, на которых они размещены.
    * И иначе работать теперь нельзя.
    *
    * @param mimg Описание запрашиваемой картинки.
    * @return 200 OK с картинкой.
    *         404, если нет картинки или она обслуживается не на этом хосте.
    */
  private def _dynImg(mimg: MImgT, returnBody: Boolean) = canDynImg(mimg)
    .andThen( new isFileNotModified.Refiner )
    .async { ctx304 =>
      import ctx304.request

      lazy val logPrefix = s"dynImg(${mimg.dynImgId.fileName})#${System.currentTimeMillis()}:"
      LOGGER.trace(s"$logPrefix To return answer, devivative?${request.derivativeOpt.nonEmpty} orig?${mimg.dynImgId.isOriginal}")

      // Надо всё-таки вернуть картинку. Возможно, картинка-дериватив ещё не создана. Уточняем:
      (if (request.derivativeOpt.isEmpty && !mimg.dynImgId.isOriginal) {
        // Запрошен дериватив, который ещё пока не существует.
        for {
          localImg <- dynImgUtil.ensureLocalImgReady(mimg, cacheResult = false)
        } yield {
          val imgFile = mLocalImgs.fileOf(localImg)
          LOGGER.trace(s"$logPrefix 200 OK, file size = ${imgFile.length} bytes")

          val status0 = Ok
          (
            if (returnBody) {
              // TODO Нужна поддержка HTTP Range request.
              status0.sendFile(
                content   = imgFile,
                inline    = true,
                fileName  = { _ => Some(mimg.dynImgId.fileName) }
              )
            } else status0
          )
            .as {
              Try( MimeUtilJvm.probeContentType(imgFile.toPath) )
                .toOption
                .flatten
                .get
            }
            .withHeaders(
              isFileNotModified.CACHE_CONTROL_HDR,
              // TODO Нужно etag возвращать. Нужно отрефакторить ensureLocalImgReady и saveToPermanent под актуальную архитекутуру.
            )
            .withDateHeaders(
              // TODO Нужно брать дату сохраненённого узла. А тут - возвращается дата узла-оригинала. Поэтому передача файла-дериватива одному клиенту может произойти дважды: в этот запрос и в последующий запрос (уже с +ETag).
              LAST_MODIFIED -> request.mnode.meta.basic.dateCreated.toZonedDateTime,
            )
        }
      } else {
        // В базе уже есть готовая к раздаче картинка. Организуем akka-stream: swfs -> here -> client.
        // Кэшировать это скорее всего небезопасно, да и выигрыша мало (с локалхоста на локалхост), поэтому без кэша.

        // Пробрасывать в swfs "Accept-Encoding: gzip", если задан в запросе на клиенте.
        // Всякие SVG сжимаются на стороне swfs, их надо раздавать сжатыми.
        uploadCtl.downloadLogic(
          dispInline = true,
          returnBody = returnBody,
        )(ctx304)
      })
        .recoverWith { case ex: Throwable =>
          // TODO Пересобрать неисправную картинку, если не-оригинал?
          ex match {
            case _: NoSuchElementException =>
              LOGGER.error(s"$logPrefix Image not exist in ${request.storageInfo}")
              for (
                resp <- errorHandler.onClientError(request, NOT_FOUND, "Image unexpectedly missing.")
              ) yield
                resp.withHeaders(CACHE_CONTROL -> s"public, max-age=30")

            case _ =>
              LOGGER.error(s"$logPrefix Failed to read image from ${request.storageInfo}", ex)
              for (
                resp <- errorHandler.onClientError(request, FAILED_DEPENDENCY, "Internal error occured during fetching/creating an image.")
              ) yield
                resp.withHeaders(RETRY_AFTER -> "60")
          }
        }
    }

}

