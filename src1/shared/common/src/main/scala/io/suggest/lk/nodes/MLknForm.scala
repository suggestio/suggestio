package io.suggest.lk.nodes

import boopickle.Default._
import io.suggest.adv.rcvr.RcvrKey

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 13:50
  * Description: Lk Nodes form -- кросс-платформенная модель состояния формы.
  * Сабмититься на сервер для сохранения данных.
  */
object MLknForm {

  implicit val mLknFormPickler: Pickler[MLknForm] = {
    generatePickler[MLknForm]
  }

}

/** Класс модели submit-данных формы управления узлами
  *
  * @param updates Изменения в узлах, которые нужно произвести на сервере.
  *                Ключ -- цепь id ресиверов относительно текушего узла.
  */
case class MLknForm(
                     updates: Map[RcvrKey, MLknNodeUpdate] = Map.empty
                   ) {

  def withUpdates(updates2: Map[RcvrKey, MLknNodeUpdate]) = copy(updates = updates2)

}


/**
  * Модель изменений для одного узла.
  * Все поля опциональны, и None означает, что менять исходное значение не требуется.
  */
case class MLknNodeUpdate(
                           name      : Option[String]  = None,
                           isPublic  : Option[Boolean] = None
                         ) {

  def withName(name2: Option[String]) = copy(name = name2)
  def withIsPublic(isPublic2: Option[Boolean]) = copy(isPublic = isPublic2)

}