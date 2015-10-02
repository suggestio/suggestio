package io.suggest.model.n2.extra

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.IGenEsMappingProps
import io.suggest.ym.model.AdShowLevel
import io.suggest.ym.model.common.AdnRight
import io.suggest.ym.model.common.AdnSink
import play.api.libs.json._
import play.api.libs.functional.syntax._

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
    /** Outgoing show levels. */
    val OUT_SLS             = new Val("l")
    val SINKS               = new Val("i")
    val SHOW_IN_SC_NL       = new Val("n")

  }


  /**  */
  implicit val FORMAT: OFormat[MAdnExtra] = (
    (__ \ Fields.RIGHTS.fn).formatNullable[Set[AdnRight]]
      .inmap [Set[AdnRight]] (
        _ getOrElse Set.empty,
        { rights => if (rights.isEmpty) None else Some(rights) }
      ) and
    (__ \ Fields.IS_BY_USER.fn).formatNullable[Boolean]
      .inmap [Boolean] (
        _ getOrElse false,
        Some.apply
      ) and
    (__ \ Fields.SHOWN_TYPE.fn).formatNullable[String] and
    (__ \ Fields.IS_TEST.fn).formatNullable[Boolean]
      .inmap [Boolean] (
        _ getOrElse false,
        Some.apply
      ) and
    (__ \ Fields.OUT_SLS.fn).formatNullable[Iterable[MSlInfo]]
      .inmap [Map[AdShowLevel, MSlInfo]] (
        _.iterator.flatMap(identity).map(sli => sli.sl -> sli).toMap,
        { slmap => if (slmap.isEmpty) None else Some(slmap.values) }
      ) and
    (__ \ Fields.SINKS.fn).formatNullable[Set[AdnSink]]
      .inmap [Set[AdnSink]] (
        _ getOrElse Set.empty,
        { sinks => if (sinks.isEmpty) None else Some(sinks) }
      ) and
    (__ \ Fields.SHOW_IN_SC_NL.fn).formatNullable[Boolean]
      .inmap [Boolean] (
        _ getOrElse true,
        Some.apply
      )
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    import Fields._
    import FieldIndexingVariants._
    List(
      FieldString(RIGHTS.fn, index = not_analyzed, include_in_all = false),
      FieldBoolean(IS_BY_USER.fn, index = not_analyzed, include_in_all = false),
      FieldString(SHOWN_TYPE.fn, index = not_analyzed, include_in_all = false),
      FieldBoolean(IS_TEST.fn, index = not_analyzed, include_in_all = false),
      // раньше это лежало в EMAdnMPubSettings, но потом было перемещено сюда, т.к. по сути это разделение было некорректно.
      FieldNestedObject(OUT_SLS.fn, enabled = true, properties = MSlInfo.generateMappingProps),
      FieldString(SINKS.fn, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldBoolean(SHOW_IN_SC_NL.fn, index = not_analyzed, include_in_all = false)
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
  * @param outSls Контейнер с инфой об уровнях отображения.
  * @param sinks Выходы выдачи: wifi, geoloc.
  * @param showInScNodesList Можно ли узел отображать в списке узлов выдачи?
  */
case class MAdnExtra(
  rights                : Set[AdnRight]             = Set.empty,
  isUser                : Boolean                   = false,
  shownTypeIdOpt        : Option[String]            = None,
  testNode              : Boolean                   = false,
  outSls                : Map[AdShowLevel, MSlInfo] = Map.empty,
  sinks                 : Set[AdnSink]              = Set.empty,
  showInScNodesList     : Boolean                   = true
)
