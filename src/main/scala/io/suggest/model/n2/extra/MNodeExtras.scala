package io.suggest.model.n2.extra

import io.suggest.model.{PrefixedFn, IGenEsMappingProps}
import io.suggest.model.n2.node.MNode
import io.suggest.model.n2.tag.vertex.{EMTagVertex, EMTagVertexStaticT, MTagVertex}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.OFormat

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 10:25
 * Description: Модель-контейнер для различных субмоделей (ADN node, ad, tag, place, etc) одного узла N2.
 * В рамках extra-моделей узел N2 как бы "специализируется" на своей задаче.
 */
object MNodeExtras extends IGenEsMappingProps {

  /** Расшаренный пустой экземпляр для дедубликации пустых инстансов контейнера в памяти. */
  val empty = MNodeExtras()

  object Fields {

    val TAG_FN = "t"
    val ADN_FN = "a"

    object Adn extends PrefixedFn {

      override protected def _PARENT_FN = ADN_FN
      def ADN_IS_TEST_FN = _fullFn( MAdnExtra.Fields.IS_TEST.fn )
      def ADN_SINKS_FN   = _fullFn( MAdnExtra.Fields.SINKS.fn )
    }
  }

  // Изначально tag vertex жил прямо на верхнем уровне. TODO спилить это, когда портирование тегов будет завершено.
  val OLD_FORMAT: OFormat[MNodeExtras] = {
    EMTagVertex.FORMAT.inmap[MNodeExtras](
      {tag =>
        // Если все аргументы пустые, то вернуть empty вместо создания нового, неизменяемо пустого инстанса.
        if (tag.isEmpty)
          empty
        else
          MNodeExtras(tag = tag)
      },
      { _.tag }
    )
  }


  import Fields._

  /** Поддержка JSON для растущей модели [[MNodeExtras]]. */
  implicit val FORMAT: OFormat[MNodeExtras] = (
    (__ \ TAG_FN).formatNullable[MTagVertex] and
    (__ \ ADN_FN).formatNullable[MAdnExtra]
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._

  private def _obj(fn: String, model: IGenEsMappingProps): FieldObject = {
    FieldObject(fn, enabled = true, properties = model.generateMappingProps)
  }
  override def generateMappingProps: List[DocField] = {
    List(
      _obj(TAG_FN, MTagVertex),
      _obj(ADN_FN, MAdnExtra)
    )
  }

}


/** Аддон для модели [[MNode]] для поддержки маппинга полей. */
trait EMNodeExtrasStatic
  extends EMTagVertexStaticT


/** Класс-контейнер-реализация модели. */
case class MNodeExtras(
  tag: Option[MTagVertex] = None,
  adn: Option[MAdnExtra]  = None
)
