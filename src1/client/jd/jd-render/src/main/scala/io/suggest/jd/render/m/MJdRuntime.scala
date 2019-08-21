package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.common.geom.d2.MSize2di
import io.suggest.dev.MSzMult
import io.suggest.grid.GridCalc
import io.suggest.jd.MJdConf
import io.suggest.jd.render.v.JdCss
import io.suggest.jd.tags.JdTag
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.07.19 18:54
  * Description: Модель рантаймовых данных, которые целиком пакетно перегенеривается
  * при изменении плитки (шаблонов, jd-конфига и т.д.).
  */
object MJdRuntime {

  implicit object MJdRuntimeFastEq extends FastEq[MJdRuntime] {
    override def eqv(a: MJdRuntime, b: MJdRuntime): Boolean = {
      (a.jdCss ===* b.jdCss) &&
      (a.jdtWideSzMults ===* b.jdtWideSzMults)
    }
  }


  /** Генерация инстанса [[MJdRuntime]] из исходных данных.
    * Ресурсоёмкая операция, поэтому лучше вызывать только при сильной необходимости.
    *
    * @param tpls Шаблоны
    * @param jdConf Конфиг рендера.
    * @return Инстанс [[MJdRuntime]].
    */
  def make(
            tpls    : Seq[Tree[JdTag]],
            jdConf  : MJdConf,
          ): MJdRuntime = {
    val jdtWideSzMults = GridCalc.wideSzMults(tpls, jdConf)
    MJdRuntime(
      jdCss = JdCss( MJdCssArgs(
        templates       = tpls,
        conf            = jdConf,
        jdtWideSzMults  = jdtWideSzMults,
      )),
      jdtWideSzMults = jdtWideSzMults,
    )
  }

  @inline implicit def univEq: UnivEq[MJdRuntime] = UnivEq.derive

  val qdBlockLess = GenLens[MJdRuntime](_.qdBlockLess)

}


/** Контейнер рантаймовых данных jd-рендера.
  *
  * @param jdCss Отрендеренный css.
  * @param jdtWideSzMults Ассоц.массив информации wideSzMult'ов по jd-тегам.
  *                       Появился для возможности увеличения wide-блоков без влияния на остальную плитку.
  * @param qdBlockLess Состояния безблоковых qd-тегов с динамическими размерами в плитке.
  *                    Оно заполняется асинхронно через callback'и из react-measure и др.
  */
case class MJdRuntime(
                       jdCss            : JdCss,
                       jdtWideSzMults   : Map[JdTag, MSzMult],
                       qdBlockLess      : Map[JdTag, MSize2di]    = Map.empty,
                     )
