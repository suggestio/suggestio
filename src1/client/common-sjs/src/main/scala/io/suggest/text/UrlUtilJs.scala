package io.suggest.text

import scala.scalajs.js.URIUtils
import japgolly.univeq._

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
            URIUtils.encodeURIComponent( s.toString )
          }
          .mkString("=")
      }
      .mkString("&")
  }


  def urlQsTokenize(qsRaw: String): Array[String] = {
    qsRaw
      .split('&')
  }


  def qsKvsParse(kvs: IterableOnce[String]): Iterator[(String, String)] = {
    kvs
      .iterator
      // распарсить ключи и значения, причесать их
      .flatMap { kv =>
        kv.split('=') match {
          case arr if arr.length ==* 2 =>
            val Array(k2, v2) = arr.map( URIUtils.decodeURIComponent )
            Some((k2, v2))
          case _ => None
        }
      }
  }


  def qsParseToMap( qsRaw: String ): Map[String, Seq[String]] = {
    qsKvsParse( urlQsTokenize( qsRaw ) )
      .iterator
      .to( Seq )
      .groupMap( _._1 )( _._2 )
  }

}
