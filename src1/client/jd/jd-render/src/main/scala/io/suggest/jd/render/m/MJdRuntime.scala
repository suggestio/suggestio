package io.suggest.jd.render.m

import diode.FastEq
import diode.data.Pot
import io.suggest.common.geom.d2.MSize2di
import io.suggest.dev.MSzMult
import io.suggest.jd.MJdTagId
import io.suggest.jd.render.v.JdCss
import io.suggest.jd.tags.JdTag
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

import scala.collection.immutable.HashMap

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
      //(a.jdCss ===* b.jdCss) &&
      //(a.jdtWideSzMults ===* b.jdtWideSzMults) &&
      (a.qdBlockLess ===* b.qdBlockLess) //&&
      //(a.jdTagsById ===* b.jdTagsById)
    }
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
  *                    Только HashMap, чтобы гарантировать быстрое добавление новых элементов в массив.
  * @param jdTagsById Теги по ключу. Для связывания стабильных названий стилей в JdCss с JdR.
  */
case class MJdRuntime(
                       jdCss              : JdCss,
                     // TODO Заменить ключи: JdTag на MJdTagId.
                       jdtWideSzMults     : HashMap[JdTag, MSzMult],
                       qdBlockLess        : HashMap[JdTag, Pot[MSize2di]]     = HashMap.empty,
                       jdTagsById         : HashMap[MJdTagId, JdTag]          = HashMap.empty,
                     )
