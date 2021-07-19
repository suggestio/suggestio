package io.suggest.lk.nodes

import enumeratum.values._
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.2020 9:24
  * Description: Кросс-платформенная модель ключей для точечно-обновляемых значений в форме lk-nodes.
  * Появилась с целью унификации зоопарка различных флагов в узлах, хотя одними лишь флагами форма не ограничена.
  */
object MLknOpKeys extends StringEnum[MLknOpKey] {

  /** ADN-mode: Узел включён?
    * Adv-mode: Размещение карточки на узле активно?
    */
  case object AdvEnabled extends MLknOpKey("a")

  case object NodeEnabled extends MLknOpKey("e")

  // Adv-only:

  /** Показывать карточку всегда раскрытой. */
  case object ShowOpened extends MLknOpKey("o")

  /** Карточка отображается всегда в обводке. */
  case object AlwaysOutlined extends MLknOpKey("l")


  override def values = findValues

}


/** Ключ для обновляемого значения. */
sealed abstract class MLknOpKey(override val value: String ) extends StringEnumEntry

object MLknOpKey {

  @inline implicit def univEq: UnivEq[MLknOpKey] = UnivEq.derive

  implicit def lknOpKeyJson: Format[MLknOpKey] =
    EnumeratumUtil.valueEnumEntryFormat( MLknOpKeys )

  implicit def OpKeyJson = new EnumeratumUtil.ValueEnumEntryKeyReadsWrites( MLknOpKeys )( identity )


  implicit final class OptionsKeyExt( private val ok: MLknOpKey ) extends AnyVal {

    def isAdn: Boolean = {
      ok ==* MLknOpKeys.AdvEnabled
    }

    def isAdv: Boolean = {
      (ok ==* MLknOpKeys.AdvEnabled) ||
      (ok ==* MLknOpKeys.ShowOpened) ||
      (ok ==* MLknOpKeys.AlwaysOutlined)
    }

  }

}