package io.suggest.jd.tags

import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.17 12:19
  * Description: Интерфейс каждого элемента структуры документа.
  * Структура аналогична html/xml-тегам, но завязана на JSON и названа структурой, чтобы не путаться.
  */
object IDocTag {

  object Fields {

    val TYPE_FN = "t"

    /** Имя поля с дочерними элементами. По идее -- оно одно на все реализации. */
    val CHILDREN_FN = "c"

  }


  private val _IDT_NAME_FORMAT = (__ \ Fields.TYPE_FN).format[MDtName]

  /** Приведение Reads[Реализация IDocTag] к Reads[IDocTag]. Компилятор не хочет этого делать сам. */
  private def _toIdtReads[X <: IDocTag](implicit rx: Reads[X]): Reads[IDocTag] = {
    // TODO .map(d => d: IDocTag) - это небесплатный костыль в коде. Разруливаем через asInstanceOf[], что суть есть zero-cost костыле-хак. Надо как-то нормально это разрулить.
    rx.asInstanceOf[Reads[IDocTag]] //.map(d => d: IDocTag)
  }

  /** Отрендерить в JsObject. Это аналог Json.writes[X].writes(x) */
  private def _writeJsObj[X <: IDocTag](x: X)(implicit ow: OWrites[X]): JsObject = {
    ow.writes(x)
  }

  /** Полиморфная поддержка play-json. */
  implicit val IDOC_TAG_FORMAT: OFormat[IDocTag] = {
    // Собрать читалку на основе прочитанного имени тега.
    val r: Reads[IDocTag] = _IDT_NAME_FORMAT.flatMap[IDocTag] {
      case MJdTagNames.PlainPayload  => _toIdtReads[PlainPayload]
      case MJdTagNames.Picture       => _toIdtReads[Picture]
      case MJdTagNames.AbsPos        => _toIdtReads[AbsPos]
      case MJdTagNames.Strip         => _toIdtReads[Strip]
      case MJdTagNames.Document      => _toIdtReads[JsonDocument]
      case _ => ???
    }

    // Собрать в-JSON-рендерер на основе названия тега.
    // TODO Writes указаны явно, потому что компилятор цепляет везде IDOC_TAG_FORMAT вместо нужного типа из-за Writes[-A].
    val w = OWrites[IDocTag] { docTag =>
      val dsTypeField = _IDT_NAME_FORMAT.writes( docTag.dtName )
        .value
        .head
      val dataJsObj = docTag match {
        case pp: PlainPayload     => _writeJsObj(pp)(PlainPayload.PLAIN_PAYLOAD_FORMAT)
        case p: Picture           => _writeJsObj(p)(Picture.PICTURE_FORMAT)
        case ap: AbsPos           => _writeJsObj(ap)(AbsPos.ABS_POS_FORMAT)
        case s: Strip             => _writeJsObj(s)(Strip.STRIP_FORMAT)
        case d: JsonDocument          => _writeJsObj(d)(JsonDocument.DOCUMENT_FORMAT)
        case _ => ???
      }
      // Добавить информацию по типу в уже отрендеренный JSON.
      dataJsObj.copy(
        underlying = dataJsObj.value + dsTypeField
      )
    }
    OFormat(r, w)
  }


  /** Поддержка play-json для поля children. */
  val CHILDREN_IDOC_TAG_FORMAT: OFormat[Seq[IDocTag]] = {
    (__ \ Fields.CHILDREN_FN).lazyFormatNullable( implicitly[Format[Seq[IDocTag]]] )
      .inmap[Seq[IDocTag]](
        {tagsOpt =>
          tagsOpt.fold[Seq[IDocTag]](Nil)(identity)
        },
        {tags =>
          if (tags.isEmpty) None else Some(tags)
        }
      )
  }


}


/** Интерфейс для всех "тегов" структуры документа. */
trait IDocTag {

  /** Название тега. */
  def dtName: MDtName

  /** Дочерние теги. */
  def children: Seq[IDocTag]

}
