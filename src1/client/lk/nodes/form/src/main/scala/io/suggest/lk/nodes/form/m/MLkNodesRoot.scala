package io.suggest.lk.nodes.form.m

import diode.FastEq
import io.suggest.lk.nodes.MLknConf

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 16:11
  * Description: Корневая circuit-модель react-формы управления под-узлами.
  */
object MLkNodesRoot {

  implicit object MLknRootFastEq extends FastEq[MLkNodesRoot] {
    override def eqv(a: MLkNodesRoot, b: MLkNodesRoot): Boolean = {
      (a.tree eq b.tree) &&
        (a.popups eq b.popups)
        //&& (a.conf eq b.conf)  // Закомменчено, ибо конфиг не изменяется во время работы.
    }
  }

}


/**
  * Класс корневой модели формы управления под-узлами.
  * @param tree Модель-контейнер для отображаемых поддеревьев узлов.
  * @param conf Конфиг формы с сервера. Не должен изменяться самой формой.
  * @param popups Состояния попапов формы.
  */
case class MLkNodesRoot(
                         conf       : MLknConf,
                         tree       : MTree,
                         popups     : MLknPopups            = MLknPopups.empty
                       )
{

  def withTree(tree2: MTree) = copy(tree = tree2)
  def withPopups(popups2: MLknPopups) = copy(popups = popups2)

}
