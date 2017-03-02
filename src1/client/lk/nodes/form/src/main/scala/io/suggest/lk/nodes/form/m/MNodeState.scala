package io.suggest.lk.nodes.form.m

import diode.data.Pot
import io.suggest.lk.nodes.ILknTreeNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.03.17 16:36
  * Description: Модель рантаймового состояния одного узла в списке узлов дерева.
  */


/** Класс модели рантаймового состояния одного узла в списке узлов.
  *
  * @param info Данные по узлу, присланные сервером.
  * @param children Состояния дочерних элементов в натуральном порядке.
  *                 Элементы запрашиваются с сервера по мере необходимости.
  */
case class MNodeState(
                       info     : ILknTreeNode,
                       children : Pot[Seq[MNodeState]] = Pot.empty
                     ) {

  def withChildren(children2: Pot[Seq[MNodeState]]) = copy(children = children2)

}
