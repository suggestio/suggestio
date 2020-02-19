package io.suggest.pick

import io.suggest.img.MImgFmts
import scalaz.{Validation, ValidationNel}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.09.17 17:33
  * Description: Константы для Mime
  */
object MimeConst {

  /** Общие куски слов, из которых можно составлять MIME-типы. */
  object Words {

    final def DELIM1 = "/"
    final def APPLICATION_ = "application" + DELIM1
    final def TEXT_ = "text" + DELIM1
    final def JSON = "json"

    // Картинки живут полностью в MImgFmts.

    final def BASE64  = "base64"

    final def MULTIPART = "multipart"
    final def FORM = "form"
    final def DATA = "data"
    final def MANIFEST = "manifest"
  }


  final def APPLICATION_JSON            = Words.APPLICATION_ + Words.JSON
  final def TEXT_HTML                   = Words.TEXT_ + "html"
  final def TEXT_PLAIN                  = Words.TEXT_ + "plain"
  final def APPLICATION_OCTET_STREAM    = Words.APPLICATION_ + "octet-stream"

  // !!! Если выставлять это в запросы вручную, то надо не зыбывать обязательный "; boundary=..."
  final def MULTIPART_FORM_DATA         = Words.MULTIPART + Words.DELIM1 + Words.FORM + "-" + Words.DATA

  final def WEB_APP_MANIFEST            = Words.APPLICATION_ + Words.MANIFEST + "+" + Words.JSON


  /** Внутренние sio-mime-типы. */
  object Sio {

    final private def MIME_PREFIX = Words.APPLICATION_ + "prs.sio-"

    /** Mime-тип данных, описывающий тип sio-данных. */
    final def DATA_CONTENT_TYPE   = MIME_PREFIX + "ct"


    /** Допустимые значения для ключа DATA_CONTENT_TYPE, т.е. название sio-типов данных. */
    object DataContentTypes {

      /** Тип: целый стрип, т.е. один блок карточки. */
      final def STRIP         = "s"

      /** Тип: элемент стрипа, например QdTag() или AbsPos(). */
      final def CONTENT_ELEMENT = "ce"

    }

  }



  /** Валидация mime-типа картинки.
    *
    * @param mime MIME-тип, требующий проверки.
    * @return ValidationNel.
    */
  def validateMimeUsing(mime: String, verifierF: String => Boolean): ValidationNel[String, String] = {
    Validation.liftNel(mime)( !verifierF(_), "e.mime.unexpected" )
  }


  /** Проверки для mime-типов загружаемых файлов. */
  object MimeChecks {

    def _anyMimeRE = "(application|audio|image|message|multipart|text|video)/(x-|vnd.)?[.a-z0-9-]{2,65}".r

    def onlyImages(mime: String): Boolean = {
      MImgFmts
        .withMime( mime )
        .nonEmpty
    }

    def all(mime: String): Boolean =
       _anyMimeRE.matches(mime)

  }

}
