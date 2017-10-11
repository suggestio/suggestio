package models.mup

import io.suggest.crypto.hash.HashesHex
import io.suggest.model.n2.media.storage.MStorage
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.sec.QsbSigner
import io.suggest.sec.m.SecretGetter
import io.suggest.util.logs.MacroLogsImplLazy
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.10.17 15:13
  * Description: URL-qs-модель, защищённая подписью, для ссылки аплоада файла на сервер.
  */

object MUploadTargetQs {

  /** Имена полей модели. */
  object Fields {

    val HASHES_HEX_FN     = "h"
    val MIME_TYPE_FN      = "m"
    val FILE_SIZE_B_FN    = "b"
    val FILE_HANDLER_FN   = "d"
    val PERSON_ID_FN      = "p"
    val VALID_TILL_FN     = "c"
    val STORAGE_FN        = "s"
    val STORAGE_HOST_FN   = "o"
    val STORAGE_INFO_FN   = "i"
    val COLOR_DETECT_FN   = "l"

    val SIGNATURE_FN      = "z"

  }


  /** Статический секретный ключ для подписывания запросов. */
  private val SIGN_SECRET: String = {
    val sg = new SecretGetter with MacroLogsImplLazy {
      override def confKey = "upload.url.sign.secret"
    }
    sg()
  }

  /** Поддержка QueryStringBindable. */
  implicit def uploadTargetQsQsb(implicit
                                 hashesHexB         : QueryStringBindable[HashesHex],
                                 strB               : QueryStringBindable[String],
                                 longB              : QueryStringBindable[Long],
                                 fileHandlerOptB    : QueryStringBindable[Option[MUploadFileHandler]],
                                 storageB           : QueryStringBindable[MStorage],
                                 strOptB            : QueryStringBindable[Option[String]],
                                 cdArgsOptB         : QueryStringBindable[Option[MColorDetectArgs]]
                                ): QueryStringBindable[MUploadTargetQs] = {
    new QueryStringBindableImpl[MUploadTargetQs] {
      def getQsbSigner(key: String) = new QsbSigner(SIGN_SECRET, Fields.SIGNATURE_FN)

      /** Биндинг значения [[MUploadTargetQs]] из URL qs. */
      override def bind(key: String, params0: Map[String, Seq[String]]): Option[Either[String, MUploadTargetQs]] = {
        val F = Fields
        val k = key1F(key)
        for {
          params            <- getQsbSigner(key).signedOrNone(k(""), params0)
          hashesHexE        <- hashesHexB.bind        ( k(F.HASHES_HEX_FN),   params )
          mimeTypeE         <- strB.bind              ( k(F.MIME_TYPE_FN),    params )
          fileSizeE         <- longB.bind             ( k(F.FILE_SIZE_B_FN),  params )
          fileHandlerOptE   <- fileHandlerOptB.bind   ( k(F.FILE_HANDLER_FN), params )
          personIdOptE      <- strOptB.bind           ( k(F.PERSON_ID_FN),    params )
          validTillE        <- longB.bind             ( k(F.VALID_TILL_FN),   params )
          storageE          <- storageB.bind          ( k(F.STORAGE_FN),      params )
          storHostE         <- strB.bind              ( k(F.STORAGE_HOST_FN), params )
          storInfoE         <- strB.bind              ( k(F.STORAGE_INFO_FN), params )
          colorDetectE      <- cdArgsOptB.bind        ( k(F.COLOR_DETECT_FN), params )
        } yield {
          for {
            hashesHex       <- hashesHexE.right
            mimeType        <- mimeTypeE.right
            fileSize        <- fileSizeE.right
            fileHandlerOpt  <- fileHandlerOptE.right
            personIdOpt     <- personIdOptE.right
            validTill       <- validTillE.right
            storage         <- storageE.right
            storHost        <- storHostE.right
            storInfo        <- storInfoE.right
            colorDetect     <- colorDetectE.right
          } yield {
            MUploadTargetQs(
              hashesHex     = hashesHex,
              mimeType      = mimeType,
              fileSizeB     = fileSize,
              fileHandler   = fileHandlerOpt,
              personId      = personIdOpt,
              validTillS    = validTill,
              storage       = storage,
              storHost      = storHost,
              storInfo      = storInfo,
              colorDetect   = colorDetect
            )
          }
        }
      }       // bind()


      /** Сериализация полей [[MUploadTargetQs]] в строку URL qs. */
      override def unbind(key: String, value: MUploadTargetQs): String = {
        val F = Fields
        val k = key1F(key)
        val unsigned = _mergeUnbinded1(
          hashesHexB.unbind         ( k(F.HASHES_HEX_FN),       value.hashesHex   ),
          strB.unbind               ( k(F.MIME_TYPE_FN),        value.mimeType    ),
          longB.unbind              ( k(F.FILE_SIZE_B_FN),      value.fileSizeB   ),
          fileHandlerOptB.unbind    ( k(F.FILE_HANDLER_FN),     value.fileHandler ),
          strOptB.unbind            ( k(F.PERSON_ID_FN),        value.personId    ),
          longB.unbind              ( k(F.VALID_TILL_FN),       value.validTillS  ),
          storageB.unbind           ( k(F.STORAGE_FN),          value.storage     ),
          strB.unbind               ( k(F.STORAGE_HOST_FN),     value.storHost    ),
          strB.unbind               ( k(F.STORAGE_INFO_FN),     value.storInfo    ),
          cdArgsOptB.unbind         ( k(F.COLOR_DETECT_FN),     value.colorDetect )
        )
        // Подписать это всё.
        getQsbSigner(key)
          .mkSigned(key, unsigned)
      }     // unbind()

    }       // new QSBImpl
  }         // implicit def qsb

}


/** Класс модели для заливки данных на сервер.
  *
  * @param hashesHex Карта контрольных сумм файла.
  * @param fileSizeB Размер файла.
  * @param fileHandler Опциональный режим перехвата файла в контроллере, чтобы вместо /tmp/... сразу сохранять в иное место.
  * @param personId id юзера.
  * @param validTillS TTL. Вычисляется как currentTimeMillis/1000 + TTL в момент генерации ссылки (в секундах).
  * @param storage Отправить файл на хранение в указанный storage. Например, SeaWeedFS.
  * @param storHost Хост стораджа, т.к. URL hostname не покрывается сигнатурой модели.
  * @param storInfo Строка данных, воспринимаемая конкретным storage'ем, нужная для сохранения.
  *                 Например, для SeaWeedFS это будет зарезрвированный fid.
  * @param colorDetect Запустить MainColorDetector после.
  *                    Значение -- кол-во цветов, которые надо отправить на клиент.
  */
case class MUploadTargetQs(
                            hashesHex   : HashesHex,
                            mimeType    : String,
                            fileSizeB   : Long,
                            fileHandler : Option[MUploadFileHandler],
                            personId    : Option[String],
                            validTillS  : Long,
                            storage     : MStorage,
                            storHost    : String,
                            storInfo    : String,
                            colorDetect : Option[MColorDetectArgs]
                          )
