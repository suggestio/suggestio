package io.suggest.ad.edit.m.edit.strip

import diode.FastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.09.17 22:47
  * Description: Модель props-состояния strip-редактора.
  */
object MStripEdS {

  implicit object MEditStripSFastEq extends FastEq[MStripEdS] {
    override def eqv(a: MStripEdS, b: MStripEdS): Boolean = {
      a.confirmDelete == b.confirmDelete
    }
  }

}


/** Класс модели пропертисов редактора блока.
  *
  * @param confirmDelete Отображается подтверждение удаления блока?
  */
case class MStripEdS(
                        confirmDelete: Boolean = false
                      ) {

  def withConfirmDelete(confirmDelete: Boolean) = copy(confirmDelete = confirmDelete)

}
