package io.suggest.url.bind

import io.suggest.common.qs.QsConstants
import japgolly.univeq._

object QueryStringBindableUtil {

  /** Catenate full URL query-string key using parent key and suffix. */
  def key1(key: String, suf: String): String =
    key + QsConstants.KEY_PARTS_DELIM_STR + suf

  def key1F(key: String): String => String =
    key1(key, _)

  /** Catenate zero or more fields into single URL query-string. */
  def _mergeUnbinded(unbinded: IterableOnce[String]): String = {
    unbinded
      .iterator
      .filter(_.nonEmpty)
      .mkString("&")
  }

  def _mergeUnbinded1(unbinded: String*): String =
    _mergeUnbinded(unbinded)

}
