package io.suggest.jd

import io.suggest.jd.tags.JdTag
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.primo.id.IId
import io.suggest.scalaz.ZTreeUtil.ZTREE_FORMAT
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 17:40
  * Description: Модель данных рекламной карточки для рендера её на клиенте.
  * Также используется редактором карточек в обратном направлении для сабмита карточки.
  */
object MJdAdData {

  object Fields {
    val TEMPATE_FN = "t"
    val EDGES_FN   = "e"
    val NODE_ID_FN = "i"
  }

  /** Поддержка play-json. */
  implicit def MAD_EDIT_FORM_FORMAT: OFormat[MJdAdData] = (
    (__ \ Fields.TEMPATE_FN).format[Tree[JdTag]] and
    // Массив эджей без Nullable, т.к. это очень маловероятная ситуация слишком пустой карточки.
    (__ \ Fields.EDGES_FN).format[Iterable[MJdEdge]] and
    (__ \ Fields.NODE_ID_FN).formatNullable[String]
  )(apply, unlift(unapply))


  implicit def univEq: UnivEq[MJdAdData] = {
    import io.suggest.scalaz.ZTreeUtil.zTreeUnivEq
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}


/** Класс контейнера данных формы редактирования карточки.
  * Именно этот класс является основным языком связи js-клиента и jvm-сервера.
  *
  * @param nodeId id узла этой карточки. Может и отсутствовать.
  * @param template Шаблон документа.
  * @param edges Эджи с данными для рендера документа.
  */
case class MJdAdData(
                      template    : Tree[JdTag],
                      edges       : Iterable[MJdEdge],
                      nodeId      : Option[String]
                    ) {

  /** Кэшируемая карта эджей. */
  lazy val edgesMap = IId.els2idMap[EdgeUid_t, MJdEdge]( edges )

}
