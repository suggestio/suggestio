package io.suggest.sjs.common.tags.search

import io.suggest.sc.TagSearchConstants.Req._
import io.suggest.sjs.common.model.loc.{ILocEnv, MLocEnv}

import scala.scalajs.js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.09.15 20:43
 * Description: Модель аргументов для поиска тегов.
 */
object MTagSearchArgs {

  /**
    * Рендер инстанса модели в JSON.
    *
    * @param args Аргументы для рендера.
    * @return JSON-объект, для дальнейшего рендера в ?query=string на стороне JS-роутера.
    */
  def toJson(args: MTagSearchArgs): Dictionary[Any] = {
    val d = Dictionary.empty[Any]

    for (_faceFts <- args.faceFts)
      d(FACE_FTS_QUERY_FN) = _faceFts

    for (_limit <- args.limit)
      d(LIMIT_FN) = _limit

    for (_offset <- args.offset)
      d(OFFSET_FN) = _offset

    val _locEnv = args.locEnv
    if ( MLocEnv.nonEmpty(_locEnv))
      d(LOC_ENV_FN) = MLocEnv.toJson(_locEnv)

    d
  }

}


/** Дефолтовая реализация модели аргументов поискового запроса тегов. */
case class MTagSearchArgs(
  faceFts  : Option[String]    = None,
  limit    : Option[Int]       = None,
  offset   : Option[Int]       = None,
  locEnv   : ILocEnv           = MLocEnv.empty
)
