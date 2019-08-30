package io.suggest.jd

import io.suggest.jd.tags.JdTag
import io.suggest.primo.id.OptId
import io.suggest.scalaz.ZTreeUtil.ZTREE_FORMAT
import japgolly.univeq._
import monocle.macros.GenLens
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
object MJdData {

  object Fields {
    val TEMPATE_FN = "t"
    val EDGES_FN   = "e"
    val NODE_ID_FN = "i"
  }

  /** Поддержка play-json. */
  implicit def jdDataJson: OFormat[MJdData] = (
    (__ \ Fields.TEMPATE_FN).format[Tree[JdTag]] and
    // Массив эджей без Nullable, т.к. это очень маловероятная ситуация слишком пустой карточки.
    (__ \ Fields.EDGES_FN).format[Iterable[MJdEdge]] and
    (__ \ Fields.NODE_ID_FN).formatNullable[String]
  )(apply, unlift(unapply))


  @inline implicit def univEq: UnivEq[MJdData] = {
    import io.suggest.scalaz.ZTreeUtil.zTreeUnivEq
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  val template = GenLens[MJdData](_.template)
  val edges    = GenLens[MJdData](_.edges)
  val nodeId   = GenLens[MJdData](_.nodeId)

}


/** Класс контейнера данных формы редактирования карточки.
  * Именно этот класс является основным языком связи js-клиента и jvm-сервера.
  *
  * @param nodeId id узла этой карточки. Может и отсутствовать.
  * @param template Шаблон документа.
  * @param edges Эджи с данными для рендера документа.
  */
final case class MJdData(
                          template    : Tree[JdTag],
                          edges       : Iterable[MJdEdge],
                          nodeId      : Option[String]
                        )
  extends OptId[String]
{

  override def id = nodeId

}
