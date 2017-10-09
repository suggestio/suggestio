package io.suggest.pick

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

    final def IMAGE   = "image"
    final def JPEG    = "jpeg"
    final def GIF     = "gif"
    final def PNG     = "png"

    final def BASE64  = "base64"

    final def MULTIPART = "multipart"
    final def FORM = "form"
    final def DATA = "data"
  }


  final def APPLICATION_JSON            = Words.APPLICATION_ + Words.JSON
  final def TEXT_HTML                   = Words.TEXT_ + "html"
  final def TEXT_PLAIN                  = Words.TEXT_ + "plain"
  final def APPLICATION_OCTET_STREAM    = Words.APPLICATION_ + "octet-stream"
  final def MULTIPART_FORM_DATA         = Words.MULTIPART + Words.DELIM1 + Words.FORM + "-" + Words.DATA


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

    /** MIME тип для JSON-представления JSON-document-тега. */
    //final def JDTAG_JSON = MIME_PREFIX + "jdt+" + Words.JSON

    /** Mime-тип с координатами в виде JSON. */
    final def COORD_2D_JSON = MIME_PREFIX + "coords-2d+" + Words.JSON

  }


  /** Mime-типы для картинок и сопутствующая им утиль. */
  object Image {

    final def PREFIX  = Words.IMAGE + Words.DELIM1

    final def JPEG    = PREFIX + Words.JPEG
    final def PNG     = PREFIX + Words.PNG
    final def GIF     = PREFIX + Words.GIF

    def isImage(ct: String): Boolean = {
      val ct2 = ct.toLowerCase()
      val prefix = PREFIX
      (ct2 startsWith prefix) && {
        val afterSlash = ct2.substring( prefix.length )
        (Words.JPEG :: Words.PNG :: Words.GIF :: Nil)
          .contains(afterSlash)
      }
    }

    /** Проверка Content-Type на предмет допустимости для upload'а
      * на сервер в качестве иллюстрации в рекламной карточке. */
    def isImageForAd(ct: String): Boolean = {
      isImage(ct)
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

}
