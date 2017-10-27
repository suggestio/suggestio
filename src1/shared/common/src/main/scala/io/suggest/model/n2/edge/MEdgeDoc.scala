package io.suggest.model.n2.edge

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 13:50
  * Description: Модель-контейнер для хранения какой-то документарной вещи внутри эджа.
  *
  * Это неявно-пустая модель.
  */
object MEdgeDoc extends IEmpty {

  override type T = MEdgeDoc

  /** Частоиспользуемый (на сервере) инстанс модели-пустышки. */
  override val empty = MEdgeDoc()


  /** Список имён полей модели [[MEdgeDoc]]. */
  object Fields {

    val UID_FN = "i"

    val TEXT_FN = "t"

  }


  /** Поддержка play-json. */
  implicit val MEDGE_DOC_FORMAT: OFormat[MEdgeDoc] = (
    (__ \ Fields.UID_FN).formatNullable[Int] and
    (__ \ Fields.TEXT_FN).formatNullable[Seq[String]]
      .inmap[Seq[String]](
        { _.getOrElse(Nil) },
        { texts => if (texts.isEmpty) None else Some(texts) }
      )
  )(apply, unlift(unapply))

}


/** Контейнер данных ресурсов документа.
  *
  * @param uid уникальный id среди эджей, чтобы можно было находить поле среди других полей.
  * @param text Строки текста, хранимые и нормально индексирумые на сервере.
  */
case class MEdgeDoc(
                     uid    : Option[Int]   = None,
                     text   : Seq[String]   = Nil
                   )
  extends EmptyProduct
{
  override def toString: String = {
    val sb = new StringBuilder(64)
    for (u <- uid)
      sb.append("u#")
        .append(u)
        .append(",")

    if (text.nonEmpty) {
      sb.append("t=[")
      for (t <- text) {
        sb.append(t)
          .append(", ")
      }
      sb.append("]")
    }

    sb.toString()
  }
}

