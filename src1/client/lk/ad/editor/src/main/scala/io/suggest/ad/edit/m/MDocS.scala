package io.suggest.ad.edit.m

import com.quilljs.delta.Delta
import diode.FastEq
import io.suggest.jd.render.m.MJdArgs

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
        (a.qDelta eq b.qDelta)
    }
  }

}


/** Класс модели состояния работы с документом.
  *
  * @param jdArgs Текущий набор данных для рендера шаблона.
  * @param qDelta Текущая Delta редактируемного текста в quill-редакторе.
  */
case class MDocS(
                  jdArgs        : MJdArgs,
                  qDelta        : Option[Delta] = None
                ) {

  def withJdArgs(jdArgs: MJdArgs) = copy(jdArgs = jdArgs)
  def withQDelta(qDelta: Option[Delta]) = copy(qDelta = qDelta)

}
