package io.suggest.jd

import io.suggest.file.MSrvFileInfo
import io.suggest.model.n2.edge.{EdgeUid_t, MPredicate}
import io.suggest.primo.id.IId
import japgolly.univeq.UnivEq
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
    val SRV_FILE_FN     = "f"
  }

  /** Поддержка play-json между клиентом и сервером. */
  implicit val MAD_EDIT_EDGE_FORMAT: OFormat[MJdEditEdge] = {
    val F = Fields
    (
      (__ \ F.PREDICATE_FN).format[MPredicate] and
      (__ \ F.UID_FN).format[EdgeUid_t] and
      (__ \ F.TEXT_FN).formatNullable[String] and
      (__ \ F.NODE_ID_FN).formatNullable[String] and
      (__ \ F.URL_FN).formatNullable[String] and
      (__ \ F.SRV_FILE_FN).formatNullable[MSrvFileInfo]
    )(apply, unlift(unapply))
  }

  implicit def univEq: UnivEq[MJdEditEdge] = UnivEq.derive

}


/** Данные по эджу для редактируемого документа.
  *
  * @param predicate Предикат.
  * @param nodeId id узла.
  * @param url Ссылка на ресурс, на картинку, например.
  * @param fileSrv КроссПлатформенная инфа по файлу-узлу на стороне сервера.
  */
case class MJdEditEdge(
                        predicate           : MPredicate,   // TODO MPredicate заменить на MPredicates.JdContent.Child или что-то типа него.
                        override val id     : EdgeUid_t,
                        text                : Option[String] = None,
                        nodeId              : Option[String] = None,
                        url                 : Option[String] = None,
                        fileSrv             : Option[MSrvFileInfo]  = None
                      )
  extends IId[EdgeUid_t]
{

  def withNodeId(nodeId: Option[String])  = copy(nodeId = nodeId)
  def withText(text: Option[String])      = copy(text = text)
  def withUrl(url: Option[String])        = copy(url = url)
  def withFileSrv(fileSrv: Option[MSrvFileInfo]) = copy(fileSrv = fileSrv)

  /** Подобрать значение для imgSrc. */
  def imgSrcOpt: Option[String] = {
    fileSrv
      .map(_.url)
      .orElse(url)
  }

}