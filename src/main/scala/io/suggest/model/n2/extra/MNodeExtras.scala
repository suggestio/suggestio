package io.suggest.model.n2.extra

import io.suggest.common.empty.{IEmpty, EmptyProduct}
import io.suggest.model.PrefixedFn
import io.suggest.model.es.IGenEsMappingProps
import io.suggest.model.n2.extra.tag.MTagExtra
import play.api.libs.functional.syntax._
import play.api.libs.json.{OFormat, _}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 10:25
 * Description: Модель-контейнер для различных субмоделей (ADN node, ad, tag, place, etc) одного узла N2.
 * В рамках extra-моделей узел N2 как бы "специализируется" на своей задаче.
 */
object MNodeExtras extends IGenEsMappingProps with IEmpty {

  override type T = MNodeExtras

  /** Расшаренный пустой экземпляр для дедубликации пустых инстансов контейнера в памяти. */
  override val empty: MNodeExtras = {
    new MNodeExtras() {
      override def nonEmpty = false
    }
  }

  /** Статическая модель полей модели [[MNodeExtras]]. */
  object Fields {

    object Tag extends PrefixedFn {
      val TAG_FN = "t"
      override protected def _PARENT_FN = TAG_FN
      def FACES_FN     = _fullFn( MTagExtra.Fields.Faces.FACES_FN )
      def FACE_NAME_FN = _fullFn( MTagExtra.Fields.Faces.FACE_NAME_FN )
    }

    object Adn extends PrefixedFn {
      val ADN_FN = "a"
      override protected def _PARENT_FN = ADN_FN
      def IS_TEST_FN          = _fullFn( MAdnExtra.Fields.IS_TEST.fn )
      def RIGHTS_FN           = _fullFn( MAdnExtra.Fields.RIGHTS.fn )
      def SHOWN_TYPE_FN       = _fullFn( MAdnExtra.Fields.SHOWN_TYPE.fn )
      def SHOW_IN_SC_NL_FN    = _fullFn( MAdnExtra.Fields.SHOW_IN_SC_NL.fn )
    }

  }


  import Fields.Adn.ADN_FN
  import Fields.Tag.TAG_FN

  /** Поддержка JSON для растущей модели [[MNodeExtras]]. */
  implicit val FORMAT: OFormat[MNodeExtras] = (
    (__ \ TAG_FN).formatNullable[MTagExtra] and
    (__ \ ADN_FN).formatNullable[MAdnExtra]
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._

  private def _obj(fn: String, model: IGenEsMappingProps): FieldObject = {
    FieldObject(fn, enabled = true, properties = model.generateMappingProps)
  }
  override def generateMappingProps: List[DocField] = {
    List(
      _obj(TAG_FN, MTagExtra),
      _obj(ADN_FN, MAdnExtra)
    )
  }

}


/** Класс-контейнер-реализация модели. */
case class MNodeExtras(
  tag: Option[MTagExtra] = None,
  adn: Option[MAdnExtra]  = None
  // модерация была вынесена отсюда в эджи.
)
  extends EmptyProduct
