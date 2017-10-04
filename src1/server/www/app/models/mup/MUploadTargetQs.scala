package models.mup

import io.suggest.crypto.hash.HashesHex
import io.suggest.model.n2.media.storage.MStorage
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.sec.m.SecretGetter
import io.suggest.util.logs.MacroLogsImplLazy
import play.api.mvc.QueryStringBindable
import util.qsb.QsbSigner

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
    val PERSON_ID_FN      = "p"
    val VALID_TILL_FN     = "c"
    val STORAGE_FN        = "s"
    val STORAGE_INFO_FN   = "i"

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
                                 hashesHexB     : QueryStringBindable[HashesHex],
                                 strB           : QueryStringBindable[String],
                                 longB          : QueryStringBindable[Long],
                                 storageOptB    : QueryStringBindable[Option[MStorage]],
                                 strOptB        : QueryStringBindable[Option[String]]
                                ): QueryStringBindable[MUploadTargetQs] = {
    new QueryStringBindableImpl[MUploadTargetQs] {
      def getQsbSigner(key: String) = new QsbSigner(SIGN_SECRET, Fields.SIGNATURE_FN)

      /** Биндинг значения [[MUploadTargetQs]] из URL qs. */
      override def bind(key: String, params0: Map[String, Seq[String]]): Option[Either[String, MUploadTargetQs]] = {
        val F = Fields
        val k = key1F(key)
        for {
          params            <- getQsbSigner(key).signedOrNone(k(""), params0)
          hashesHexE        <- hashesHexB.bind    ( k(F.HASHES_HEX_FN),   params )
          mimeTypeE         <- strB.bind          ( k(F.MIME_TYPE_FN),    params )
          fileSizeE         <- longB.bind         ( k(F.FILE_SIZE_B_FN),  params )
          personIdOptE      <- strOptB.bind       ( k(F.PERSON_ID_FN),    params )
          validTillE        <- longB.bind         ( k(F.VALID_TILL_FN),   params )
          storageOptE       <- storageOptB.bind   ( k(F.STORAGE_FN),      params )
          storInfoOptE      <- strOptB.bind       ( k(F.STORAGE_INFO_FN), params )
        } yield {
          for {
            hashesHex       <- hashesHexE.right
            mimeType        <- mimeTypeE.right
            fileSize        <- fileSizeE.right
            personIdOpt     <- personIdOptE.right
            validTill       <- validTillE.right
            storageOpt      <- storageOptE.right
            storInfoOpt     <- storInfoOptE.right
          } yield {
            MUploadTargetQs(
              hashesHex     = hashesHex,
              mimeType      = mimeType,
              fileSizeB     = fileSize,
              personId      = personIdOpt,
              validTillS    = validTill,
              storage       = storageOpt,
              storInfo      = storInfoOpt
            )
          }
        }
      }       // bind()


      /** Сериализация полей [[MUploadTargetQs]] в строку URL qs. */
      override def unbind(key: String, value: MUploadTargetQs): String = {
        val F = Fields
        val k = key1F(key)
        val unsigned = _mergeUnbinded1(
          hashesHexB.unbind   ( k(F.HASHES_HEX_FN),       value.hashesHex ),
          strB.unbind         ( k(F.MIME_TYPE_FN),        value.mimeType ),
          longB.unbind        ( k(F.FILE_SIZE_B_FN),      value.fileSizeB ),
          strOptB.unbind      ( k(F.PERSON_ID_FN),        value.personId ),
          longB.unbind        ( k(F.VALID_TILL_FN),       value.validTillS ),
          storageOptB.unbind  ( k(F.STORAGE_FN),          value.storage ),
          strOptB.unbind      ( k(F.STORAGE_INFO_FN),     value.storInfo )
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
  * @param personId id юзера.
  * @param validTillS TTL. Вычисляется как currentTimeMillis/1000 + TTL в момент генерации ссылки (в секундах).
  * @param storage Отправить файл на хранение в указанный storage. Например, SeaWeedFS.
  * @param storInfo Строка данных, воспринимаемая конкретным storage'ем, нужная для сохранения.
  *                 Например, для SeaWeedFS это будет зарезрвированный fid.
  */
case class MUploadTargetQs(
                            hashesHex   : HashesHex,
                            mimeType    : String,
                            fileSizeB   : Long,
                            personId    : Option[String],
                            validTillS  : Long,
                            storage     : Option[MStorage],
                            storInfo    : Option[String]
                          )
