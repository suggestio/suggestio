package io.suggest.text

import scala.scalajs.js
import scala.scalajs.js.URIUtils

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.18 15:16
  * Description: js-only утиль для работы со ссылками.
  */
object UrlUtilJs {

  /** Сериализация списка ключ-значение в строку для URL.
    *
    * @param acc Выхлоп toUrlHashAcc().
    * @return Строка, пригодная для записи в URL.
    */
  def qsPairsToString[T <: (String, Any)](acc: IterableOnce[T]): String = {
    acc
      .iterator
      .map { kv =>
        kv.productIterator
          .map { s =>
            URIUtils.encodeURIComponent(s.toString)
          }
          .mkString("=")
      }
      .mkString("&")
  }


  def qsKvsParse(kvs: IterableOnce[String]): Iterator[(String, String)] = {
    kvs
      .iterator
      // распарсить ключи и значения, причесать их
      .flatMap { kv =>
        kv.split('=') match {
          case Array(k, v) =>
            // из ключа надо удалить namespace-префикс вместе с точкой.
            val k2 = URIUtils.decodeURIComponent(k)
            val v2 = URIUtils.decodeURIComponent( v )
            Some(k2 -> v2)
          case _ => None
        }
      }
  }

}
