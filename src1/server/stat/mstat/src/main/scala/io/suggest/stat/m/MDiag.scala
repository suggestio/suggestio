package io.suggest.stat.m

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.09.16 14:08
  * Description: Неявно-пустая json-модель данных для каких-то диагностических данных с удалённой системы,
  * Изначально: ошибки js выдачи из MRemoteError пробрасывались в MStat с учётом этой модели.
  *
  * В начальном варианте здесь было только два неиндексируемых поля, но в планах было добавить в js возможность
  * развёрнутых структурированных отчётов, для возможности обзора статистики по ним.
  * Например, поля serverity (warn, error, ...) и int_code (целочисленный код проблемы по ErrorMsgs или WarnMsgs).
  */
object MDiag
  extends IEsMappingProps
  with IEmpty
{

  override type T = MDiag

  /** Контейнер имён полей модели. */
  object Fields {

    /** Имя поля с диагностическим сообщением. */
    def MESSAGE_FN = "message"

    /** Имя поля с техническим состоянием системы на момент возникновения случая, требующего диагностики. */
    def STATE_FN   = "state"

  }

  /**
    * Пустой инстанс модели.
    * val т.к. MDiag почти всегда пустая, за искл. ошибок всяких, которых должно быть как можно меньше.
    */
  override val empty = MDiag()


  import Fields._

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[MDiag] = (
    (__ \ MESSAGE_FN).formatNullable[String] and
    (__ \ STATE_FN).formatNullable[String]
  )(apply, unlift(unapply))


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.MESSAGE_FN -> FText.notIndexedJs,
      F.STATE_FN   -> FText.notIndexedJs,
    )
  }

}


/**
  * Класс модели диагностических данных [[MStat]].
  * @param message Сообщение о проблеме/ошибке и прочем в произвольной форме.
  * @param state Некая техническая строка-описание состояния системы во время возникновения проблемы.
  *              Только для чтения программистами.
  */
final case class MDiag(
  message : Option[String] = None,
  state   : Option[String] = None
)
  extends EmptyProduct
{

  def toStringSb(sb: StringBuilder = new StringBuilder()): StringBuilder = {
    sb.append('{')
    for (s <- state)
      sb.append(s).append(' ')
    for (msg <- message)
      sb.append('\n')
        .append(msg)
        .append('\n')
    sb.append('}')
  }

  override def toString = toStringSb().toString()
}