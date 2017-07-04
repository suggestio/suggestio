package io.suggest.stat.m

import io.suggest.es.model.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 17:35
  * Description: Суб-модель экшенов статистики.
  * Была выдана путём унификации полей исходной sc-статистики наподобии успешной модели MEdge.
  *
  * Из возможных проблем: kibana vs nested objects: [[https://github.com/elastic/kibana/issues/1084]]
  * Как вариант, отказ от Nested object (со всеми вытекающими) или использовать форк kibana с поддержкой nested.
  */
object MAction extends IGenEsMappingProps {

  object Fields {

    /** Действие, которого касается статистика. */
    val ACTION_FN     = "action"

    /** Имя поля с id затронутых узлов. */
    val NODE_ID_FN    = "nodeId"

    /** Имя поля с человеко-читаемыми именами узлов. */
    val NODE_NAME_FN  = "nodeName"

    /** Имя индексируемого поля с каким-то количеством.
      * Узлов, например или чего-то ещё в зависимости от экшена. */
    val COUNT_FN      = "count"

    /** Какая-то доп.строка текста. По начальной задумке - не индексируется вообще никак. */
    val TEXT_NI_FN    = "text_ni"

  }


  import Fields._

  implicit val FORMAT: OFormat[MAction] = (
    (__ \ ACTION_FN).formatNullable[Seq[MActionType]]
      .inmap [Seq[MActionType]] (
        { _.getOrElse(Nil) },
        { ss  => if (ss.isEmpty) None else Some(ss) }
    ) and
    (__ \ NODE_ID_FN).formatNullable[Seq[String]]
      .inmap [Seq[String]] (
        { _.getOrElse(Nil) },
        { ids => if (ids.isEmpty) None else Some(ids) }
      ) and
    (__ \ NODE_NAME_FN).formatNullable[Seq[String]]
      .inmap[Seq[String]](
        { _.getOrElse(Nil) },
        { names => if (names.isEmpty) None else Some(names) }
      ) and
    (__ \ COUNT_FN).formatNullable[Seq[Int]]
      .inmap [Seq[Int]] (
        { _.getOrElse(Nil) },
        { counts => if (counts.isEmpty) None else Some(counts) }
      ) and
    (__ \ TEXT_NI_FN).formatNullable[Seq[String]]
      .inmap [Seq[String]] (
        { _.getOrElse(Nil) },
        { texts => if (texts.isEmpty) None else Some(texts) }
      )
  )(apply, unlift(unapply))


  import io.suggest.es.util.SioEsUtil._

  private def _strField(id: String) = {
    FieldKeyword(id, index = true, include_in_all = true)
  }

  override def generateMappingProps: List[DocField] = {
    List(
      _strField(ACTION_FN),
      _strField(NODE_ID_FN),
      _strField(NODE_NAME_FN),
      FieldNumber(COUNT_FN, fieldType = DocFieldTypes.integer, index = true, include_in_all = true),
      FieldText(TEXT_NI_FN, index = false, include_in_all = false)
    )
  }

}


case class MAction(
  actions   : Seq[MActionType],
  nodeId    : Seq[String]       = Nil,
  nodeName  : Seq[String]       = Nil,
  count     : Seq[Int]          = Nil,
  textNi    : Seq[String]       = Nil
)
