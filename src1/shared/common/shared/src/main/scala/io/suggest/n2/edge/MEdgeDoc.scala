package io.suggest.n2.edge

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.util.SioConstants

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 13:50
  * Description: Модель-контейнер для хранения какой-то документарной вещи внутри эджа.
  *
  * Это неявно-пустая модель и КЛИЕНТ-серверная целиком.
  */
object MEdgeDoc
  extends IEmpty
  with IEsMappingProps
{

  override type T = MEdgeDoc

  /** Частоиспользуемый (на сервере) инстанс модели-пустышки. */
  override val empty = MEdgeDoc()


  /** Список имён полей модели [[MEdgeDoc]]. */
  object Fields {

    val UID_FN = "i"

    val TEXT_FN = "t"

  }

  /** Поддержка play-json. */
  implicit val edgeDocJson: OFormat[MEdgeDoc] = (
    (__ \ Fields.UID_FN).formatNullable[EdgeUid_t] and
    (__ \ Fields.TEXT_FN).formatNullable[Seq[String]]
      .inmap[Seq[String]](
        { _.getOrElse(Nil) },
        { texts => if (texts.isEmpty) None else Some(texts) }
      )
  )(apply, unlift(unapply))


  val uid = GenLens[MEdgeDoc](_.uid)
  val text = GenLens[MEdgeDoc](_.text)

  @inline implicit def univEq: UnivEq[MEdgeDoc] = UnivEq.derive

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.UID_FN -> FNumber(
        typ = DocFieldTypes.Integer,
        index = someFalse,
      ),
      // Текст следует индексировать по-нормальному. Потом в будущем схема индексации неизбежно будет расширена.
      F.TEXT_FN -> FText(
        index = someTrue,
        // Скопипасчено с MNode._all. Начиная с ES-6.0, поле _all покидает нас, поэтому тут свой индекс.
        analyzer = Some(SioConstants.ENGRAM_AN_1),
        searchAnalyzer = Some(SioConstants.DFLT_AN),
      ),
    )
  }

}


/** Контейнер данных ресурсов документа.
  *
  * @param uid уникальный id среди эджей, чтобы можно было находить поле среди других полей.
  * @param text Строки текста, хранимые и нормально индексирумые на сервере.
  */
case class MEdgeDoc(
                     uid    : Option[EdgeUid_t]   = None,
                     text   : Seq[String]         = Nil,
                   )
  extends EmptyProduct
{

  override def toString: String = {
    val sb = new StringBuilder(64)
    for (u <- uid)
      sb.append('#')
        .append(u)
        .append(',')

    if (text.nonEmpty) {
      val isMany = text.lengthCompare(1) > 0
      if (isMany)
        sb.append('[')
      for (t <- text) {
        sb.append(t)
          .append(", ")
      }
      if (isMany)
        sb.append(']')
    }

    sb.toString()
  }

}

