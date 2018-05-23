package io.suggest.sc.search

import io.suggest.common.empty.{EmptyProduct, EmptyUtil}
import io.suggest.geo.MLocEnv
import io.suggest.sc.TagSearchConstants.Req._
import io.suggest.sc.{MScApiVsn, MScApiVsns, ScConstants}
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

object MScTagsSearchQs {

  /** Поддержка play-json. */
  implicit def MSC_TAGS_SEARCH_QS: OFormat[MScTagsSearchQs] = (
    (__ \ FACE_FTS_QUERY_FN).formatNullable[String] and
    (__ \ LIMIT_FN).formatNullable[Int] and
    (__ \ OFFSET_FN).formatNullable[Int] and
    (__ \ LOC_ENV_FN).formatNullable[MLocEnv]
      .inmap[MLocEnv](
        EmptyUtil.opt2ImplMEmptyF( MLocEnv ),
        EmptyUtil.implEmpty2OptF
      ) and
    (__ \ RCVR_ID_FN).formatNullable[String] and
    (__ \ ScConstants.ReqArgs.VSN_FN).format[MScApiVsn]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MScTagsSearchQs] = UnivEq.derive

}


/** Дефолтовая реализация модели аргументов поиска тегов. */
case class MScTagsSearchQs(
                            tagsQuery   : Option[String]  = None,
                            limitOpt    : Option[Int]     = None,
                            offsetOpt   : Option[Int]     = None,
                            locEnv      : MLocEnv         = MLocEnv.empty,
                            rcvrId      : Option[String]  = None,
                            apiVsn      : MScApiVsn       = MScApiVsns.unknownVsn
                          )
  extends EmptyProduct
{

  def withLimitOffset(limitOpt: Option[Int] = None, offsetOpt: Option[Int] = None) = copy(limitOpt = limitOpt, offsetOpt = offsetOpt)

}
