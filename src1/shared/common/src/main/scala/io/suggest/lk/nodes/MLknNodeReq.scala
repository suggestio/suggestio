package io.suggest.lk.nodes

import boopickle.Default._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.17 18:49
  * Description: Модель тела запроса создания/редактирования узла в списке узлов.
  */
object MLknNodeReq {

  /** BooPickler для инстансов модели. */
  implicit val mLknNodeReqPickler: Pickler[MLknNodeReq] = {
    generatePickler[MLknNodeReq]
  }

}


/**
  * Класс модели реквеста добавления узла.
  *
  * @param name Имя узла.
  * @param id Идентификатор узла, если задан.
  * @param parentId Новый id родительского узла, если необходимо.
  */
case class MLknNodeReq(
                        name     : String,
                        id       : Option[String],
                        parentId : Option[String] = None
                      )
