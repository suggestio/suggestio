package io.suggest.ad.edit.m

import diode.FastEq
import io.suggest.ad.edit.m.pop.MAePopupsS
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:30
  * Description: Корневая модель состояния формы редактора рекламной карточки.
  */
object MAeRoot {

  implicit object MAeRootFastEq extends FastEq[MAeRoot] {
    override def eqv(a: MAeRoot, b: MAeRoot): Boolean = {
      (a.conf ===* b.conf) &&
        (a.doc ===* b.doc) &&
        (a.popups ===* b.popups)
    }
  }

  implicit def univEq: UnivEq[MAeRoot] = UnivEq.derive

}


/** Класс корневой модели состояния редактора рекламной карточки.
  *
  * @param conf Конфиг, присланный сервером.
  * @param doc Состояние редактирования документа. Там почти всё и живёт.
  * @param popups Состояние попапов.
  */
case class MAeRoot(
                    conf        : MAdEditFormConf,
                    doc         : MDocS,
                    popups      : MAePopupsS        = MAePopupsS.empty
                  ) {

  /** Экспорт данных формы. */
  def toForm: MAdEditForm = ???

  def withDoc(doc: MDocS) = copy(doc = doc)
  def withPopups(popups: MAePopupsS) = copy(popups = popups)

}
