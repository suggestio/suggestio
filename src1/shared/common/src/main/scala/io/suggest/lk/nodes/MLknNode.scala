package io.suggest.lk.nodes

import boopickle.Default._
import io.suggest.primo.id.IId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 14:57
  * Description: Кросс-платформенная модель данных по одному узлу для формы управления узлами (маячками).
  */

object MLknNode {

  /** Поддержка сериализации/десериализации. */
  implicit val lknNodePickler: Pickler[MLknNode] = {
    generatePickler[MLknNode]
  }

}


/**
  * Класс модели данных по узлу.
  * По идее, это единственая реализация [[MLknNode]].
  */
case class MLknNode(
                     /** Уникальный id узла. */
                     override val id        : String,
                     /** Отображаемое название узла. */
                     name                   : String,
                     /** Код типа узла по модели MNodeTypes. */
                     ntypeId                : String,
                     /** Является ли узел активным сейчас? */
                     isEnabled              : Boolean,
                     /** Опциональные данные текущей видимости узла для других пользователей s.io.
                       * None значит, что сервер не определял это значение.
                       */
                     //isPublic             : Boolean,
                     /** Может ли юзер управлять значением флага isEnabled или удалять узел?
                       * None значит, что сервер не интересовался этим вопросом.
                       */
                     canChangeAvailability  : Option[Boolean],

                     /** Имеется ли размещение текущей рекламной карточки на указанном узле? */
                     hasAdv                 : Option[Boolean]
                   )
  extends IId[String]
