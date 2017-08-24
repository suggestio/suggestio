package io.suggest.jd

import io.suggest.model.n2.edge.{EdgeUid_t, MPredicate}
import io.suggest.primo.id.IId
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 17:41
  * Description: Модель инфа по эджу для редактора карточек.
  * Модель является кросс-платформенной, но на сервере существует лишь на входе и выходе из jdoc-редакторов.
  *
  * Модель повторяет своей структурой MEdge, но не содержит нерелевантных для jd-редактора полей.
  */
object MJdEditEdge {

  /** Названия полей модели для сериализации в JSON. */
  object Fields {
    val PREDICATE_FN    = "p"
    val UID_FN          = "i"
    val TEXT_FN         = "t"
    val NODE_ID_FN      = "n"
    val URL_FN          = "u"
  }

  /** Поддержка play-json между клиентом и сервером. */
  implicit val MAD_EDIT_EDGE_FORMAT: OFormat[MJdEditEdge] = (
    (__ \ Fields.PREDICATE_FN).format[MPredicate] and
    (__ \ Fields.UID_FN).format[EdgeUid_t] and
    (__ \ Fields.TEXT_FN).formatNullable[String] and
    (__ \ Fields.NODE_ID_FN).formatNullable[String] and
    (__ \ Fields.URL_FN).formatNullable[String]
  )(apply, unlift(unapply))

}


/** Данные по эджу для редактируемого документа.
  *
  * @param predicate Предикат.
  * @param nodeId id узла.
  * @param url Ссылка на ресурс, на картинку, например.
  */
case class MJdEditEdge(
                        predicate           : MPredicate,
                        override val id     : EdgeUid_t,
                        text                : Option[String] = None,
                        nodeId              : Option[String] = None,
                        url                 : Option[String] = None
                      )
  extends IId[EdgeUid_t]
