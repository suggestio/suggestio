package io.suggest.pick

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
  }


  final def APPLICATION_JSON            = Words.APPLICATION_ + Words.JSON
  final def TEXT_HTML                   = Words.TEXT_ + "html"
  final def TEXT_PLAIN                  = Words.TEXT_ + "plain"
  final def APPLICATION_OCTET_STREAM    = Words.APPLICATION_ + "octet-stream"


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

}
