package io.suggest.lk.nodes.form.m

import diode.FastEq
import io.suggest.lk.nodes.MLknForm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 16:11
  * Description: Корневая circuit-модель react-формы управления под-узлами.
  */
object MLkNodesRoot {

  object MLknRootFastEq extends FastEq[MLkNodesRoot] {
    override def eqv(a: MLkNodesRoot, b: MLkNodesRoot): Boolean = {
      a.tree eq b.tree
    }
  }

}


/**
  * Класс корневой модели формы управления под-узлами.
  * @param tree Модель-контейнер для отображаемых поддеревьев узлов.
  */
case class MLkNodesRoot(
                         tree       : MTree
                       )
{

  def withTree(tree2: MTree) = copy(tree = tree2)


  /** Извлечь данные из разных моделей в инстанс, пригодный для сабмита. */
  def toForm: MLknForm = ???

}
