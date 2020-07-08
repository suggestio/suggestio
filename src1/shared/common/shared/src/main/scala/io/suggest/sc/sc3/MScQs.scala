package io.suggest.sc.sc3

import io.suggest.common.empty.EmptyUtil
import io.suggest.sc.ads.{MAdsSearchReq, MScFocusArgs, MScGridArgs, MScNodesArgs}
import io.suggest.sc.index.MScIndexArgs
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.05.18 13:14
  * Description: Модель-контейнер для всех реквестов, идущих через ScUniApi.
  */
object MScQs {

  object Fields {
    val COMMON_FN         = "c"
    val ADS_SEARCH_FN     = "g"
    val INDEX_FN          = "i"
    val FOCUSED_ARGS_FN   = "f"
    val GRID_FN           = "d"
    val NODES_FN          = "n"
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
    (__ \ Fields.FOCUSED_ARGS_FN).formatNullable[MScFocusArgs] and
    (__ \ Fields.GRID_FN).formatNullable[MScGridArgs] and
    (__ \ Fields.NODES_FN).formatNullable[MScNodesArgs]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MScQs] = UnivEq.derive

  def common = GenLens[MScQs](_.common)
  def search = GenLens[MScQs](_.search)
  def index  = GenLens[MScQs](_.index)
  def foc    = GenLens[MScQs](_.foc)
  def grid = GenLens[MScQs](_.grid)
  def nodes = GenLens[MScQs](_.nodes)


  implicit final class MScQsExt(private val scQs: MScQs) extends AnyVal {

    def withSearch = MScQs.search.set(_: MAdsSearchReq)(scQs)


    /** Есть ли какие-то полезные данные для поиска карточек?
      * Если false, значит поисковый запрос на базе данных из этого инстанса вернёт вообще все карточки. */
    def hasAnySearchCriterias: Boolean = {
      scQs.common.locEnv.nonEmpty ||
      scQs.search.rcvrId.nonEmpty ||
      scQs.search.prodId.nonEmpty ||
      scQs.search.tagNodeId.nonEmpty
    }

  }

}


/** Класс-контейнер qs-аргументов для Sc3 UApi, позволяющего делать сложный запрос к Sc-контроллеру.
  *
  * @param search Запрос поиска grid-ads
  * @param index Запрос index
  * @param foc Запрос фокусировки.
  * @param common Пошаренные части запросов.
  * @param grid Данные, пробрасываем в ScAdsTile.
  * @param nodes Параметры поиска узлов.
  */
case class MScQs(
                  common    : MScCommonQs            = MScCommonQs.empty,
                  search    : MAdsSearchReq          = MAdsSearchReq.empty,
                  index     : Option[MScIndexArgs]   = None,
                  foc       : Option[MScFocusArgs]   = None,
                  grid      : Option[MScGridArgs]    = None,
                  nodes     : Option[MScNodesArgs]   = None,
                )
