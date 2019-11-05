package io.suggest.jd.render.m

import diode.data.Pot
import io.suggest.dev.MSzMult
import io.suggest.jd.MJdTagId
import io.suggest.jd.tags.JdTag
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import monocle.macros.GenLens

import scala.collection.immutable.HashMap

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.10.2019 18:16
  * Description: Контейнер ратаймовых jd-данных, пошаренных между JdCss и общим MJdRuntime.
  */

object MJdRuntimeData {

  // Без FastEq, т.к. данные модели мега-волатильные на любой чих.

  val qdBlockLess = GenLens[MJdRuntimeData](_.qdBlockLess)

  @inline implicit def univEq: UnivEq[MJdRuntimeData] = UnivEq.derive

}


/** @param jdtWideSzMults Ассоц.массив информации wideSzMult'ов по jdId.
  *                       Появился для возможности увеличения wide-блоков без влияния на остальную плитку.
  * @param qdBlockLess Состояния безблоковых qd-тегов с динамическими размерами в плитке.
  *                    Оно заполняется асинхронно через callback'и из react-measure и др.
  *                    Только HashMap, чтобы гарантировать быстрое добавление новых элементов в массив.
  *                    Используется MJdTagId: прямая адресация по JdTag создаёт лютый гемор на уровне кода,
  *                    т.к. react-measure асинхронен.
  *                    Pot.pending() означает, что запрошен принудительный вызов measure()-функции.
  *                    pending() нельзя выставлять до первого рендера, т.к. вызов measure() будет фейлить.
  * @param jdTagsById Теги по ключу. Для связывания стабильных названий стилей в JdCss с JdR.
  */
case class MJdRuntimeData(
                           jdtWideSzMults     : HashMap[MJdTagId, MSzMult],
                           qdBlockLess        : HashMap[MJdTagId, Pot[MQdBlSize]],
                           jdTagsById         : HashMap[MJdTagId, JdTag],
                         )
