package io.suggest.sc.sc3

import io.suggest.common.empty.EmptyUtil
import io.suggest.sc.ads.{MAdsSearchReq, MScFocusArgs}
import io.suggest.sc.index.MScIndexArgs
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.05.18 13:14
  * Description: Модель-контейнер для всех реквестов, идущих через Sc UApi.
  */
object MScQs {

  object Fields {
    val COMMON_FN         = "c"
    val ADS_SEARCH_FN     = "g"
    val INDEX_FN          = "i"
    val FOCUSED_ARGS_FN   = "f"
  }

  /** Поддержка json. Не ясно, нужна ли.
    * Для возможной генерации ссылок на клиенте - будет нужна. */
  implicit def mScQsFormat: OFormat[MScQs] = (
    (__ \ Fields.COMMON_FN).format[MScCommonQs] and
    (__ \ Fields.ADS_SEARCH_FN).formatNullable[MAdsSearchReq]
      .inmap[MAdsSearchReq](
        EmptyUtil.opt2ImplMEmptyF( MAdsSearchReq ),
        EmptyUtil.implEmpty2OptF
      ) and
    (__ \ Fields.INDEX_FN).formatNullable[MScIndexArgs] and
    (__ \ Fields.FOCUSED_ARGS_FN).formatNullable[MScFocusArgs]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MScQs] = UnivEq.derive

  val common = GenLens[MScQs](_.common)
  val search = GenLens[MScQs](_.search)
  val index  = GenLens[MScQs](_.index)
  val foc    = GenLens[MScQs](_.foc)

}


/** Класс-контейнер qs-аргументов для Sc3 UApi, позволяющего делать сложный запрос к Sc-контроллеру.
  *
  * @param ads Запрос поиска grid-ads
  * @param index Запрос index
  * @param foc Запрос фокусировки.
  * @param common Пошаренные части запросов.
  */
case class MScQs(
                  common    : MScCommonQs            = MScCommonQs.empty,
                  search    : MAdsSearchReq          = MAdsSearchReq.empty,
                  index     : Option[MScIndexArgs]   = None,
                  foc       : Option[MScFocusArgs]   = None,
                ) {

  /** Есть ли какие-то полезные данные для поиска карточек?
    * Если false, значит поисковый запрос на базе данных из этого инстанса вернёт вообще все карточки. */
  def hasAnySearchCriterias: Boolean = {
    common.locEnv.nonEmpty ||
      search.rcvrId.nonEmpty ||
      search.prodId.nonEmpty ||
      search.tagNodeId.nonEmpty
  }

  def withSearch(search: MAdsSearchReq) = copy(search = search)
  def withFoc(foc: Option[MScFocusArgs]) = copy(foc = foc)
  def withIndex(index: Option[MScIndexArgs]) = copy(index = index)
  def withCommon(common: MScCommonQs) = copy(common = common)

}
