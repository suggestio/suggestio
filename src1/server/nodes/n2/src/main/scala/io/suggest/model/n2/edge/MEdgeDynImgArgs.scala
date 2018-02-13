package io.suggest.model.n2.edge

import io.suggest.es.model.IGenEsMappingProps
import io.suggest.es.util.SioEsUtil._
import io.suggest.img.{MImgFmt, MImgFmts}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.02.18 16:10
  * Description: Неявно-пустая модель с инфой по связанной dyn-картинке.
  * Похожа на MDynImgId, но очень упрощена и более сериализована местами:
  * Поле dynOpsStr - сериализованная строка, а не Seq[ImOp].
  */
object MEdgeDynImgArgs extends IGenEsMappingProps {

  /** Названия ES-полей. */
  object Fields {
    val FORMAT_FN  = "f"
    val DYN_OPS_FN = "o"
  }


  /** Поддержка play-json. */
  implicit val MDYN_IMG_ARGS_FORMAT: OFormat[MEdgeDynImgArgs] = {
    // Штатный сериализатор и десериализатор.
    val fmt0 = (
      (__ \ Fields.FORMAT_FN).format[MImgFmt] and
      (__ \ Fields.DYN_OPS_FN).formatNullable[String]
    )(apply, unlift(unapply))

    // Изначально, данные по картинки хранились в edge.info в виде строки. Но её стало мало из-за формата.
    // TODO Можно удалить после resaveMany() для всех узлов.
    val fallbackReads = (fmt0: Reads[MEdgeDynImgArgs])
      .orElse {
        for {
          dynOpsStr <- implicitly[Reads[String]]
        } yield {
          MEdgeDynImgArgs(
            dynFormat = MImgFmts.JPEG,
            dynOpsStr = Some(dynOpsStr)
          )
        }
      }

    OFormat(fallbackReads, fmt0)
  }


  override def generateMappingProps: List[DocField] = {
    List(
      FieldKeyword(id = Fields.FORMAT_FN, index = true, include_in_all = false),
      FieldText(id = Fields.DYN_OPS_FN, index = false, include_in_all = false)
    )
  }

  implicit def univEq: UnivEq[MEdgeDynImgArgs] = UnivEq.derive

}


/** Класс-контейнер данных по сборке динамической картинки внутри эджа.
  *
  * @param dynFormat Ожидаемый формат картинки на выходе. Обычно JPEG.
  * @param dynOpsStr Дополнительные опции рендера, если есть.
  */
final case class MEdgeDynImgArgs(
                                  dynFormat : MImgFmt,
                                  dynOpsStr : Option[String]    = None
                                )

