package io.suggest.model.n2.extra

import io.suggest.adn.edit.m.MAdnResView
import io.suggest.adn.{MAdnRight, MAdnRights}
import io.suggest.common.empty.EmptyUtil
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
  object Fields {

    val RES_VIEW_FN         = "r"
    val RIGHTS              = "g"
    val IS_BY_USER          = "u"
    val SHOWN_TYPE          = "s"
    val IS_TEST             = "t"

  }


  import Fields._

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[MAdnExtra] = (
    (__ \ RES_VIEW_FN).formatNullable[MAdnResView]
      .inmap[MAdnResView](
        EmptyUtil.opt2ImplMEmptyF( MAdnResView ),
        EmptyUtil.implEmpty2OptF
      ) and
    (__ \ RIGHTS).formatNullable[Set[MAdnRight]]
      .inmap [Set[MAdnRight]] (
        EmptyUtil.opt2ImplEmptyF( Set.empty ),
        { rights => if (rights.isEmpty) None else Some(rights) }
      ) and
    (__ \ IS_BY_USER).formatNullable[Boolean]
      .inmap [Boolean] (
        _.getOrElseFalse,
        someF
      ) and
    (__ \ SHOWN_TYPE).formatNullable[String] and
    (__ \ IS_TEST).formatNullable[Boolean]
      .inmap [Boolean] (
        _.getOrElseFalse,
        someF
      )
  )(apply, unlift(unapply))


  import io.suggest.es.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldObject(RES_VIEW_FN, enabled = false, properties = Nil),
      FieldKeyword(RIGHTS, index = true, include_in_all = false),
      FieldBoolean(IS_BY_USER, index = true, include_in_all = false),
      FieldKeyword(SHOWN_TYPE, index = true, include_in_all = false),
      FieldBoolean(IS_TEST, index = true, include_in_all = false),
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
  * @param resView Уровень представления ресурсов adn-узла. Логотипы, картинки приветствия и т.д. - сюда.
  */
case class MAdnExtra(
                      resView               : MAdnResView               = MAdnResView.empty,
                      rights                : Set[MAdnRight]            = Set.empty,
                      isUser                : Boolean                   = false,
                      shownTypeIdOpt        : Option[String]            = None,
                      testNode              : Boolean                   = false,
                    ) {

  def withResView(resView: MAdnResView) = copy(resView = resView)

  def isProducer: Boolean = rights.contains( MAdnRights.PRODUCER )
  def isReceiver: Boolean = rights.contains( MAdnRights.RECEIVER )

}
