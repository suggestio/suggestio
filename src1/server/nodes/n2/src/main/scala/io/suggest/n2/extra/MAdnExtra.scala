package io.suggest.n2.extra

import io.suggest.adn.edit.m.MAdnResView
import io.suggest.adn.{MAdnRight, MAdnRights}
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.empty.EmptyUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.es.{IEsMappingProps, MappingDsl}
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.xplay.json.PlayJsonUtil
import monocle.macros.GenLens

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 18:08
 * Description: Данные сугубо узла ADN теперь живут здесь.
 * Сюда попали поля из полей-моделей AdNetMember и NodeConf.
 */
object MAdnExtra
  extends IEsMappingProps
{

  /** В качестве эксперимента, имена полей этой модели являются отдельной моделью. */
  object Fields {

    val RES_VIEW_FN         = "resView"
    val RIGHTS              = "rights"
    val IS_BY_USER          = "isUser"
    val SHOWN_TYPE          = "shownType"
    val IS_TEST             = "isTestNode"

  }


  import Fields._

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[MAdnExtra] = (
    PlayJsonUtil.fallbackPathFormatNullable[MAdnResView]( RES_VIEW_FN, "r" )
      .inmap[MAdnResView](
        EmptyUtil.opt2ImplMEmptyF( MAdnResView ),
        EmptyUtil.implEmpty2OptF
      ) and
    PlayJsonUtil.fallbackPathFormatNullable[Set[MAdnRight]]( RIGHTS, "g" )
      .inmap [Set[MAdnRight]] (
        EmptyUtil.opt2ImplEmptyF( Set.empty ),
        { rights => if (rights.isEmpty) None else Some(rights) }
      ) and
    PlayJsonUtil.fallbackPathFormatNullable[Boolean]( IS_BY_USER, "u" )
      .inmap [Boolean] (
        _.getOrElseFalse,
        someF
      ) and
    PlayJsonUtil.fallbackPathFormatNullable[String]( SHOWN_TYPE, "s" ) and
    PlayJsonUtil.fallbackPathFormatNullable[Boolean]( IS_TEST, "t" )
      .inmap [Boolean] (
        _.getOrElseFalse,
        someF
      )
  )(apply, unlift(unapply))


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.RES_VIEW_FN -> FObject.disabled,
      F.RIGHTS      -> FKeyWord.indexedJs,
      F.IS_BY_USER  -> FBoolean.indexedJs,
      F.SHOWN_TYPE  -> FKeyWord.indexedJs,
      F.IS_TEST     -> FBoolean.indexedJs,
    )
  }

  @inline implicit def univEq: UnivEq[MAdnExtra] = UnivEq.derive

  def resView = GenLens[MAdnExtra](_.resView)

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

  def isProducer: Boolean = rights.contains( MAdnRights.PRODUCER )
  def isReceiver: Boolean = rights.contains( MAdnRights.RECEIVER )

}
