package io.suggest.sjs.common.tags.search

import io.suggest.sc.TagSearchConstants.Req._
import io.suggest.sjs.common.model.ToJsonDictDummyT

import scala.scalajs.js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.09.15 20:43
 * Description: Модель аргументов для поиска тегов.
 */
trait ITagSearchArgs extends ToJsonDictDummyT {

  /** Полнотекстовый поиск по человеческим названиям тегов. */
  def faceFts   : Option[String]

  /** Лимит выборки. */
  def limit     : Option[Int]

  /** Абсолютный сдвиг выборки. */
  def offset    : Option[Int]

  override def toJson: Dictionary[Any] = {
    val d = super.toJson

    val _faceFts = faceFts
    if (_faceFts.isDefined)
      d(FACE_FTS_QUERY_FN) = _faceFts.get

    val _limit = limit
    if (_limit.isDefined)
      d(LIMIT_FN) = _limit.get

    val _offset = offset
    if (_offset.isDefined)
      d(OFFSET_FN) = _offset.get

    d
  }

}


/** Дефолтовая реализация модели аргументов поискового запроса тегов. */
case class MTagSearchArgs(
  override val faceFts  : Option[String]    = None,
  override val limit    : Option[Int]       = None,
  override val offset   : Option[Int]       = None
)
  extends ITagSearchArgs
