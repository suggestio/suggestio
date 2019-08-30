package io.suggest.ad.edit.m.vld

import diode.FastEq
import io.suggest.ad.edit.m.pop.MAePopupsS
import io.suggest.jd.render.m.MJdDataJs
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.10.17 14:38
  * Description: Мгновенная модель для вызова [[io.suggest.ad.edit.c.JdVldAh]], состоит из некоторых частей MAeRoot.
  */
object MJdVldAh {

  /** Поддержка FastEq для инстансов [[MJdVldAh]]. */
  implicit object MJdVldAhFastEq extends FastEq[MJdVldAh] {
    override def eqv(a: MJdVldAh, b: MJdVldAh): Boolean = {
      (a.jdData ===* b.jdData) &&
      (a.popups ===* b.popups)
    }
  }

  @inline implicit def univEq: UnivEq[MJdVldAh] = UnivEq.derive

}


/** Класс модели-контейнера данных для контроллера валидации.
  *
  * @param jdData Шаблон и эджи документа.
  * @param popups Доступ к попапам (чтобы можно было отрендерить ошибку).
  */
case class MJdVldAh(
                     jdData     : MJdDataJs,
                     popups     : MAePopupsS,
                   )
