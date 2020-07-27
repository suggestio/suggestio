package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.common.empty.EmptyProduct
import io.suggest.scalaz.NodePath_t
import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.09.17 18:24
  * Description: Модель состояния таскания jd-тегов.
  */
object MJdDndS {

  val empty = apply()

  /** Поддержка FastEq для интсансов [[MJdDndS]]. */
  implicit object MJdDndSFastEq extends FastEq[MJdDndS] {
    override def eqv(a: MJdDndS, b: MJdDndS): Boolean = {
      a.jdt ===* b.jdt
    }
  }

  @inline implicit def univEq: UnivEq[MJdDndS] = UnivEq.derive

  def jdt = GenLens[MJdDndS](_.jdt)

}


/** Класс модели состояния Drag-n-Drop, которая влияет на рендер.
  * Часть данных пропихивается через event.dataTransfer, а не здесь.
  *
  * Модель неявно-пустая, т.к. DnD может начинаться за пределами текущей страницы.
  *
  * @param jdt Инстанс docTag'а. Тут два варианта:
  *            - Если DnD внутри страницы, то это инстанс IDocTag из текущего документа.
  *            - ?? Если DnD извне, то тут будет десериализованный инстанс ??
  */
case class MJdDndS(
                    jdt             : Option[NodePath_t]  = None
                  )
  extends EmptyProduct

