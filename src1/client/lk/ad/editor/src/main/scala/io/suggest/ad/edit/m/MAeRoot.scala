package io.suggest.ad.edit.m

import diode.FastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:30
  * Description: Корневая модель состояния формы редактора рекламной карточки.
  */
object MAeRoot {

  implicit object MAeRootFastEq extends FastEq[MAeRoot] {
    override def eqv(a: MAeRoot, b: MAeRoot): Boolean = {
      (a.conf eq b.conf) &&
        (a.doc eq b.doc)
    }
  }

}


/** Класс корневой модели состояния редактора рекламной карточки.
  *
  * @param conf Конфиг, присланный сервером.
  * @param doc Состояние редактирования документа. Там почти всё и живёт.
  */
case class MAeRoot(
                        conf        : MAdEditFormConf,
                        doc         : MDocS
                      ) {

  /** Экспорт данных формы. */
  def toForm: MAdEditForm = ???

  def withDoc(doc: MDocS) = copy(doc = doc)

}
