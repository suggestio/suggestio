package io.suggest.ym.model.common

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.menum.EnumJsonReadsValT

import scala.collection.JavaConversions._
import io.suggest.sc.ScConstants.ShowLevels._
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.08.14 17:32
 * Description:  Уровни отображения рекламных карточек. */
object AdShowLevels extends EnumMaybeWithName with EnumJsonReadsValT {

  /**
   * Надстройка над исходным классом-значением.
   * @param name Исходный строковой id enum-элемента.
   */
  protected[this] abstract class Val(val name: String)
   extends super.Val(name)
   with SlNameTokenStr {

    /** Визуальный приоритет отображения. Если надо отобразить несколько галочек, то
      * они должны отображаться в неком стабильном порядке. */
    def visualPrio: Int

    /** При рендере галочки, она должна иметь этот css-класс. */
    def checkboxCssClass: String

  }

  override type T = Val

  /** Сконвертить множество уровней в множество строковых id этих уровней. */
  implicit def sls2strings(sls: Set[T]) = sls.map(_.name)

  def withNameTyped(n: String): T = withName(n)


  /** Отображать на нулевом уровне, т.е. при входе в ТЦ/ресторан и т.д. */
  val LVL_START_PAGE: T = new Val(ID_START_PAGE) {
    override def visualPrio = 100
    override def checkboxCssClass = "firstPage-catalog"
  }

  /** Отображать в каталоге продьюсеров. */
  val LVL_CATS: T = new Val(ID_CATS) {
    override def visualPrio = 200
    override def checkboxCssClass = "common-catalog"
  }

  /** Отображать эту рекламу внутри каталога продьюсера. */
  val LVL_PRODUCER: T = new Val(ID_PRODUCER) {
    override def visualPrio = 300
    override def checkboxCssClass = "shop-catalog"
  }


  /** Десериализатор списка уровней отображения для jackson. */
  // TODO N2 спилить это счастье отсюда.
  val deserializeShowLevels: PartialFunction[Any, Set[T]] = {
    case v: java.lang.Iterable[_] =>
      v.map {
        rawSL => AdShowLevels.withName(rawSL.toString) : T
      }.toSet

    case s: String =>
      Set(AdShowLevels.withName(s))

    case null => Set.empty
  }

}
