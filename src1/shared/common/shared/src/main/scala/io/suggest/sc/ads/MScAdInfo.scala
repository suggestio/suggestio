package io.suggest.sc.ads

import io.suggest.common.empty.{EmptyProduct, EmptyUtil, IEmpty}
import io.suggest.n2.edge.{MEdgeFlagData, MPredicates}
import io.suggest.sc.ScConstants
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.11.2019 7:36
  * Description: Серверная доп.инфа по рекламной карточке, которая не требуется клиент-сайд-конвертации перед
  * сохранением в состояние sc3.
  */
object MScAdInfo extends IEmpty {

  override type T = MScAdInfo
  override lazy val empty = apply()

  @inline implicit def univEq: UnivEq[MScAdInfo] = UnivEq.derive

  implicit def scAdInfoJson: OFormat[MScAdInfo] = (
    (__ \ "e").formatNullable[Boolean] and
    (__ \ "f").formatNullable[Iterable[MEdgeFlagData]]
      .inmap[Iterable[MEdgeFlagData]](
        EmptyUtil.opt2ImplEmptyF( Nil ),
        { flags => if (flags.isEmpty) None else Some(flags) }
      ) and
    (__ \ "o").formatNullable[Iterable[MScAdMatchInfo]]
      .inmap[Iterable[MScAdMatchInfo]](
        EmptyUtil.opt2ImplEmptyF( Nil ),
        { matchInfos => Option.when( matchInfos.nonEmpty )( matchInfos ) }
      )
  )(apply, unlift(unapply))

  def canEditOpt = GenLens[MScAdInfo](_.canEditOpt)
  def flags = GenLens[MScAdInfo](_.flags)
  def matchInfos = GenLens[MScAdInfo](_.matchInfos)

}

/** Контейнер разной инфы по карточке с сервера, которая пробрасывается в состояние выдачи без доп.обработки.
  *
  * @param canEditOpt Может ли юзер редактировать карточку?
  * @param flags Какие-то доп-флаги, обрабатываемые на клиенте.
  * @param matchInfos Как была найдена указанная карточка
  *                   Предикат, без доп.подробностей, т.к. подробности для выдачи известны ещё на уровне qs.
  *                   (bt-маячок, геолокация, ресивер и тд.)
  */
case class MScAdInfo(
                      canEditOpt      : Option[Boolean]           = None,
                      flags           : Iterable[MEdgeFlagData]   = Nil,
                      matchInfos      : Iterable[MScAdMatchInfo]  = Nil,
                    )
  extends EmptyProduct
{

  def canEdit = canEditOpt contains true

  /** Это карточка 404? */
  def isMad404: Boolean = {
    matchInfos.exists { matchInfo =>
      (matchInfo.predicates contains MPredicates.Receiver) &&
      matchInfo.nodeMatchings.exists { nodeMatchInfo =>
        nodeMatchInfo.nodeId.exists( ScConstants.Mad404.is404Node )
      }
    }
  }

}
