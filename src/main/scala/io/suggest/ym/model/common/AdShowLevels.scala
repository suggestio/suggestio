package io.suggest.ym.model.common

import scala.collection.JavaConversions._
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.08.14 17:32
 * Description: Разжиревшый Enum AdShowLevels вынесен из AdNet в ходе рефакторингов и реорганизаций уровней
 * отображения.
 */


/** Уровни отображения рекламы. Используется как bitmask, но через денормализацию поля. */
object AdShowLevels extends Enumeration {

  /**
   * Надстройка над исходным классом-значением.
   * @param name Исходный строковой id enum-элемента.
   * @param visualPrio Визуальный приоритет отображения. Если надо отобразить несколько галочек, то
   *                   они должны отображаться в неком стабильном порядке.
   * @param checkboxCssClass При рендере галочки, она должна иметь этот css-класс.
   */
  protected case class Val(
    name: String,
    visualPrio: Int,
    checkboxCssClass: String
  ) extends super.Val(name) with SlNameTokenStr

  type AdShowLevel = Val

  implicit def value2val(v: Value) = v.asInstanceOf[AdShowLevel]

  /** Сконвертить множество уровней в множество строковых id этих уровней. */

  implicit def sls2strings(sls: Set[AdShowLevel]) = sls.map(_.name)

  def withNameTyped(n: String): AdShowLevel = withName(n)


  /** Отображать на нулевом уровне, т.е. при входе в ТЦ/ресторан и т.д. */
  val LVL_START_PAGE: AdShowLevel = Val("d", 100, "firstPage-catalog")

  /** Отображать в каталоге продьюсеров. */
  val LVL_CATS: AdShowLevel = Val("h", 200, "common-catalog")

  /** Отображать эту рекламу внутри каталога продьюсера. */
  val LVL_PRODUCER: AdShowLevel = Val("m", 300, "shop-catalog")


  def maybeWithName(n: String): Option[AdShowLevel] = {
    try {
      Some(withName(n))
    } catch {
      case _: Exception => None
    }
  }


  /** Десериализатор списка уровней отображения. */
  val deserializeShowLevels: PartialFunction[Any, Set[AdShowLevel]] = {
    case v: java.lang.Iterable[_] =>
      v.map {
        rawSL => AdShowLevels.withName(rawSL.toString) : AdShowLevel
      }.toSet

    case s: String =>
      Set(AdShowLevels.withName(s))

    case null => Set.empty
  }

}
