package io.suggest.model.n2.extra

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.ym.model.common.{AdnRight, AdnRights}
import io.suggest.common.empty.EmptyUtil._
import io.suggest.es.model.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.common.empty.OptionUtil.BoolOptOps

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 18:08
 * Description: Данные сугубо узла ADN теперь живут здесь.
 * Сюда попали поля из полей-моделей AdNetMember и NodeConf.
 */
object MAdnExtra extends IGenEsMappingProps {

  /** В качестве эксперимента, имена полей этой модели являются отдельной моделью. */
  object Fields extends EnumMaybeWithName {

    protected[this] sealed class Val(val fn: String)
      extends super.Val(fn)

    override type T = Val

    val RIGHTS: T           = new Val("g")
    val IS_BY_USER          = new Val("u")
    val SHOWN_TYPE          = new Val("s")
    val IS_TEST             = new Val("t")
    val SHOW_IN_SC_NL       = new Val("n")

  }

  import Fields._

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[MAdnExtra] = (
    (__ \ RIGHTS.fn).formatNullable[Set[AdnRight]]
      .inmap [Set[AdnRight]] (
        _.getOrElse( Set.empty ),
        { rights => if (rights.isEmpty) None else Some(rights) }
      ) and
    (__ \ IS_BY_USER.fn).formatNullable[Boolean]
      .inmap [Boolean] (
        _.getOrElseFalse,
        someF
      ) and
    (__ \ SHOWN_TYPE.fn).formatNullable[String] and
    (__ \ IS_TEST.fn).formatNullable[Boolean]
      .inmap [Boolean] (
        _.getOrElseFalse,
        someF
      ) and
    (__ \ SHOW_IN_SC_NL.fn).formatNullable[Boolean]
      .inmap [Boolean] (
        _.getOrElseTrue,
        someF
      )
  )(apply, unlift(unapply))


  import io.suggest.es.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldKeyword(RIGHTS.fn, index = true, include_in_all = false),
      FieldBoolean(IS_BY_USER.fn, index = true, include_in_all = false),
      FieldKeyword(SHOWN_TYPE.fn, index = true, include_in_all = false),
      FieldBoolean(IS_TEST.fn, index = true, include_in_all = false),
      FieldBoolean(SHOW_IN_SC_NL.fn, index = true, include_in_all = false)
    )
  }

}


/** Экземпляр модели данных об участнике рекламной сети.
  * @param rights Права участника сети.
  * @param isUser Узел созданный обычным юзером.
  * @param shownTypeIdOpt ID отображаемого типа участника сети. Нужно для задания кастомных типов на стороне web21.
  *                       Появилось, когда понадобилось обозначить торговый центр вокзалом/портом, не меняя его свойств.
  * @param testNode Отметка о тестовом характере существования этого узла.
  *                 Он не должен отображаться для обычных участников сети, а только для других тестовых узлов.
  * @param showInScNl Можно ли узел отображать в списке узлов выдачи?
  */
case class MAdnExtra(
  rights                : Set[AdnRight]             = Set.empty,
  isUser                : Boolean                   = false,
  shownTypeIdOpt        : Option[String]            = None,
  testNode              : Boolean                   = false,
  showInScNl            : Boolean                   = true
) {


  def isProducer: Boolean = rights.contains( AdnRights.PRODUCER )
  def isReceiver: Boolean = rights.contains( AdnRights.RECEIVER )

}
