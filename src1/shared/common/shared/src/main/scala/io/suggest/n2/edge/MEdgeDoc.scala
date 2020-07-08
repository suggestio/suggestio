package io.suggest.n2.edge

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.primo.id.OptId
import io.suggest.text.StringUtil
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
  override lazy val empty = MEdgeDoc()


  /** Список имён полей модели [[MEdgeDoc]]. */
  object Fields {

    val UID_FN = "i"

    val TEXT_FN = "t"

  }

  /** Поддержка play-json. */
  implicit val edgeDocJson: OFormat[MEdgeDoc] = {
    (
      (__ \ Fields.UID_FN).formatNullable[EdgeUid_t] and {
        val textPath = (__ \ Fields.TEXT_FN)
        // TODO После resaveMany() заменить это всё на (__ \ Fields.TEXT_FN).formatNullable[String]
        // До 2020-03-05 тут был массив строк ["ads"] с одной строкой внутри.
        val textReadsCompat = textPath
          .read[String]
          .map( Option.apply )
          .orElse {
            textPath
              .readNullable[Seq[String]]
              .map( _.flatMap(_.headOption) )
          }
        val textWrites = textPath.writeNullable[String]
        OFormat( textReadsCompat, textWrites )
      }
    )(apply, unlift(unapply))
  }


  def id = GenLens[MEdgeDoc](_.id)
  def text = GenLens[MEdgeDoc](_.text)

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
  * @param id уникальный id среди эджей, чтобы можно было находить поле среди других полей.
  * @param text Строки текста, хранимые и нормально индексирумые на сервере.
  */
final case class MEdgeDoc(
                           override val id    : Option[EdgeUid_t]     = None,
                           text               : Option[String]        = None,
                         )
  extends EmptyProduct
  with OptId[EdgeUid_t]
{

  override def toString: String = {
    if (isEmpty) {
      ""
    } else {
      val sb = new StringBuilder(36, "D")

      for (edgeUid <- id)
        sb.append('#').append(edgeUid)

      for (txt <- text) {
        sb.append('(')
          .append( StringUtil.strLimitLen(txt, 32) )
          .append(')')
      }

      sb.toString()
    }
  }

}

