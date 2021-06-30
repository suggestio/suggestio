package io.suggest.url.bind

import scala.collection.immutable.AbstractMap
import scala.scalajs.js
import japgolly.univeq._

import scala.scalajs.js.URIUtils
import scala.util.Try


object QsBindableUtilJs {

  /** Make URL query-string binding function.
    * Expecting already urlDecoded key/values map. */
  private def _bindFirst[T](toT: String => T): QsBinderF[T] = { (key, params) =>
    for {
      values    <- params.get( key )
      value     <- values.headOption
    } yield {
      Try( toT(value) )
        .toEither
        .left.map(_.getMessage)
    }
  }

  def _unbind[T](stringify: T => String = stringifyDefault): QsUnbinderF[T] = { (key, value) =>
    URIUtils.encodeURIComponent(key) + "=" + URIUtils.encodeURIComponent(stringify(value))
  }

  def stringifyDefault[T]: T => String = _.toString

  /** Client-side analogue for play.QueryStringBindable.bindableString
    * binding: Use UrlUtilJs.qsKvsParse() to parse URL parts, and decodeURIcomponent() will be already done. */
  implicit def qsBindableString: QsBindable[String] = {
    new QsBindable[String] {
      override def bindF = _bindFirst[String](identity)
      override def unbindF = _unbind[String]()
    }
  }


  implicit def qsBindableLong: QsBindable[Long] = {
    new QsBindable[Long] {
      override def bindF = _bindFirst(_.toLong)
      override def unbindF = _unbind[Long]()
    }
  }


  implicit def qsBindableInt: QsBindable[Int] = {
    new QsBindable[Int] {
      override def bindF = _bindFirst(_.toInt)
      override def unbindF = _unbind[Int]()
    }
  }


  implicit def qsBindableBoolean: QsBindable[Boolean] = {
    new QsBindable[Boolean] {
      override def bindF = _bindFirst(_.toBoolean)
      override def unbindF = _unbind[Boolean]()
    }
  }


  /** Wrap js.Dictionary into s.c.i.AbstractMap, with converting values into single-length Seq[].
    * Used for dirty zero-cost convertion from raw URL QueryString JsObject dictionary into immutable map,
    * compatible with play.QueryStringBindable.bind() method.
    */
  def unsafeJSDictionaryAsImmutableMap[T](qsDict: js.WrappedDictionary[T]): Map[String, Seq[T]] = {
    new AbstractMap[String, Seq[T]] {
      private def _toSeqValue( s: T ) =
        s :: Nil

      private def _toSeqKeyValue( kv: (String, T) ) =
        kv._1 -> _toSeqValue(kv._2)

      override def removed(key: String): Map[String, Seq[T]] = {
        qsDict
          .iterator
          .filter(_._1 !=* key)
          .map( _toSeqKeyValue )
          .toMap
      }

      override def updated[V1 >: Seq[T]](key: String, value: V1): Map[String, V1] = {
        iterator
          .map { case kv0 @ (k, _) =>
            if (k ==* key) k -> value
            else kv0
          }
          .toMap
      }

      override def get(key: String): Option[Seq[T]] = {
        qsDict
          .get(key)
          .map( _toSeqValue )
      }

      override def iterator: Iterator[(String, Seq[T])] = {
        qsDict
          .iterator
          .map( _toSeqKeyValue )
      }
    }
  }

}
