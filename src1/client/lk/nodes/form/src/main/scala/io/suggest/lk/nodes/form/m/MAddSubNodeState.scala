package io.suggest.lk.nodes.form.m

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.03.17 18:05
  * Description: Модель состояния данных добавляемого узла.
  * @param name Название узла, задаётся юзером.
  * @param id Заданный id-узла. Для маячков, в первую очередь.
  */
case class MAddSubNodeState(
                            name    : String          = "",
                            id      : Option[String]  = None
                           ) {

  def withName(name2: String) = copy(name = name2)
  def withId(id2: Option[String]) = copy(id = id2)

}
