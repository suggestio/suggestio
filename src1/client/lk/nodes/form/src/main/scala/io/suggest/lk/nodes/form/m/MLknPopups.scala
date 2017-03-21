package io.suggest.lk.nodes.form.m

import diode.FastEq
import io.suggest.common.empty.EmptyProduct

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 22:27
  * Description: Модель состояния подсистемы попапов react-формы LkNodes.
  */
object MLknPopups {

  def empty = MLknPopups()

  implicit object MLknPopupsFastEq extends FastEq[MLknPopups] {
    override def eqv(a: MLknPopups, b: MLknPopups): Boolean = {
      (a.createNodeS eq b.createNodeS) &&
        (a.deleteNodeS eq b.deleteNodeS) &&
        (a.editTfDailyS eq b.editTfDailyS)
    }
  }

}


/** Класс модели состояния попапов формы управления узлами.
  * Неявно-пустая модель.
  *
  * @param createNodeS Состояние попапа добавления узла, если есть.
  * @param deleteNodeS Состояние попапа удаления узла, если есть.
  */
case class MLknPopups(
                       createNodeS   : Option[MCreateNodeS]    = None,
                       deleteNodeS   : Option[MDeleteNodeS]    = None,
                       editTfDailyS  : Option[MEditTfDailyS]   = None
                     )
  extends EmptyProduct
{

  def withCreateNodeState(cns2: Option[MCreateNodeS]) = copy(createNodeS = cns2)
  def withDeleteNodeState(dns2: Option[MDeleteNodeS]) = copy(deleteNodeS = dns2)
  def withEditTfDailyState(tds: Option[MEditTfDailyS]) = copy(editTfDailyS = tds)

}
