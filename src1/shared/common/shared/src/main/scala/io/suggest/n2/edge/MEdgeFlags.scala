package io.suggest.n2.edge

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq._
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.11.2019 14:39
  * Description: Модель произвольных флагов для эджа.
  * Идёт на смену поля flag, который максимум на один флаг.
  */
object MEdgeFlags extends StringEnum[MEdgeFlag] {

  /** Всегда рендерить карточку с обводкой, даже если карточка скрыта.
    * Используется при размещении карточки на своих узлах.
    * client-side флаг.
    */
  case object AlwaysOutlined extends MEdgeFlag("AO")

  /** Текущий эдж находится в подвешенном состоянии: ещё пока готовится и не совсем готов.
    * Например, происходит chunked upload.
    */
  case object InProgress extends MEdgeFlag("~")

  /** Всегда раскрытая (на конкретном узле) карточка. */
  case object AlwaysOpened extends MEdgeFlag("OP")


  override def values = findValues

}


/** Класс edge-флага.
  *
  * @param value Сериализуемое значение.
  */
sealed abstract class MEdgeFlag(override val value: String) extends StringEnumEntry

object MEdgeFlag {

  @inline implicit def univEq: UnivEq[MEdgeFlag] = UnivEq.derive

  implicit def edgeFlagJson: Format[MEdgeFlag] =
    EnumeratumUtil.valueEnumEntryFormat( MEdgeFlags )


  implicit class EdgeFlagOpsExt( val ef: MEdgeFlag ) extends AnyVal {

    /** Разрешается отправлять данный флаг в выдачу? */
    def isScClientSide: Boolean = {
      ef match {
        case MEdgeFlags.InProgress => false
        // AlwaysOutlined - отрабатывается только на клиенте.
        // AlwaysOpened - отрабатывается на клиенте и на сервере.
        case _ => true
      }
    }

  }

}
