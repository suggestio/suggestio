package io.suggest.model.n2.extra

import io.suggest.common.empty.{IEmpty, EmptyProduct}
import io.suggest.model.PrefixedFn
import io.suggest.model.es.IGenEsMappingProps
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

  /** Поддержка JSON для растущей модели [[MNodeExtras]]. */
  implicit val FORMAT: OFormat[MNodeExtras] = {
    (__ \ ADN_FN).formatNullable[MAdnExtra]
      .inmap [MNodeExtras] (
        { adnExtraOpt => MNodeExtras(adn = adnExtraOpt) },
        { mne => mne.adn }
      )
  }


  import io.suggest.util.SioEsUtil._

  private def _obj(fn: String, model: IGenEsMappingProps): FieldObject = {
    FieldObject(fn, enabled = true, properties = model.generateMappingProps)
  }
  override def generateMappingProps: List[DocField] = {
    List(
      _obj(ADN_FN, MAdnExtra)
    )
  }

}


/** Класс-контейнер-реализация модели. */
case class MNodeExtras(
  adn: Option[MAdnExtra]  = None
  // модерация и теги были вынесены в эджи.
)
  extends EmptyProduct
