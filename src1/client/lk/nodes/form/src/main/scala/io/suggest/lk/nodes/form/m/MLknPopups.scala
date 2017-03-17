package io.suggest.lk.nodes.form.m

import diode.FastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 22:27
  * Description: Модель состояния подсистемы попапов react-формы LkNodes.
  *
  */
object MLknPopups {

  def empty = MLknPopups()

  implicit object MLknPopupsFastEq extends FastEq[MLknPopups] {
    override def eqv(a: MLknPopups, b: MLknPopups): Boolean = {
      a.createNodeS eq b.createNodeS
    }
  }

}


/** Класс модели состояния попапов формы управления узлами.
  * Неявно-пустая модель.
  *
  * @param createNodeS Состояние попапа добавления узла, если есть.
  */
case class MLknPopups(
                       createNodeS   : Option[MCreateNodeS]    = None
                     ) {

  def hasOpenedPopups = createNodeS.nonEmpty

}
