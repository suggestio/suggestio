package io.suggest.ad.edit.m

import diode.FastEq
import io.suggest.jd.render.m.MJdArgs
import io.suggest.jd.tags.IDocTag

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:33
  * Description: Модель состояния документа в редакторе.
  */
object MDocS {

  /** Реализация FastEq для инстансов [[MDocS]]. */
  implicit object MDocSFastEq extends FastEq[MDocS] {
    override def eqv(a: MDocS, b: MDocS): Boolean = {
      (a.jdArgs eq b.jdArgs) &&
        (a.selectedTag eq b.selectedTag)
    }
  }

}


/** Класс модели состояния работы с документом.
  *
  * @param jdArgs Текущий набор данных для рендера шаблона.
  * @param selectedTag Текущий выделенный элемент, с которым происходит взаимодействие юзера.
  */
case class MDocS(
                  jdArgs        : MJdArgs,
                  selectedTag   : Option[IDocTag]   = None
                ) {

  def withJdArgs(jdArgs: MJdArgs) = copy(jdArgs = jdArgs)

}
