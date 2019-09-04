package io.suggest.jd

import io.suggest.primo.id.OptId
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 17:40
  * Description: Модель данных рекламной карточки для рендера её на клиенте.
  * Также используется редактором карточек в обратном направлении для сабмита карточки.
  */
object MJdData {

  object Fields {
    val DOC_FN      = "d"
    val EDGES_FN    = "e"
  }

  /** Поддержка play-json. */
  implicit def jdDataJson: OFormat[MJdData] = (
    (__ \ Fields.DOC_FN).format[MJdDoc] and
    // Массив эджей без Nullable, т.к. это очень маловероятная ситуация слишком пустой карточки.
    (__ \ Fields.EDGES_FN).format[Iterable[MJdEdge]]
  )(apply, unlift(unapply))


  @inline implicit def univEq: UnivEq[MJdData] = {
    import io.suggest.scalaz.ZTreeUtil.zTreeUnivEq
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  val doc       = GenLens[MJdData](_.doc)
  val edges     = GenLens[MJdData](_.edges)

}


/** Класс контейнера данных формы редактирования карточки.
  * Именно этот класс является основным языком связи js-клиента и jvm-сервера.
  *
  * @param doc Данные документа.
  * @param edges Эджи с данными для рендера документа.
  */
final case class MJdData(
                          doc         : MJdDoc,
                          edges       : Iterable[MJdEdge],
                        )
  extends OptId[String]
{

  // TODO Удалить OptId отсюда.
  override def id = doc.jdId.nodeId

}
