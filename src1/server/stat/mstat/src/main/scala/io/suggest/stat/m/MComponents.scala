package io.suggest.stat.m

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.09.16 18:49
  * Description: Модель тегов компонентов системы s.io.
  *
  * 2016.nov.10: "Компоненты" стали более мелкими вещами, а именно тегами, позволяющими установить
  * какую-то цепочку идентификаторов разнокалиберных компонентов системы. Например:
  * - "выдача" + "ошибка" = значит, что компонент был ScRemoteError.
  * - "выдача" + AdOpen   = фокусировка выдачи на карточке.
  * и т.д.
  */
object MComponents extends StringEnum[MComponent] {

  /** Крупный компонент sc (showcase), т.е. выдача на главной. */
  case object Sc extends MComponent("выдача")

  /** Сайт (для single-page app типа выдачи). */
  case object Site extends MComponent("site")

  /** Некие index-страницы. */
  case object Index extends MComponent("index")

  /** Карточка. */
  case object Ad extends MComponent("карточка")

  /** Открытие другого "компонента". Например фокусировка на карточке в выдаче. */
  case object Open extends MComponent("открыть")

  /** Плитка карточек. */
  case object Tile extends MComponent("плитка")

  /** Тег или теги. */
  case object Tags extends MComponent("теги")

  /** Некий-то "компонент", генерящий статистику ошибки.
    * Обычно применяется в связке с другими компонентами, чтобы уточнить суть. */
  case object Error extends MComponent("ошибка")

  /** Content-Security-Policy. */
  case object CSP extends MComponent("csp")

  //val Lk: T       = new Val("ЛК")
  //val Sys: T      = new Val("SYS")
  //...

  override def values = findValues

}


sealed abstract class MComponent(override val value: String) extends StringEnumEntry {
  def strId = value
}

object MComponent {

  @inline implicit def univEq: UnivEq[MComponent] = UnivEq.derive

  implicit def mComponentFormat: Format[MComponent] =
    EnumeratumUtil.valueEnumEntryFormat( MComponents )

}
