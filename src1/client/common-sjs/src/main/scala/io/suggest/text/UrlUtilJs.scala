package io.suggest.text

import scala.scalajs.js.URIUtils

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.18 15:16
  * Description: js-only утиль для работы со ссылками.
  */
object UrlUtilJs {

  /** Сериализация списк ключ-значение в строку для URL.
    *
    * @param acc Выхлоп toUrlHashAcc().
    * @return Строка, пригодная для записи в URL.
    */
  def qsPairsToString[T <: (String, Any)](acc: TraversableOnce[T]): String = {
    acc.toIterator
      .map { kv =>
        kv.productIterator
          .map { s =>
            URIUtils.encodeURIComponent(s.toString)
          }
          .mkString("=")
      }
      .mkString("&")
  }

}
