package io.suggest.sc.ads

import io.suggest.common.empty.{EmptyProduct, EmptyUtil, IEmpty}
import io.suggest.model.n2.edge.MEdgeFlagData
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._

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
      )
  )(apply, unlift(unapply))

}

/** Контейнер разной инфы по карточке с сервера, которая пробрасывается в состояние выдачи без доп.обработки.
  *
  * @param canEditOpt Может ли юзер редактировать карточку?
  * @param flags Какие-то доп-флаги, обрабатываемые на клиенте.
  */
case class MScAdInfo(
                      canEditOpt      : Option[Boolean]           = None,
                      flags           : Iterable[MEdgeFlagData]   = Nil,
                    )
  extends EmptyProduct
{

  def canEdit = canEditOpt contains true

}
