package io.suggest.stat.m

import io.suggest.common.menum.{EnumJsonReadsValT, EnumMaybeWithName, StrIdValT}

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
object MComponents extends EnumMaybeWithName with EnumJsonReadsValT with StrIdValT {

  /** Класс для всех экземпляров модели.
    *
    * @param strId LOWER CASE ONLY!
    */
  protected class Val(override val strId: String)
    extends super.Val
    with ValT

  override type T = Val


  /** Крупный компонент sc (showcase), т.е. выдача на главной. */
  val Sc        : T = new Val("выдача")

  /** Сайт (для single-page app типа выдачи). */
  val Site      : T = new Val("site")

  /** Некие index-страницы. */
  val Index     : T = new Val("index")

  /** Карточка. */
  val Ad        : T = new Val("карточка")

  /** Открытие другого "компонента". Например фокусировка на карточке в выдаче. */
  val Open      : T = new Val("открыть")

  /** Плитка карточек. */
  val Tile      : T = new Val("плитка")

  /** Тег или теги. */
  val Tags      : T = new Val("теги")

  /** Некий-то "компонент", генерящий статистику ошибки.
    * Обычно применяется в связке с другими компонентами, чтобы уточнить суть. */
  val Error     : T = new Val("ошибка")

  /** Content-Security-Policy. */
  val CSP       : T = new Val("csp")

  //val Lk: T       = new Val("ЛК")
  //val Sys: T      = new Val("SYS")
  //...

}
